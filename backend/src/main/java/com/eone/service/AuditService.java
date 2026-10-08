package com.eone.service;

import com.eone.domain.AuditLog;
import com.eone.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;
    public void record(UUID actor, String action, String entityType, UUID entityId, String metadata) {
        record(null,actor,action,entityType,entityId,metadata);
    }
    public void record(UUID department, UUID actor, String action, String entityType, UUID entityId, String metadata) {
        AuditLog log = new AuditLog();
        log.setDepartmentId(department);
        log.setActorUserId(actor); log.setAction(action); log.setEntityType(entityType); log.setEntityId(entityId); log.setMetadata(metadata);
        repo.save(log);
    }
}
