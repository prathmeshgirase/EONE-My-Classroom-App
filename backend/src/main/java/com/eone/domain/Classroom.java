package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="classrooms", uniqueConstraints=@UniqueConstraint(columnNames={"program_id","academic_term_id","year_level","division"}))
@Getter @Setter @NoArgsConstructor
public class Classroom extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="program_id") private Program program;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="academic_term_id") private AcademicTerm academicTerm;
    @Enumerated(EnumType.STRING) @Column(name="year_level", nullable=false, length=30) private YearLevel yearLevel;
    @Column(nullable=false, length=30) private String division;
    @Column(nullable=false, length=120) private String name;
}
