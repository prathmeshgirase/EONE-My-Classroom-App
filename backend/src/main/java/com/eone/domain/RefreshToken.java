package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean revoked;
}
