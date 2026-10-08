package com.eone.api;

import com.eone.domain.*;
import com.eone.repository.*;
import com.eone.security.CurrentUser;
import com.eone.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/v1/class-subjects") @RequiredArgsConstructor @Transactional
public class SubjectController {
    private final AccessService access;
    private final ClassSubjectRepository classSubjects;
    private final ChapterRepository chapters;
    private final LearningMaterialRepository materials;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;

    @GetMapping("/{id}")
    public Dtos.SubjectView get(@PathVariable UUID id, Authentication auth) {
        access.requireViewClassSubject(CurrentUser.id(auth), id);
        return DtoMapper.subject(classSubjects.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Class subject not found")));
    }

    @GetMapping("/{id}/chapters")
    public List<Dtos.ChapterView> chapters(@PathVariable UUID id, Authentication auth) {
        access.requireViewClassSubject(CurrentUser.id(auth), id);
        return chapters.findByClassSubjectIdOrderByPositionAsc(id).stream()
                .map(c -> new Dtos.ChapterView(c.getId(), c.getTitle(), c.getPosition(), materials.findByChapterIdOrderByCreatedAtDesc(c.getId()).stream().map(DtoMapper::material).toList())).toList();
    }

    @PostMapping("/{id}/chapters") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.ChapterView createChapter(@PathVariable UUID id, @Valid @RequestBody Dtos.ChapterCreateRequest req, Authentication auth) {
        UUID userId = CurrentUser.id(auth); access.requireTeach(userId, id);
        ClassSubject cs = classSubjects.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Class subject not found"));
        Chapter c = new Chapter(); c.setClassSubject(cs); c.setTitle(req.title()); c.setPosition(req.position()); c = chapters.save(c);
        audit.record(cs.getClassroom().getProgram().getDepartment().getId(),userId,"CREATE_CHAPTER","Chapter",c.getId(),c.getTitle());
        return new Dtos.ChapterView(c.getId(), c.getTitle(), c.getPosition(), List.of());
    }

    @PostMapping("/chapters/{chapterId}/materials") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.MaterialView createMaterial(@PathVariable UUID chapterId, @Valid @RequestBody Dtos.MaterialCreateRequest req, Authentication auth) {
        Chapter chapter = chapters.findById(chapterId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Chapter not found"));
        UUID userId=CurrentUser.id(auth); access.requireTeach(userId, chapter.getClassSubject().getId());
        LearningMaterial m=new LearningMaterial(); m.setChapter(chapter);m.setTitle(req.title());m.setType(req.type());m.setUrl(req.url());m.setDescription(req.description());
        m.setCreatedBy(users.getReferenceById(userId)); m=materials.save(m);
        notifications.notifyClassroomStudents(chapter.getClassSubject().getClassroom(), "New learning material", req.title(), NotificationType.GENERAL, "MATERIAL", m.getId());
        audit.record(chapter.getClassSubject().getClassroom().getProgram().getDepartment().getId(),userId,"CREATE_MATERIAL","LearningMaterial",m.getId(),m.getTitle());
        return DtoMapper.material(m);
    }

    @GetMapping("/{id}/assignments")
    public List<Dtos.AssignmentView> assignments(@PathVariable UUID id, Authentication auth) {
        UUID userId=CurrentUser.id(auth); access.requireViewClassSubject(userId,id);
        var visible = access.canTeach(userId, id) ? assignments.findByClassSubjectIdOrderByDueAtAsc(id)
                : assignments.findByClassSubjectIdAndPublishedTrueOrderByDueAtAsc(id);
        return visible.stream().map(a -> assignmentView(a,userId)).toList();
    }

    @PostMapping("/{id}/assignments") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.AssignmentView createAssignment(@PathVariable UUID id, @Valid @RequestBody Dtos.AssignmentCreateRequest req, Authentication auth) {
        UUID userId=CurrentUser.id(auth); access.requireTeach(userId,id);
        if (!Double.isFinite(req.maxMarks())) throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum marks must be finite");
        if (!req.dueAt().isAfter(java.time.Instant.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment due date must be in the future");
        ClassSubject cs=classSubjects.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Class subject not found"));
        Assignment a=new Assignment();a.setClassSubject(cs);a.setTitle(req.title());a.setDescription(req.description());a.setDueAt(req.dueAt());a.setMaxMarks(req.maxMarks());a.setPublished(req.published());a.setCreatedBy(users.getReferenceById(userId));
        a=assignments.save(a);
        if(a.isPublished()) notifications.notifyClassroomStudents(cs.getClassroom(),"New assignment",a.getTitle(),NotificationType.ASSIGNMENT,"ASSIGNMENT",a.getId());
        audit.record(cs.getClassroom().getProgram().getDepartment().getId(),userId,"CREATE_ASSIGNMENT","Assignment",a.getId(),a.getTitle());
        return assignmentView(a,userId);
    }

    private Dtos.AssignmentView assignmentView(Assignment a, UUID userId) {
        Dtos.SubmissionView mine=submissions.findByAssignmentIdAndStudentId(a.getId(),userId).map(DtoMapper::submission).orElse(null);
        return new Dtos.AssignmentView(a.getId(),a.getClassSubject().getId(),a.getClassSubject().getClassroom().getId(),a.getClassSubject().getClassroom().getName(),a.getClassSubject().getSubject().getName(),a.getTitle(),a.getDescription(),a.getDueAt(),a.getMaxMarks(),a.isPublished(),mine,a.getClosedAt());
    }
}
