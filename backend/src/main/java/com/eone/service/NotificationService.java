package com.eone.service;

import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final EnrollmentRepository enrollments;
    private final RoleAssignmentRepository roles;

    public void notifyUser(User user, String title, String body, NotificationType type, String relatedType, UUID relatedId) {
        Notification n = new Notification();
        n.setUser(user); n.setTitle(title); n.setBody(body); n.setType(type); n.setRelatedType(relatedType); n.setRelatedId(relatedId);
        n.setPushDueAt(java.time.Instant.now());
        notifications.save(n);
    }

    @Transactional
    public void notifyClassroomStudents(Classroom classroom, String title, String body, NotificationType type, String relatedType, UUID relatedId) {
        enrollments.findByClassroomIdAndStatus(classroom.getId(), EnrollmentStatus.ACTIVE)
                .forEach(e -> notifyUser(e.getStudent(), title, body, type, relatedType, relatedId));
    }

    @Transactional
    public void notifySubjectTeachers(ClassSubject cs, String title, String body, NotificationType type, String relatedType, UUID relatedId) {
        Set<UUID> seen = new HashSet<>();
        roles.findByClassSubjectIdAndRoleAndActiveTrue(cs.getId(), ScopedRole.SUBJECT_TEACHER).forEach(r -> {
            if (seen.add(r.getUser().getId())) notifyUser(r.getUser(), title, body, type, relatedType, relatedId);
        });
        roles.findByClassroomIdAndRoleAndActiveTrue(cs.getClassroom().getId(), ScopedRole.CLASS_TEACHER).forEach(r -> {
            if (seen.add(r.getUser().getId())) notifyUser(r.getUser(), title, body, type, relatedType, relatedId);
        });
    }
}
