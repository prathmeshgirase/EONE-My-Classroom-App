package com.eone.repository;
import com.eone.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {}
