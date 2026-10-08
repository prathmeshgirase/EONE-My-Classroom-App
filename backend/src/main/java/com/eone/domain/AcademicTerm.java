package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name="academic_terms")
@Getter @Setter @NoArgsConstructor
public class AcademicTerm extends BaseEntity {
    @Column(nullable=false, unique=true, length=60) private String name;
    @Column(name="start_date", nullable=false) private LocalDate startDate;
    @Column(name="end_date", nullable=false) private LocalDate endDate;
    @Column(nullable=false) private boolean active;
    public AcademicTerm(String name, LocalDate startDate, LocalDate endDate, boolean active){this.name=name;this.startDate=startDate;this.endDate=endDate;this.active=active;}
}
