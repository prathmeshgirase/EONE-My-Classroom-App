package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="assignments")
@Getter @Setter @NoArgsConstructor
public class Assignment extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="class_subject_id") private ClassSubject classSubject;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, columnDefinition="text") private String description;
    @Column(name="due_at", nullable=false) private Instant dueAt;
    @Column(name="max_marks", nullable=false) private double maxMarks;
    @Column(nullable=false) private boolean published = true;
    @Column(name="closed_at") private Instant closedAt;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
}
