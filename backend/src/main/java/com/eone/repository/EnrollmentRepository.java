package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);
    List<Enrollment> findByClassroomIdAndStatus(UUID classroomId, EnrollmentStatus status);
    boolean existsByStudentIdAndClassroomIdAndStatus(UUID studentId, UUID classroomId, EnrollmentStatus status);
    Optional<Enrollment> findByStudentIdAndClassroomId(UUID studentId, UUID classroomId);
}
