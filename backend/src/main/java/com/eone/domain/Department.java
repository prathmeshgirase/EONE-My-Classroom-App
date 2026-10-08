package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="departments")
@Getter @Setter @NoArgsConstructor
public class Department extends BaseEntity {
    @Column(nullable=false, unique=true, length=30) private String code;
    @Column(nullable=false, length=120) private String name;
    public Department(String code, String name) { this.code=code; this.name=name; }
}
