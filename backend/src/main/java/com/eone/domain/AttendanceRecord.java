package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="attendance_records", uniqueConstraints=@UniqueConstraint(columnNames={"session_id","student_id"}))
@Getter @Setter @NoArgsConstructor
public class AttendanceRecord extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="session_id") private AttendanceSession session;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="student_id") private User student;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private AttendanceStatus status;
}
