package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findByClassroomIdOrderByCreatedAtDesc(UUID classroomId);
    List<Announcement> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);
    List<Announcement> findByClassSubjectIdOrderByCreatedAtDesc(UUID classSubjectId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "createdBy")
    @org.springframework.data.jpa.repository.Query("""
        select a from Announcement a where
        (a.classroom.id = :scope or a.classSubject.id = :scope or a.department.id = :scope)
        and a.targetType = :target
        and (lower(a.title) like lower(concat('%', :search, '%'))
             or lower(a.body) like lower(concat('%', :search, '%')))
        """)
    org.springframework.data.domain.Page<Announcement> search(UUID scope, TargetType target, String search,
        org.springframework.data.domain.Pageable pageable);
}
