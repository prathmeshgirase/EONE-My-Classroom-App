package com.eone.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "submission_revisions")
@Getter @Setter @NoArgsConstructor
public class SubmissionRevision extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id") private Submission submission;
    @Column(name = "answer_text", columnDefinition = "text") private String answerText;
    @Column(name = "attachment_url", length = 1000) private String attachmentUrl;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SubmissionStatus status;
}
