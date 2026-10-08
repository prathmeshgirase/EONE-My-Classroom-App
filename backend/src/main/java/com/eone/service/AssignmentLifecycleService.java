package com.eone.service;

import com.eone.api.*;
import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentLifecycleService {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final SubmissionRevisionRepository revisions;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;
    private final AuditService audit;
    private final StoredFileRepository files;

    public Assignment edit(UUID id, UUID actor, Dtos.AssignmentEditRequest request) {
        Assignment assignment = teachingAssignment(id, actor);
        if (assignment.isPublished()) conflict("Only drafts can be edited");
        if (!request.dueAt().isAfter(Instant.now()) || !Double.isFinite(request.maxMarks()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a future deadline and finite positive marks");
        assignment.setTitle(request.title().trim()); assignment.setDescription(request.description());
        assignment.setDueAt(request.dueAt()); assignment.setMaxMarks(request.maxMarks());
        audit.record(assignment.getClassSubject().getClassroom().getProgram().getDepartment().getId(),actor, "EDIT_ASSIGNMENT", "Assignment", id, "Draft edited");
        return assignment;
    }

    public Assignment publish(UUID id, UUID actor) {
        Assignment assignment = teachingAssignment(id, actor);
        if (assignment.isPublished()) return assignment;
        if (!assignment.getDueAt().isAfter(Instant.now())) conflict("Set a future deadline before publishing");
        assignment.setPublished(true);
        notifications.notifyClassroomStudents(assignment.getClassSubject().getClassroom(), "New assignment",
                assignment.getTitle(), NotificationType.ASSIGNMENT, "ASSIGNMENT", id);
        audit.record(assignment.getClassSubject().getClassroom().getProgram().getDepartment().getId(),actor, "PUBLISH_ASSIGNMENT", "Assignment", id, "Published");
        return assignment;
    }

    public Assignment close(UUID id, UUID actor) {
        Assignment assignment = teachingAssignment(id, actor);
        if (!assignment.isPublished()) conflict("Publish the draft before closing submissions");
        if (assignment.getClosedAt() == null) {
            assignment.setClosedAt(Instant.now());
            audit.record(assignment.getClassSubject().getClassroom().getProgram().getDepartment().getId(),actor, "CLOSE_ASSIGNMENT", "Assignment", id, "Submissions closed");
        }
        return assignment;
    }

    public Dtos.SubmissionView submit(UUID id, UUID actor, Dtos.SubmitAssignmentRequest request) {
        Assignment assignment = locked(id);
        if (!enrollments.existsByStudentIdAndClassroomIdAndStatus(actor,
                assignment.getClassSubject().getClassroom().getId(), EnrollmentStatus.ACTIVE))
            throw new ApiException(HttpStatus.FORBIDDEN, "Only enrolled students may submit this assignment");
        if (!assignment.isPublished()) conflict("Assignment is not published");
        if (assignment.getClosedAt() != null) conflict("Submissions are closed");
        if (request.attachmentUrl() != null && request.attachmentUrl().startsWith("/")) {
            StoredFile file;
            try {
                if (!request.attachmentUrl().startsWith("/api/v1/files/")) throw new IllegalArgumentException();
                file = files.findById(UUID.fromString(request.attachmentUrl().substring(14))).orElseThrow();
            } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid attachment");
            }
            if (!actor.equals(file.getOwnerId()) || file.getAssignment() == null || !id.equals(file.getAssignment().getId()))
                throw new ApiException(HttpStatus.FORBIDDEN, "This attachment does not belong to your assignment");
        }
        if ((request.answerText() == null || request.answerText().isBlank())
                && (request.attachmentUrl() == null || request.attachmentUrl().isBlank()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide answer text or an attachment");
        Submission submission = submissions.findByAssignmentIdAndStudentId(id, actor).orElseGet(Submission::new);
        Instant now = Instant.now();
        if (submission.getId() != null) {
            if (submission.getStatus() == SubmissionStatus.GRADED) conflict("Graded work cannot be resubmitted");
            if (now.isAfter(assignment.getDueAt())) conflict("The resubmission deadline has passed");
            // Preserve the previous version before updating the current submission.
            SubmissionRevision previous = new SubmissionRevision();
            previous.setSubmission(submission); previous.setAnswerText(submission.getAnswerText());
            previous.setAttachmentUrl(submission.getAttachmentUrl()); previous.setSubmittedAt(submission.getSubmittedAt());
            previous.setStatus(submission.getStatus()); revisions.save(previous);
        }
        User student = users.getReferenceById(actor);
        submission.setAssignment(assignment); submission.setStudent(student);
        submission.setAnswerText(request.answerText()); submission.setAttachmentUrl(request.attachmentUrl());
        submission.setSubmittedAt(now); submission.setMarks(null); submission.setFeedback(null); submission.setGradedBy(null);
        submission.setStatus(now.isAfter(assignment.getDueAt()) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);
        submission = submissions.save(submission);
        notifications.notifySubjectTeachers(assignment.getClassSubject(), "Assignment submitted",
                student.getFirstName() + " submitted " + assignment.getTitle(), NotificationType.SUBMISSION, "SUBMISSION", submission.getId());
        audit.record(assignment.getClassSubject().getClassroom().getProgram().getDepartment().getId(),actor, "SUBMIT_ASSIGNMENT", "Submission", submission.getId(), assignment.getTitle());
        return DtoMapper.submission(submission);
    }

    private Assignment teachingAssignment(UUID id, UUID actor) {
        Assignment assignment = locked(id);
        access.requireTeach(actor, assignment.getClassSubject().getId());
        return assignment;
    }
    private Assignment locked(UUID id) {
        return assignments.findLockedById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }
    private void conflict(String message) { throw new ApiException(HttpStatus.CONFLICT, message); }
}
