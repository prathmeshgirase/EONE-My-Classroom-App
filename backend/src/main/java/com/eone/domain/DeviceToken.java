package com.eone.domain;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name="device_tokens") @Getter @Setter
public class DeviceToken extends BaseEntity {
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false, unique=true, length=512) private String token;
}
