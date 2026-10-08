package com.eone.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="password_resets") @Getter @Setter
public class PasswordReset extends BaseEntity {
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean used;
}
