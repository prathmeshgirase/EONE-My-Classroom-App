package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
    List<AttendanceSession> findByClassSubjectIdOrderBySessionDateDesc(UUID classSubjectId);
    @org.springframework.data.jpa.repository.Query("select count(s)>0 from AttendanceSession s where s.classSubject.id=:subject and s.sessionDate=:date and lower(trim(coalesce(s.topic,'')))=:topic")
    boolean duplicate(UUID subject,java.time.LocalDate date,String topic);
}
