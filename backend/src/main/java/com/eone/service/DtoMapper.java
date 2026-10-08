package com.eone.service;

import com.eone.api.Dtos;
import com.eone.domain.*;

public final class DtoMapper {
    private DtoMapper() {}

    public static Dtos.UserView user(User u) {
        var d = u.getDepartment();
        return new Dtos.UserView(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getAccountType(), u.getStatus(), d == null ? null : d.getId(), d == null ? null : d.getName());
    }

    public static Dtos.ClassroomView classroom(Classroom c) {
        return new Dtos.ClassroomView(c.getId(), c.getName(), c.getProgram().getCode(), c.getProgram().getName(),
                c.getYearLevel().name(), c.getDivision(), c.getAcademicTerm().getName(), c.getProgram().getDepartment().getId());
    }

    public static Dtos.SubjectView subject(ClassSubject cs) {
        return new Dtos.SubjectView(cs.getId(), cs.getSubject().getId(), cs.getSubject().getCode(), cs.getSubject().getName(),
                cs.getClassroom().getId(), cs.getClassroom().getName());
    }

    public static Dtos.MaterialView material(LearningMaterial m) {
        return new Dtos.MaterialView(m.getId(), m.getTitle(), m.getType(), m.getUrl(), m.getDescription(), m.getCreatedAt());
    }

    public static Dtos.SubmissionView submission(Submission s) {
        return new Dtos.SubmissionView(s.getId(), s.getStudent().getId(), s.getStudent().getFirstName()+" "+s.getStudent().getLastName(),
                s.getAnswerText(), s.getAttachmentUrl(), s.getStatus(), s.getSubmittedAt(), s.getMarks(), s.getFeedback());
    }

    public static Dtos.NotificationView notification(Notification n) {
        return new Dtos.NotificationView(n.getId(), n.getTitle(), n.getBody(), n.getType(), n.getRelatedType(), n.getRelatedId(), n.getCreatedAt(), n.getReadAt());
    }
}
