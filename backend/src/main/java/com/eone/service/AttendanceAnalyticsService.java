package com.eone.service;

import com.eone.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceAnalyticsService {
    private final JdbcTemplate jdbc;
    private final AccessService access;
    public record Summary(UUID studentId, String studentName, long present, long absent, long late, long excused, Double percentage) {}

    public record SubjectSummary(UUID classSubjectId, String subject, long present, long absent, long late, long excused, Double percentage) {}
    public List<SubjectSummary> mine(UUID actor) {
        return jdbc.query("""
            select cs.id, sub.name,
              count(case when r.status='PRESENT' then 1 end) as present,
              count(case when r.status='ABSENT' then 1 end) as absent,
              count(case when r.status='LATE' then 1 end) as late,
              count(case when r.status='EXCUSED' then 1 end) as excused,
              100.0 * count(case when r.status in ('PRESENT','LATE') then 1 end)
                / nullif(count(case when r.status in ('PRESENT','ABSENT','LATE') then 1 end),0) as percentage
            from enrollments e join class_subjects cs on cs.classroom_id=e.classroom_id
            join subjects sub on sub.id=cs.subject_id
            left join attendance_sessions s on s.class_subject_id=cs.id
            left join attendance_records r on r.session_id=s.id and r.student_id=e.student_id
            where e.student_id=? and e.status='ACTIVE'
            group by cs.id, sub.name order by sub.name, cs.id
            """, (r,n) -> new SubjectSummary(r.getObject("id",UUID.class),r.getString("name"),
                r.getLong("present"),r.getLong("absent"),r.getLong("late"),r.getLong("excused"),
                r.getObject("percentage")==null ? null : r.getDouble("percentage")),actor);
    }

    private static final String CLASS_SUMMARY = """
        select u.id as student_id, u.first_name, u.last_name,
          count(case when r.status = 'PRESENT' then 1 end) as present,
          count(case when r.status = 'ABSENT' then 1 end) as absent,
          count(case when r.status = 'LATE' then 1 end) as late,
          count(case when r.status = 'EXCUSED' then 1 end) as excused,
          100.0 * count(case when r.status in ('PRESENT','LATE') then 1 end)
            / nullif(count(case when r.status in ('PRESENT','ABSENT','LATE') then 1 end), 0) as percentage
        from enrollments e join users u on u.id = e.student_id
        left join attendance_records r on r.student_id = e.student_id and r.session_id in (
          select s.id from attendance_sessions s join class_subjects cs on cs.id = s.class_subject_id where cs.classroom_id = ?)
        where e.classroom_id = ? and e.status = 'ACTIVE'
        group by u.id, u.first_name, u.last_name
        """;

    public Dtos.PageView<Summary> classroom(UUID actor, UUID classroom, int page, int size, double below) {
        access.requireManageClassroom(actor, classroom);
        if (page < 0 || page > 100000 || size < 1 || size > 100 || !Double.isFinite(below) || below < 0 || below > 101)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use page 0–100000, size 1–100 and below 0–101");
        String filtered = "select * from (" + CLASS_SUMMARY + ") summary where (? > 100 or percentage < ?)";
        Long count = jdbc.queryForObject("select count(*) from (" + filtered + ") counted", Long.class, classroom, classroom, below, below);
        List<Summary> rows = jdbc.query(filtered + " order by last_name, first_name, student_id limit ? offset ?", (r, n) ->
                new Summary(r.getObject("student_id", UUID.class), r.getString("first_name") + " " + r.getString("last_name"),
                        r.getLong("present"), r.getLong("absent"), r.getLong("late"), r.getLong("excused"),
                        r.getObject("percentage") == null ? null : r.getDouble("percentage")),
                classroom, classroom, below, below, size, (long) page * size);
        long total = count == null ? 0 : count;
        return new Dtos.PageView<>(rows, page, size, total, (int) ((total + size - 1) / size));
    }
}
