package com.eone;

import com.eone.domain.*;
import com.eone.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired DepartmentRepository departments;
    @Autowired ProgramRepository programs;
    @Autowired AcademicTermRepository terms;
    @Autowired ClassroomRepository classrooms;
    @Autowired SubjectRepository subjects;
    @Autowired ClassSubjectRepository classSubjects;
    @Autowired UserRepository users;
    @Autowired RoleAssignmentRepository roles;
    @Autowired EnrollmentRepository enrollments;
    @Autowired AssignmentRepository assignments;
    @Autowired SubmissionRepository submissions;
    @Autowired NotificationRepository notifications;
    @Autowired ChapterRepository chapters;
    @Autowired StoredFileRepository files;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired AuditLogRepository auditLogs;
    @Autowired PasswordResetRepository passwordResets;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired com.eone.security.JwtService jwt;
    @Autowired jakarta.persistence.EntityManager entityManager;

    User teacher, student, hod, outsider;
    Classroom classroom;
    ClassSubject taught, other;
    Assignment draft, published, unrelated;

    @BeforeEach void setup() {
        var department = departments.save(new Department("CS", "Computer Science"));
        var program = programs.save(new Program(department, "BSC", "Computer Science"));
        var term = terms.save(new AcademicTerm("2026", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), true));
        classroom = new Classroom();
        classroom.setProgram(program); classroom.setAcademicTerm(term); classroom.setYearLevel(YearLevel.SY);
        classroom.setDivision("A"); classroom.setName("SY A"); classroom = classrooms.save(classroom);
        teacher = person(department, AccountType.STAFF);
        hod = person(department, AccountType.STAFF);
        student = person(department, AccountType.STUDENT);
        outsider = person(department, AccountType.STUDENT);
        taught = subject(department, "DBMS"); other = subject(department, "OS");
        var role = new RoleAssignment(); role.setUser(teacher); role.setRole(ScopedRole.SUBJECT_TEACHER); role.setClassSubject(taught); roles.save(role);
        var admin = new RoleAssignment(); admin.setUser(hod); admin.setRole(ScopedRole.HOD); admin.setDepartment(department); roles.save(admin);
        var enrollment = new Enrollment(); enrollment.setStudent(student); enrollment.setClassroom(classroom); enrollments.save(enrollment);
        draft = assignment(taught, "Draft", false);
        published = assignment(taught, "Published", true);
        unrelated = assignment(other, "Unrelated", true);
    }

    @Test void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test void announcementPagesRespectScopeSearchAndAuditDepartment() throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("title", "Exam schedule", "body", "Friday",
            "targetType", "SUBJECT", "classSubjectId", taught.getId()));
        mvc.perform(post("/api/v1/announcements").with(user(teacher.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/announcements/page").param("target","SUBJECT").param("scope",taught.getId().toString())
            .param("search","EXAM").param("size","1").with(user(student.getId().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Exam schedule"));
        mvc.perform(get("/api/v1/announcements/page").param("target","SUBJECT").param("scope",taught.getId().toString())
            .with(user(outsider.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/announcements/page").param("target","SUBJECT").param("scope",other.getId().toString())
            .with(user(teacher.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/announcements/page").param("target","SUBJECT").param("scope",taught.getId().toString())
            .param("size","101").with(user(student.getId().toString()))).andExpect(status().isBadRequest());
        assertThat(auditLogs.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo("CREATE_ANNOUNCEMENT");
            assertThat(log.getDepartmentId()).isEqualTo(classroom.getProgram().getDepartment().getId());
        });
    }

    @Test void duplicateAttendanceDateAndTopicAreRejectedWithoutDeletingHistory() throws Exception {
        String body="{\"sessionDate\":\""+LocalDate.now()+"\",\"topic\":\"Revision\",\"records\":[{\"studentId\":\""+student.getId()+"\",\"status\":\"PRESENT\"}]}";
        String endpoint="/api/v1/attendance/class-subjects/"+taught.getId()+"/sessions";
        mvc.perform(post(endpoint).with(user(teacher.getId().toString())).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mvc.perform(post(endpoint).with(user(teacher.getId().toString())).contentType(MediaType.APPLICATION_JSON).content(body.replace("Revision"," revision ")))
            .andExpect(status().isConflict());
    }

    @Test void recoveryCodeIsSingleUseAndInvalidatesOldAccessTokens() throws Exception {
        String oldToken=jwt.createAccessToken(student.getId());
        var reset=new PasswordReset();reset.setUserId(student.getId());reset.setTokenHash(com.eone.service.RecoveryService.hash("test-code"));
        reset.setExpiresAt(Instant.now().plusSeconds(300));passwordResets.saveAndFlush(reset);
        String body="{\"token\":\"test-code\",\"password\":\"NewStrongPassword123!\"}";
        mvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        entityManager.flush();
        assertThat(passwordEncoder.matches("NewStrongPassword123!",users.findById(student.getId()).orElseThrow().getPasswordHash())).isTrue();
        mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+jwt.createAccessToken(student.getId()))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test void recoveryRequestDoesNotDiscloseAccountExistenceAndExpiredCodesFail() throws Exception {
        var known=mvc.perform(post("/api/v1/auth/password/request").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("email",student.getEmail())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var unknown=mvc.perform(post("/api/v1/auth/password/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"unknown@example.invalid\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown);
        var reset=new PasswordReset();reset.setUserId(student.getId());reset.setTokenHash(com.eone.service.RecoveryService.hash("expired"));
        reset.setExpiresAt(Instant.now().minusSeconds(1));passwordResets.saveAndFlush(reset);
        mvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"expired\",\"password\":\"NewStrongPassword123!\"}")).andExpect(status().isBadRequest());
    }

    @Test void inboxPagesStayOwnedAndValidateBounds() throws Exception {
        for (int i=0;i<3;i++) {
            var n=new Notification();n.setUser(student);n.setTitle("Update");n.setBody("Body");n.setType(NotificationType.GENERAL);notifications.save(n);
        }
        mvc.perform(get("/api/v1/notifications/page").param("size","2").with(user(student.getId().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/notifications/page").with(user(outsider.getId().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/notifications/page").param("size","101").with(user(student.getId().toString())))
            .andExpect(status().isBadRequest());
    }

    @Test void auditPageRequiresHodAndExcludesUnscopedHistory() throws Exception {
        UUID department=hod.getDepartment().getId();
        var scoped=new AuditLog();scoped.setDepartmentId(department);scoped.setAction("APPROVE_USER");scoped.setEntityType("User");auditLogs.save(scoped);
        var unscoped=new AuditLog();unscoped.setAction("LEGACY");unscoped.setEntityType("User");auditLogs.save(unscoped);
        String endpoint="/api/v1/hod/departments/"+department+"/audit";
        mvc.perform(get(endpoint).with(user(teacher.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get(endpoint).with(user(hod.getId().toString()))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].action").value("APPROVE_USER"));
    }

    @Test void myAttendanceIncludesOnlyOwnActiveSubjects() throws Exception {
        entityManager.flush();
        mvc.perform(get("/api/v1/attendance/me/summary").with(user(student.getId().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/attendance/me/summary").with(user(outsider.getId().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private org.springframework.mock.web.MockMultipartFile pdf(String name) {
        return new org.springframework.mock.web.MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.7\nTest fixture\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private String uploadSubmission(User actor, Assignment assignment) throws Exception {
        var response = mvc.perform(multipart("/api/v1/assignments/{id}/submit-file", assignment.getId())
                .file(pdf("answer.pdf")).param("answerText", "My answer").with(user(actor.getId().toString())))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("attachmentUrl").asText();
    }

    @Test void submissionFilesArePrivateAndOldVersionsRemainDownloadable() throws Exception {
        String url = uploadSubmission(student, published);
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).with(user(student.getId().toString())))
                .andExpect(status().isOk()).andExpect(content().bytes(pdf("answer.pdf").getBytes()))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(url).with(user(teacher.getId().toString()))).andExpect(status().isOk());
        mvc.perform(get(url).with(user(hod.getId().toString()))).andExpect(status().isForbidden());
        var enrollment = new Enrollment(); enrollment.setStudent(outsider); enrollment.setClassroom(classroom); enrollments.save(enrollment);
        mvc.perform(get(url).with(user(outsider.getId().toString()))).andExpect(status().isForbidden());
        String replacement = uploadSubmission(student, published);
        assertThat(replacement).isNotEqualTo(url);
        var submission = submissions.findByAssignmentIdAndStudentId(published.getId(), student.getId()).orElseThrow();
        mvc.perform(get("/api/v1/assignments/submissions/{id}/history", submission.getId()).with(user(student.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].attachmentUrl").value(url));
        mvc.perform(get(url).with(user(student.getId().toString()))).andExpect(status().isOk());
    }

    @Test void materialUploadIsTeacherScopedAndDownloadsFollowEnrollment() throws Exception {
        var chapter = new Chapter(); chapter.setClassSubject(taught); chapter.setTitle("Files"); chapter.setPosition(1); chapter = chapters.save(chapter);
        String endpoint = "/api/v1/class-subjects/chapters/" + chapter.getId() + "/materials/upload";
        mvc.perform(multipart(endpoint).file(pdf("a.pdf")).param("title", "Notes").with(user(hod.getId().toString())))
                .andExpect(status().isForbidden());
        var result = mvc.perform(multipart(endpoint).file(pdf("../../notes.pdf")).param("title", "Notes").with(user(teacher.getId().toString())))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String url = json.readTree(result.getContentAsString()).get("url").asText();
        mvc.perform(get(url).with(user(student.getId().toString()))).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"notes.pdf\""));
        mvc.perform(get(url).with(user(outsider.getId().toString()))).andExpect(status().isForbidden());
        var enrollment = enrollments.findAll().stream().filter(e -> e.getStudent().getId().equals(student.getId())).findFirst().orElseThrow();
        enrollment.setStatus(EnrollmentStatus.DROPPED); enrollments.saveAndFlush(enrollment);
        mvc.perform(get(url).with(user(student.getId().toString()))).andExpect(status().isForbidden());
    }

    @Test void uploadsRejectEmptyOversizedAndDisguisedFiles() throws Exception {
        String endpoint = "/api/v1/assignments/" + published.getId() + "/submit-file";
        mvc.perform(multipart(endpoint).file(new org.springframework.mock.web.MockMultipartFile("file", "fake.pdf", "application/pdf", "<html>bad</html>".getBytes()))
                .with(user(student.getId().toString()))).andExpect(status().isBadRequest());
        mvc.perform(multipart(endpoint).file(new org.springframework.mock.web.MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                .with(user(student.getId().toString()))).andExpect(status().isBadRequest());
        mvc.perform(multipart(endpoint).file(new org.springframework.mock.web.MockMultipartFile("file", "big.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]))
                .with(user(student.getId().toString()))).andExpect(status().isPayloadTooLarge());
        assertThat(files.count()).isZero();
    }

    @Test void attachmentsCannotBeReusedByAnotherStudentOrAssignment() throws Exception {
        String url = uploadSubmission(student, published);
        var enrollment = new Enrollment(); enrollment.setStudent(outsider); enrollment.setClassroom(classroom); enrollments.save(enrollment);
        String body = json.writeValueAsString(java.util.Map.of("attachmentUrl", url));
        mvc.perform(post("/api/v1/assignments/{id}/submit", published.getId()).with(user(outsider.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/assignments/{id}/submit", unrelated.getId()).with(user(student.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    @Test void rejectedClosedSubmissionCleansUpNewBlobOnRollback() throws Exception {
        published.setClosedAt(Instant.now()); assignments.saveAndFlush(published);
        mvc.perform(multipart("/api/v1/assignments/{id}/submit-file", published.getId())
                .file(pdf("rejected.pdf")).with(user(student.getId().toString()))).andExpect(status().isConflict());
        var paths = files.findAll().stream().map(f -> java.nio.file.Path.of("target/upload-tests", f.getStorageKey())).toList();
        assertThat(paths).hasSize(1);
        org.springframework.test.context.transaction.TestTransaction.flagForRollback();
        org.springframework.test.context.transaction.TestTransaction.end();
        for (var path : paths) assertThat(java.nio.file.Files.exists(path)).isFalse();
        assertThat(files.count()).isZero();
    }

    @Test void attendanceAnalyticsAreScopedAndFilterInDatabase() throws Exception {
        String summary = "/api/v1/attendance/classrooms/" + classroom.getId() + "/summary";
        mvc.perform(get(summary).with(user(student.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get(summary).with(user(teacher.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get(summary).with(user(hod.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(summary).param("below", "75").with(user(hod.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/v1/attendance/class-subjects/{id}/sessions", taught.getId())
                .with(user(teacher.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionDate\":\"" + LocalDate.now() + "\",\"records\":[{\"studentId\":\"" + student.getId() + "\",\"status\":\"ABSENT\"}]}"))
                .andExpect(status().isCreated());
        // MockMvc shares this test transaction; a real HTTP write commits before the next request.
        entityManager.flush();
        mvc.perform(get(summary).param("below", "75").with(user(hod.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].percentage").value(0.0));
    }

    @Test void onlyAssignedTeacherCanPublishAndCloseDraft() throws Exception {
        mvc.perform(post("/api/v1/assignments/{id}/publish", draft.getId()).with(user(hod.getId().toString())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/assignments/{id}/publish", draft.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("OPEN"));
        mvc.perform(post("/api/v1/assignments/{id}/close", draft.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CLOSED"));
        mvc.perform(post("/api/v1/assignments/{id}/submit", draft.getId()).with(user(student.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answerText\":\"answer\"}"))
                .andExpect(status().isConflict());
    }

    @Test void draftEditIsScopedAndPublishedContentCannotBeChanged() throws Exception {
        String body = "{\"title\":\"Revised draft\",\"description\":\"New instructions\",\"dueAt\":\""
                + Instant.now().plusSeconds(7200) + "\",\"maxMarks\":30}";
        mvc.perform(patch("/api/v1/assignments/{id}", draft.getId()).with(user(student.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/assignments/{id}", draft.getId()).with(user(teacher.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Revised draft"));
        mvc.perform(patch("/api/v1/assignments/{id}", published.getId()).with(user(teacher.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/class-subjects/{id}/assignments", taught.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/class-subjects/{id}/assignments", taught.getId()).with(user(student.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void resubmissionPreservesPreviousAnswerAndHistoryIsPrivate() throws Exception {
        var endpoint = "/api/v1/assignments/" + published.getId() + "/submit";
        mvc.perform(post(endpoint).with(user(student.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerText\":\"first version\"}")).andExpect(status().isOk());
        mvc.perform(post(endpoint).with(user(student.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerText\":\"second version\"}")).andExpect(status().isOk());
        var submission = submissions.findByAssignmentIdAndStudentId(published.getId(), student.getId()).orElseThrow();
        var history = "/api/v1/assignments/submissions/" + submission.getId() + "/history";
        mvc.perform(get(history).with(user(student.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].answerText").value("first version"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(history).with(user(hod.getId().toString()))).andExpect(status().isForbidden());
        mvc.perform(get(history).with(user(outsider.getId().toString()))).andExpect(status().isForbidden());
    }

    @Test void firstLateSubmissionAllowedButResubmissionAfterDeadlineRejected() throws Exception {
        published.setDueAt(Instant.now().minusSeconds(10)); assignments.saveAndFlush(published);
        var endpoint = "/api/v1/assignments/" + published.getId() + "/submit";
        mvc.perform(post(endpoint).with(user(student.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerText\":\"late answer\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LATE"));
        mvc.perform(post(endpoint).with(user(student.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerText\":\"replacement\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/assignments/{id}/statistics", published.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.students").value(1))
                .andExpect(jsonPath("$.submitted").value(1)).andExpect(jsonPath("$.late").value(1))
                .andExpect(jsonPath("$.pendingGrading").value(1));
    }

    @Test void teacherDashboardAndSubjectListExcludeUnassignedSubjects() throws Exception {
        mvc.perform(get("/api/v1/me/dashboard").with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upcomingAssignments.length()").value(1))
                .andExpect(jsonPath("$.upcomingAssignments[0].id").value(published.getId().toString()));
        mvc.perform(get("/api/v1/classrooms/{id}/subjects", classroom.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classSubjectId").value(taught.getId().toString()));
    }

    @Test void studentsCannotReadOrSubmitDrafts() throws Exception {
        mvc.perform(get("/api/v1/assignments/{id}", draft.getId()).with(user(student.getId().toString())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/assignments/{id}/submit", draft.getId()).with(user(student.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answerText\":\"answer\"}"))
                .andExpect(status().isConflict());
        assertThat(submissions.findByAssignmentIdAndStudentId(draft.getId(), student.getId())).isEmpty();
    }

    @Test void teacherCanReadOwnDraftButCannotReadAnotherSubject() throws Exception {
        mvc.perform(get("/api/v1/assignments/{id}", draft.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments/{id}", unrelated.getId()).with(user(teacher.getId().toString())))
                .andExpect(status().isForbidden());
    }

    @Test void unenrolledStudentCannotAccessClassroomOrSubmit() throws Exception {
        mvc.perform(get("/api/v1/classrooms/{id}", classroom.getId()).with(user(outsider.getId().toString())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/assignments/{id}/submit", published.getId()).with(user(outsider.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answerText\":\"answer\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void gradedWorkIsImmutableToStudentAndHodCannotGrade() throws Exception {
        var submission = new Submission(); submission.setAssignment(published); submission.setStudent(student);
        submission.setStatus(SubmissionStatus.GRADED); submission.setAnswerText("Original");
        submission.setSubmittedAt(Instant.now()); submission.setMarks(15.0); submission.setGradedBy(teacher);
        submission = submissions.save(submission);
        mvc.perform(post("/api/v1/assignments/{id}/submit", published.getId()).with(user(student.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"answerText\":\"Replacement\"}"))
                .andExpect(status().isConflict());
        assertThat(submission.getAnswerText()).isEqualTo("Original");
        assertThat(submission.getMarks()).isEqualTo(15.0);
        mvc.perform(patch("/api/v1/assignments/submissions/{id}/grade", submission.getId()).with(user(hod.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"marks\":20}"))
                .andExpect(status().isForbidden());
    }

    @Test void attendanceRejectsInvalidNestedMarksAndDuplicates() throws Exception {
        var path = "/api/v1/attendance/class-subjects/" + taught.getId() + "/sessions";
        mvc.perform(post(path).with(user(teacher.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionDate\":\"" + LocalDate.now() + "\",\"records\":[{}]}"))
                .andExpect(status().isBadRequest());
        var mark = "{\"studentId\":\"" + student.getId() + "\",\"status\":\"PRESENT\"}";
        mvc.perform(post(path).with(user(teacher.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionDate\":\"" + LocalDate.now() + "\",\"records\":[" + mark + "," + mark + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test void markAllReadIncludesOlderNotificationsAndPreservesOtherUsers() throws Exception {
        for (int i = 0; i < 105; i++) notification(student);
        notification(outsider);
        mvc.perform(post("/api/v1/notifications/read-all").with(user(student.getId().toString())))
                .andExpect(status().isOk());
        assertThat(notifications.countByUserIdAndReadAtIsNull(student.getId())).isZero();
        assertThat(notifications.countByUserIdAndReadAtIsNull(outsider.getId())).isEqualTo(1);
    }

    @Test void malformedIdentifiersReturn400() throws Exception {
        mvc.perform(get("/api/v1/assignments/not-a-uuid").with(user(student.getId().toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test void hodCannotManageAnotherDepartment() throws Exception {
        var another = departments.save(new Department("IT", "Information Technology"));
        mvc.perform(get("/api/v1/hod/departments/{id}/users", another.getId()).with(user(hod.getId().toString())))
                .andExpect(status().isForbidden());
    }

    @Test void classTeacherCanSeeRosterButCannotListSubmissionsWithoutTeachingRole() throws Exception {
        var role = new RoleAssignment(); role.setUser(outsider); role.setRole(ScopedRole.CLASS_TEACHER);
        role.setClassroom(classroom); roles.save(role);
        mvc.perform(get("/api/v1/classrooms/{id}/students", classroom.getId()).with(user(outsider.getId().toString())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments/{id}/submissions", published.getId()).with(user(outsider.getId().toString())))
                .andExpect(status().isForbidden());
    }

    private void notification(User recipient) {
        var notification = new Notification(); notification.setUser(recipient); notification.setTitle("Test");
        notification.setBody("Test"); notification.setType(NotificationType.GENERAL); notifications.save(notification);
    }

    private User person(Department department, AccountType type) {
        var person = new User(); person.setEmail(UUID.randomUUID() + "@test.invalid"); person.setPasswordHash("test-only");
        person.setFirstName("Test"); person.setLastName("User"); person.setDepartment(department);
        person.setAccountType(type); person.setStatus(UserStatus.ACTIVE); return users.save(person);
    }
    private ClassSubject subject(Department department, String code) {
        var subject = new Subject(); subject.setDepartment(department); subject.setCode(code); subject.setName(code);
        var result = new ClassSubject(); result.setSubject(subjects.save(subject)); result.setClassroom(classroom);
        return classSubjects.save(result);
    }
    private Assignment assignment(ClassSubject subject, String title, boolean visible) {
        var assignment = new Assignment(); assignment.setClassSubject(subject); assignment.setTitle(title);
        assignment.setDescription("Test assignment"); assignment.setDueAt(Instant.now().plusSeconds(3600));
        assignment.setMaxMarks(20); assignment.setPublished(visible); assignment.setCreatedBy(teacher);
        return assignments.save(assignment);
    }
}
