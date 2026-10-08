package com.eone.service;

import com.eone.api.Dtos;
import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service @RequiredArgsConstructor
public class ContextService {
    private final RoleAssignmentRepository roles;
    private final EnrollmentRepository enrollments;
    private final ClassroomRepository classrooms;

    public List<Dtos.ContextView> contexts(UUID userId) {
        List<Dtos.ContextView> out = new ArrayList<>();
        for (RoleAssignment r : roles.findByUserIdAndActiveTrue(userId)) {
            Department d = r.getDepartment();
            Classroom c = r.getClassroom();
            ClassSubject cs = r.getClassSubject();
            if (d == null && c != null) d = c.getProgram().getDepartment();
            if (d == null && cs != null) d = cs.getClassroom().getProgram().getDepartment();
            if (c == null && cs != null) c = cs.getClassroom();
            out.add(new Dtos.ContextView(r.getRole().name(), d == null ? null : d.getId(), d == null ? null : d.getName(),
                    c == null ? null : c.getId(), c == null ? null : c.getName(),
                    cs == null ? null : cs.getId(), cs == null ? null : cs.getSubject().getName(), r.getRole().name()));
        }
        for (Enrollment e : enrollments.findByStudentIdAndStatus(userId, EnrollmentStatus.ACTIVE)) {
            Classroom c = e.getClassroom(); Department d = c.getProgram().getDepartment();
            out.add(new Dtos.ContextView("STUDENT", d.getId(), d.getName(), c.getId(), c.getName(), null, null, "STUDENT"));
        }
        return out;
    }

    public List<Classroom> visibleClassrooms(UUID userId) {
        LinkedHashMap<UUID, Classroom> found = new LinkedHashMap<>();
        for (RoleAssignment r : roles.findByUserIdAndActiveTrue(userId)) {
            if (r.getClassroom() != null) found.put(r.getClassroom().getId(), r.getClassroom());
            if (r.getClassSubject() != null) {
                Classroom c = r.getClassSubject().getClassroom(); found.put(c.getId(), c);
            }
            if (r.getRole() == ScopedRole.HOD && r.getDepartment() != null) {
                classrooms.findByProgramDepartmentId(r.getDepartment().getId()).forEach(c -> found.put(c.getId(), c));
            }
        }
        enrollments.findByStudentIdAndStatus(userId, EnrollmentStatus.ACTIVE).forEach(e -> found.put(e.getClassroom().getId(), e.getClassroom()));
        return new ArrayList<>(found.values());
    }
}
