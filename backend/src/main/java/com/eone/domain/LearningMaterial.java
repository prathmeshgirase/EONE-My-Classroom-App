package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="learning_materials")
@Getter @Setter @NoArgsConstructor
public class LearningMaterial extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="chapter_id") private Chapter chapter;
    @Column(nullable=false, length=180) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private MaterialType type;
    @Column(nullable=false, length=1000) private String url;
    @Column(columnDefinition="text") private String description;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
}
