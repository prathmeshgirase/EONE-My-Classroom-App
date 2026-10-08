package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.Query("select a from AuditLog a where a.departmentId=:department and (:action='' or a.action=:action)")
    org.springframework.data.domain.Page<AuditLog> scoped(UUID department,String action,org.springframework.data.domain.Pageable pageable);
}
