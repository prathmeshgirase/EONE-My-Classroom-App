package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    Optional<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);
    List<Submission> findByAssignmentIdOrderBySubmittedAtAsc(UUID assignmentId);

    @org.springframework.data.jpa.repository.Query("select s.assignment.id from Submission s where s.id = :id")
    Optional<UUID> assignmentId(@org.springframework.data.repository.query.Param("id") UUID id);

    interface Statistics {
        long getStudents(); long getSubmitted(); long getGraded(); long getLate();
    }
    @org.springframework.data.jpa.repository.Query(value = """
            select count(e.id) as students, count(s.id) as submitted,
                   count(case when s.status = 'GRADED' then 1 end) as graded,
                   count(case when s.submitted_at > a.due_at then 1 end) as late
            from assignments a join class_subjects cs on cs.id = a.class_subject_id
            join enrollments e on e.classroom_id = cs.classroom_id and e.status = 'ACTIVE'
            left join submissions s on s.assignment_id = a.id and s.student_id = e.student_id
            where a.id = :id
            """, nativeQuery = true)
    Statistics statistics(@org.springframework.data.repository.query.Param("id") UUID id);
}
