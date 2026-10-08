package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name="audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog extends BaseEntity {
    @Column(name="department_id") private UUID departmentId;
    @Column(name="actor_user_id") private UUID actorUserId;
    @Column(nullable=false, length=80) private String action;
    @Column(name="entity_type", nullable=false, length=80) private String entityType;
    @Column(name="entity_id") private UUID entityId;
    @Column(columnDefinition="text") private String metadata;
}
