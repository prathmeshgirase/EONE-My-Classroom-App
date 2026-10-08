package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ChapterRepository extends JpaRepository<Chapter, UUID> { List<Chapter> findByClassSubjectIdOrderByPositionAsc(UUID classSubjectId); }
