package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"classSubject","classSubject.classroom","classSubject.subject"})
    @org.springframework.data.jpa.repository.Query("""
        select a from Assignment a where a.published=true and a.dueAt>=:since and (
          exists(select e.id from Enrollment e where e.student.id=:actor and e.classroom=a.classSubject.classroom and e.status=com.eone.domain.EnrollmentStatus.ACTIVE)
          or exists(select r.id from RoleAssignment r where r.user.id=:actor and r.active=true and (
            (r.role=com.eone.domain.ScopedRole.SUBJECT_TEACHER and r.classSubject=a.classSubject)
            or (r.role=com.eone.domain.ScopedRole.CLASS_TEACHER and r.classroom=a.classSubject.classroom)
            or (r.role=com.eone.domain.ScopedRole.HOD and r.department=a.classSubject.classroom.program.department))))
        order by a.dueAt,a.id
        """)
    List<Assignment> dashboard(UUID actor,java.time.Instant since,org.springframework.data.domain.Pageable page);
    List<Assignment> findByClassSubjectIdAndPublishedTrueOrderByDueAtAsc(UUID classSubjectId);
    List<Assignment> findByClassSubjectIdOrderByDueAtAsc(UUID classSubjectId);
    List<Assignment> findByClassSubjectClassroomIdAndPublishedTrueOrderByDueAtAsc(UUID classroomId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Assignment a where a.id = :id")
    Optional<Assignment> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
