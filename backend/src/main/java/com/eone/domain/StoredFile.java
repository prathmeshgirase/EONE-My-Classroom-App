package com.eone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "stored_files") @Getter @Setter
public class StoredFile extends BaseEntity {
    @Column(nullable = false) private UUID ownerId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chapter_id") private Chapter chapter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignment_id") private Assignment assignment;
    @Column(nullable = false, unique = true) private String storageKey;
    @Column(nullable = false) private String filename;
    @Column(nullable = false) private String contentType;
    @Column(nullable = false) private long byteSize;
}
