package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="submissions", uniqueConstraints=@UniqueConstraint(columnNames={"assignment_id","student_id"}))
@Getter @Setter @NoArgsConstructor
public class Submission extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="assignment_id") private Assignment assignment;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="student_id") private User student;
    @Column(name="answer_text", columnDefinition="text") private String answerText;
    @Column(name="attachment_url", length=1000) private String attachmentUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private SubmissionStatus status;
    @Column(name="submitted_at") private Instant submittedAt;
    private Double marks;
    @Column(columnDefinition="text") private String feedback;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="graded_by") private User gradedBy;
}
