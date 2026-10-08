package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> { Optional<AttendanceRecord> findBySessionIdAndStudentId(UUID sessionId, UUID studentId); List<AttendanceRecord> findByStudentId(UUID studentId); List<AttendanceRecord> findBySessionId(UUID sessionId); }
