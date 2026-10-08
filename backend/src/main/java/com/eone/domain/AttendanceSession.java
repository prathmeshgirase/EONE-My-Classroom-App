package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name="attendance_sessions")
@Getter @Setter @NoArgsConstructor
public class AttendanceSession extends BaseEntity {
    @Column(length=64) private String sessionKey;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="class_subject_id") private ClassSubject classSubject;
    @Column(name="session_date", nullable=false) private LocalDate sessionDate;
    @Column(length=180) private String topic;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
}
