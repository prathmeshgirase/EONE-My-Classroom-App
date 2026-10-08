package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubjectRepository extends JpaRepository<Subject, UUID> { List<Subject> findByDepartmentId(UUID departmentId); }
