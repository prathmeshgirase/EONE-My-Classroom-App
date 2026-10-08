package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProgramRepository extends JpaRepository<Program, UUID> { List<Program> findByDepartmentId(UUID departmentId); }
