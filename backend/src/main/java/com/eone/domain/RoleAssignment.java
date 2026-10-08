package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="role_assignments")
@Getter @Setter @NoArgsConstructor
public class RoleAssignment extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private ScopedRole role;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="classroom_id") private Classroom classroom;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="class_subject_id") private ClassSubject classSubject;
    @Column(nullable=false) private boolean active = true;
}
