package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="programs", uniqueConstraints=@UniqueConstraint(columnNames={"department_id","code"}))
@Getter @Setter @NoArgsConstructor
public class Program extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
    @Column(nullable=false, length=30) private String code;
    @Column(nullable=false, length=120) private String name;
    public Program(Department department, String code, String name){this.department=department;this.code=code;this.name=name;}
}
