package com.eone;

import com.eone.domain.ScopedRole;
import com.eone.repository.*;
import com.eone.service.AccessService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessServiceTest {
    @Test
    void hodDoesNotAutomaticallyBecomeSubjectTeacher() {
        var roles = mock(RoleAssignmentRepository.class);
        var enrollments = mock(EnrollmentRepository.class);
        var classSubjects = mock(ClassSubjectRepository.class);
        var classrooms = mock(ClassroomRepository.class);
        var access = new AccessService(roles, enrollments, classSubjects, classrooms);

        UUID userId = UUID.randomUUID();
        UUID classSubjectId = UUID.randomUUID();
        when(classSubjects.findById(classSubjectId)).thenReturn(java.util.Optional.of(mock(com.eone.domain.ClassSubject.class)));
        when(roles.existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(userId, ScopedRole.SUBJECT_TEACHER, classSubjectId)).thenReturn(false);

        assertFalse(access.canTeach(userId, classSubjectId));
        verify(roles).existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(userId, ScopedRole.SUBJECT_TEACHER, classSubjectId);
    }

    @Test
    void explicitSubjectTeacherCanTeachAssignedClassSubject() {
        var roles = mock(RoleAssignmentRepository.class);
        var enrollments = mock(EnrollmentRepository.class);
        var classSubjects = mock(ClassSubjectRepository.class);
        var classrooms = mock(ClassroomRepository.class);
        var access = new AccessService(roles, enrollments, classSubjects, classrooms);

        UUID userId = UUID.randomUUID();
        UUID classSubjectId = UUID.randomUUID();
        when(classSubjects.findById(classSubjectId)).thenReturn(java.util.Optional.of(mock(com.eone.domain.ClassSubject.class)));
        when(roles.existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(userId, ScopedRole.SUBJECT_TEACHER, classSubjectId)).thenReturn(true);

        assertTrue(access.canTeach(userId, classSubjectId));
    }
}
