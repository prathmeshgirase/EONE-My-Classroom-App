package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DepartmentRepository extends JpaRepository<Department, UUID> { Optional<Department> findByCodeIgnoreCase(String code); }
