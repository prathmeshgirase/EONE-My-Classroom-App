package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="subjects", uniqueConstraints=@UniqueConstraint(columnNames={"department_id","code"}))
@Getter @Setter @NoArgsConstructor
public class Subject extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
    @Column(nullable=false, length=30) private String code;
    @Column(nullable=false, length=120) private String name;
}
