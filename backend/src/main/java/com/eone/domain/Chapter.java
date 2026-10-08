package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="chapters", uniqueConstraints=@UniqueConstraint(columnNames={"class_subject_id","position"}))
@Getter @Setter @NoArgsConstructor
public class Chapter extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="class_subject_id") private ClassSubject classSubject;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false) private int position;
}
