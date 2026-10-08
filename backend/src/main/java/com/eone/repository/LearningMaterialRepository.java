package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> { List<LearningMaterial> findByChapterIdOrderByCreatedAtDesc(UUID chapterId); }
