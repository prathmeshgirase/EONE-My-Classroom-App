package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="class_subjects", uniqueConstraints=@UniqueConstraint(columnNames={"classroom_id","subject_id"}))
@Getter @Setter @NoArgsConstructor
public class ClassSubject extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="classroom_id") private Classroom classroom;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="subject_id") private Subject subject;
}
