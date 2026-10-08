package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> { List<ClassSubject> findByClassroomId(UUID classroomId); Optional<ClassSubject> findByClassroomIdAndSubjectId(UUID classroomId, UUID subjectId); }
