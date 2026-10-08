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

import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/v1/assignments") @RequiredArgsConstructor @Transactional
public class AssignmentController {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AssignmentLifecycleService lifecycle;
    private final SubmissionRevisionRepository revisions;

    @PatchMapping("/{id}")
    public Dtos.AssignmentView edit(@PathVariable UUID id, @Valid @RequestBody Dtos.AssignmentEditRequest req, Authentication auth) {
        return view(lifecycle.edit(id, CurrentUser.id(auth), req), CurrentUser.id(auth));
    }

    @PostMapping("/{id}/publish")
    public Dtos.AssignmentView publish(@PathVariable UUID id, Authentication auth) {
        return view(lifecycle.publish(id, CurrentUser.id(auth)), CurrentUser.id(auth));
    }

    @PostMapping("/{id}/close")
    public Dtos.AssignmentView close(@PathVariable UUID id, Authentication auth) {
        return view(lifecycle.close(id, CurrentUser.id(auth)), CurrentUser.id(auth));
    }

    @GetMapping("/submissions/{submissionId}/history")
    public Dtos.PageView<Dtos.RevisionView> history(@PathVariable UUID submissionId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, Authentication auth) {
        UUID actor = CurrentUser.id(auth);
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        access.requireViewClassSubject(actor, submission.getAssignment().getClassSubject().getId());
        if (!submission.getStudent().getId().equals(actor)) access.requireTeach(actor, submission.getAssignment().getClassSubject().getId());
        if (page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST, "Use page >= 0 and size between 1 and 100");
        return Dtos.PageView.of(revisions.findBySubmissionIdOrderBySubmittedAtDesc(submissionId,
                org.springframework.data.domain.PageRequest.of(page, size)).map(r -> new Dtos.RevisionView(
                        r.getId(), r.getAnswerText(), r.getAttachmentUrl(), r.getSubmittedAt(), r.getStatus())));
    }

    @GetMapping("/{id}")
    public Dtos.AssignmentView get(@PathVariable UUID id, Authentication auth) {
        UUID userId=CurrentUser.id(auth);
        Assignment a=find(id); access.requireViewClassSubject(userId,a.getClassSubject().getId());
        if (!a.isPublished()) access.requireTeach(userId, a.getClassSubject().getId());
        return view(a,userId);
    }

    @PostMapping("/{id}/submit")
    public Dtos.SubmissionView submit(@PathVariable UUID id, @Valid @RequestBody Dtos.SubmitAssignmentRequest req, Authentication auth) {
        return lifecycle.submit(id, CurrentUser.id(auth), req);
    }

    @GetMapping("/{id}/submissions")
    public List<Dtos.SubmissionView> submissions(@PathVariable UUID id, Authentication auth) {
        Assignment a=find(id); access.requireTeach(CurrentUser.id(auth),a.getClassSubject().getId());
        return submissions.findByAssignmentIdOrderBySubmittedAtAsc(id).stream().map(DtoMapper::submission).toList();
    }

    @GetMapping("/{id}/statistics")
    public Dtos.AssignmentStats statistics(@PathVariable UUID id, Authentication auth) {
        Assignment assignment = find(id);
        access.requireTeach(CurrentUser.id(auth), assignment.getClassSubject().getId());
        var stats = submissions.statistics(id);
        return new Dtos.AssignmentStats(stats.getStudents(), stats.getSubmitted(), stats.getStudents() - stats.getSubmitted(),
                stats.getGraded(), stats.getSubmitted() - stats.getGraded(), stats.getLate());
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    public Dtos.SubmissionView grade(@PathVariable UUID submissionId, @Valid @RequestBody Dtos.GradeSubmissionRequest req, Authentication auth) {
        UUID userId=CurrentUser.id(auth);
        UUID assignmentId = submissions.assignmentId(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        assignments.findLockedById(assignmentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        Submission s=submissions.findById(submissionId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Submission not found"));
        access.requireTeach(userId,s.getAssignment().getClassSubject().getId());
        if(!Double.isFinite(req.marks()) || req.marks()<0 || req.marks()>s.getAssignment().getMaxMarks()) throw new ApiException(HttpStatus.BAD_REQUEST,"Marks must be between 0 and max marks");
        s.setMarks(req.marks());s.setFeedback(req.feedback());s.setGradedBy(users.getReferenceById(userId));s.setStatus(SubmissionStatus.GRADED);s=submissions.save(s);
        notifications.notifyUser(s.getStudent(),"Assignment graded",s.getAssignment().getTitle()+" has been graded",NotificationType.GRADE,"SUBMISSION",s.getId());
        audit.record(s.getAssignment().getClassSubject().getClassroom().getProgram().getDepartment().getId(),userId,"GRADE_SUBMISSION","Submission",s.getId(),"marks="+req.marks());
        return DtoMapper.submission(s);
    }

    private Assignment find(UUID id){return assignments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Assignment not found"));}
    private Dtos.AssignmentView view(Assignment a, UUID userId){
        var mine=submissions.findByAssignmentIdAndStudentId(a.getId(),userId).map(DtoMapper::submission).orElse(null);
        return new Dtos.AssignmentView(a.getId(),a.getClassSubject().getId(),a.getClassSubject().getClassroom().getId(),a.getClassSubject().getClassroom().getName(),a.getClassSubject().getSubject().getName(),a.getTitle(),a.getDescription(),a.getDueAt(),a.getMaxMarks(),a.isPublished(),mine,a.getClosedAt());
    }
}
