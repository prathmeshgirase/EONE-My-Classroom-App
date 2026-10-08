package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByUserIdAndActiveTrue(UUID userId);
    boolean existsByUserIdAndRoleAndDepartmentIdAndActiveTrue(UUID userId, ScopedRole role, UUID departmentId);
    boolean existsByUserIdAndRoleAndClassroomIdAndActiveTrue(UUID userId, ScopedRole role, UUID classroomId);
    boolean existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(UUID userId, ScopedRole role, UUID classSubjectId);
    List<RoleAssignment> findByClassSubjectIdAndRoleAndActiveTrue(UUID classSubjectId, ScopedRole role);
    List<RoleAssignment> findByClassroomIdAndRoleAndActiveTrue(UUID classroomId, ScopedRole role);
    List<RoleAssignment> findByDepartmentIdAndRoleAndActiveTrue(UUID departmentId, ScopedRole role);
}
