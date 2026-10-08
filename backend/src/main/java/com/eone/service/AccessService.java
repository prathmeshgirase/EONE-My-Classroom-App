package com.eone.service;

import com.eone.api.ApiException;
import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final RoleAssignmentRepository roles;
    private final EnrollmentRepository enrollments;
    private final ClassSubjectRepository classSubjects;
    private final ClassroomRepository classrooms;

    public boolean isHod(UUID userId, UUID departmentId) {
        return roles.existsByUserIdAndRoleAndDepartmentIdAndActiveTrue(userId, ScopedRole.HOD, departmentId);
    }

    public boolean canManageClassroom(UUID userId, UUID classroomId) {
        Classroom c = classrooms.findById(classroomId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Classroom not found"));
        return isHod(userId, c.getProgram().getDepartment().getId()) ||
                roles.existsByUserIdAndRoleAndClassroomIdAndActiveTrue(userId, ScopedRole.CLASS_TEACHER, classroomId);
    }

    public boolean canTeach(UUID userId, UUID classSubjectId) {
        ClassSubject cs = classSubjects.findById(classSubjectId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Class subject not found"));
        return roles.existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(userId, ScopedRole.SUBJECT_TEACHER, classSubjectId);
    }

    public boolean canViewClassroom(UUID userId, UUID classroomId) {
        if (canManageClassroom(userId, classroomId)) return true;
        boolean student = enrollments.existsByStudentIdAndClassroomIdAndStatus(userId, classroomId, EnrollmentStatus.ACTIVE);
        if (student) return true;
        return roles.findByUserIdAndActiveTrue(userId).stream().anyMatch(r ->
                r.getClassSubject() != null && r.getClassSubject().getClassroom().getId().equals(classroomId));
    }

    public boolean canViewClassSubject(UUID userId, UUID classSubjectId) {
        ClassSubject cs = classSubjects.findById(classSubjectId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Class subject not found"));
        return canTeach(userId, classSubjectId) || canManageClassroom(userId, cs.getClassroom().getId()) ||
                enrollments.existsByStudentIdAndClassroomIdAndStatus(userId, cs.getClassroom().getId(), EnrollmentStatus.ACTIVE);
    }


    public boolean canViewRoster(UUID userId, UUID classroomId) {
        if (canManageClassroom(userId, classroomId)) return true;
        return roles.findByUserIdAndActiveTrue(userId).stream().anyMatch(r ->
                r.getRole() == ScopedRole.SUBJECT_TEACHER && r.getClassSubject() != null &&
                r.getClassSubject().getClassroom().getId().equals(classroomId));
    }

    public void requireViewRoster(UUID userId, UUID classroomId) {
        if (!canViewRoster(userId, classroomId)) throw new ApiException(HttpStatus.FORBIDDEN, "Teaching or classroom-management permission required");
    }

    public void requireHod(UUID userId, UUID departmentId) {
        if (!isHod(userId, departmentId)) throw new ApiException(HttpStatus.FORBIDDEN, "HOD permission required for this department");
    }

    public void requireManageClassroom(UUID userId, UUID classroomId) {
        if (!canManageClassroom(userId, classroomId)) throw new ApiException(HttpStatus.FORBIDDEN, "Class teacher or HOD permission required");
    }

    public void requireTeach(UUID userId, UUID classSubjectId) {
        if (!canTeach(userId, classSubjectId)) throw new ApiException(HttpStatus.FORBIDDEN, "Teaching permission required for this subject");
    }

    public void requireViewClassroom(UUID userId, UUID classroomId) {
        if (!canViewClassroom(userId, classroomId)) throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this classroom");
    }

    public void requireViewClassSubject(UUID userId, UUID classSubjectId) {
        if (!canViewClassSubject(userId, classSubjectId)) throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this subject");
    }
}
