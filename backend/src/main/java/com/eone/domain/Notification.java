package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="notifications")
@Getter @Setter @NoArgsConstructor
public class Notification extends BaseEntity {
    @Column(nullable=false) private int pushAttempts;
    private Instant pushDueAt;
    private Instant pushDeliveredAt;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, columnDefinition="text") private String body;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private NotificationType type;
    @Column(name="related_type", length=40) private String relatedType;
    @Column(name="related_id") private UUID relatedId;
    @Column(name="read_at") private Instant readAt;
}
