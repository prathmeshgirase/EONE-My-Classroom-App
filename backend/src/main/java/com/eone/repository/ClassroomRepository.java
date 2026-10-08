package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> { List<Classroom> findByProgramDepartmentId(UUID departmentId); }
