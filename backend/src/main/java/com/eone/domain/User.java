package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
    private java.time.Instant credentialsChangedAt;
    @Column(nullable=false, unique=true, length=190) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="first_name", nullable=false, length=80) private String firstName;
    @Column(name="last_name", nullable=false, length=80) private String lastName;
    @Column(length=30) private String phone;
    @Enumerated(EnumType.STRING) @Column(name="account_type", nullable=false, length=20) private AccountType accountType;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private UserStatus status;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
}
