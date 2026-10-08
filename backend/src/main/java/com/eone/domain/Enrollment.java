package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="enrollments", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","classroom_id"}))
@Getter @Setter @NoArgsConstructor
public class Enrollment extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="student_id") private User student;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="classroom_id") private Classroom classroom;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
}
