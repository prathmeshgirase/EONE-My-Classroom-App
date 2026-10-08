package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> { List<AcademicTerm> findByActiveTrue(); }
