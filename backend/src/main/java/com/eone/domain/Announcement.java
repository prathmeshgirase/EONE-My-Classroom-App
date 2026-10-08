package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="announcements")
@Getter @Setter @NoArgsConstructor
public class Announcement extends BaseEntity {
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, columnDefinition="text") private String body;
    @Enumerated(EnumType.STRING) @Column(name="target_type", nullable=false, length=20) private TargetType targetType;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="classroom_id") private Classroom classroom;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="class_subject_id") private ClassSubject classSubject;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
}
