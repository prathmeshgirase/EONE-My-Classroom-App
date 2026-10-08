package com.eone.api;

import com.eone.domain.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min=8,max=72) String password,
                                  @NotBlank String firstName, @NotBlank String lastName,
                                  String phone, @NotNull AccountType accountType, @NotBlank String departmentCode) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record AuthResponse(String accessToken, String refreshToken, UserView user) {}

    public record UserView(UUID id, String email, String firstName, String lastName, String phone,
                           AccountType accountType, UserStatus status, UUID departmentId, String departmentName) {}

    public record ContextView(String kind, UUID departmentId, String departmentName,
                              UUID classroomId, String classroomName,
                              UUID classSubjectId, String subjectName, String role) {}

    public record DashboardView(UserView user, List<ContextView> contexts, List<ClassroomView> classrooms,
                                long unreadNotifications, List<AssignmentView> upcomingAssignments) {}

    public record ClassroomView(UUID id, String name, String programCode, String programName,
                                String yearLevel, String division, String termName, UUID departmentId) {}
    public record SubjectView(UUID classSubjectId, UUID subjectId, String code, String name, UUID classroomId, String classroomName) {}
    public record ChapterView(UUID id, String title, int position, List<MaterialView> materials) {}
    public record MaterialView(UUID id, String title, MaterialType type, String url, String description, Instant createdAt) {}
    public record AssignmentView(UUID id, UUID classSubjectId, UUID classroomId, String classroomName, String subjectName, String title, String description,
                                 Instant dueAt, double maxMarks, boolean published, SubmissionView mySubmission, Instant closedAt) {
        public String getState() { return !published ? "DRAFT" : closedAt != null ? "CLOSED" : Instant.now().isAfter(dueAt) ? "OVERDUE" : "OPEN"; }
    }
    public record AssignmentEditRequest(@NotBlank @Size(max=180) String title, @NotBlank String description,
                                        @NotNull Instant dueAt, @Positive double maxMarks) {}
    public record AssignmentStats(long students, long submitted, long notSubmitted, long graded, long pendingGrading, long late) {}
    public record RevisionView(UUID id, String answerText, String attachmentUrl, Instant submittedAt, SubmissionStatus status) {}
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
    public record SubmissionView(UUID id, UUID studentId, String studentName, String answerText, String attachmentUrl,
                                 SubmissionStatus status, Instant submittedAt, Double marks, String feedback) {}
    public record AnnouncementView(UUID id, String title, String body, TargetType targetType, Instant createdAt, String authorName) {}
    public record NotificationView(UUID id, String title, String body, NotificationType type,
                                   String relatedType, UUID relatedId, Instant createdAt, Instant readAt) {}
    public record AttendanceRecordView(UUID studentId, String studentName, AttendanceStatus status) {}
    public record AttendanceSessionView(UUID id, UUID classSubjectId, LocalDate sessionDate, String topic,
                                        List<AttendanceRecordView> records) {}

    public record ProgramCreateRequest(@NotBlank String code, @NotBlank String name) {}
    public record TermCreateRequest(@NotBlank String name, @NotNull LocalDate startDate, @NotNull LocalDate endDate, boolean active) {}
    public record ClassroomCreateRequest(@NotNull UUID programId, @NotNull UUID termId, @NotNull YearLevel yearLevel,
                                         @NotBlank String division, @NotBlank String name) {}
    public record SubjectCreateRequest(@NotBlank String code, @NotBlank String name) {}
    public record ClassSubjectCreateRequest(@NotNull UUID classroomId, @NotNull UUID subjectId) {}
    public record RoleCreateRequest(@NotNull UUID userId, @NotNull ScopedRole role,
                                    UUID departmentId, UUID classroomId, UUID classSubjectId) {}
    public record EnrollmentCreateRequest(@NotNull UUID studentId, @NotNull UUID classroomId) {}

    public record ChapterCreateRequest(@NotBlank String title, @Min(1) int position) {}
    public record MaterialCreateRequest(@NotBlank String title, @NotNull MaterialType type,
                                        @NotBlank String url, String description) {}
    public record AssignmentCreateRequest(@NotBlank String title, @NotBlank String description,
                                          @NotNull Instant dueAt, @Positive double maxMarks, boolean published) {}
    public record SubmitAssignmentRequest(String answerText, String attachmentUrl) {}
    public record GradeSubmissionRequest(@NotNull Double marks, String feedback) {}
    public record AttendanceCreateRequest(@NotNull @PastOrPresent LocalDate sessionDate, @Size(max=180) String topic,
                                          @NotEmpty List<@NotNull @jakarta.validation.Valid AttendanceMark> records) {}
    public record AttendanceMark(@NotNull UUID studentId, @NotNull AttendanceStatus status) {}
    public record AnnouncementCreateRequest(@NotBlank String title, @NotBlank String body, @NotNull TargetType targetType,
                                            UUID departmentId, UUID classroomId, UUID classSubjectId) {}
}
