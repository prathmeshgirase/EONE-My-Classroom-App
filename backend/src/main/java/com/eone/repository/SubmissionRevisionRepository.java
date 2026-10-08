package com.eone.repository;

import com.eone.domain.SubmissionRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface SubmissionRevisionRepository extends JpaRepository<SubmissionRevision, UUID> {
    Page<SubmissionRevision> findBySubmissionIdOrderBySubmittedAtDesc(UUID id, Pageable pageable);
}
