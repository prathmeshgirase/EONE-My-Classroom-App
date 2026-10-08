package com.eone.api;

import com.eone.domain.*;
import com.eone.repository.*;
import com.eone.security.CurrentUser;
import com.eone.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/v1/attendance") @RequiredArgsConstructor @Transactional
public class AttendanceController {
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final ClassSubjectRepository classSubjects;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AttendanceAnalyticsService analytics;

    @GetMapping("/me/summary")
    public List<AttendanceAnalyticsService.SubjectSummary> mySummary(Authentication auth) {
        return analytics.mine(CurrentUser.id(auth));
    }

    @GetMapping("/classrooms/{classroomId}/summary")
    public Dtos.PageView<AttendanceAnalyticsService.Summary> summary(@PathVariable UUID classroomId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="101") double below, Authentication auth) {
        return analytics.classroom(CurrentUser.id(auth), classroomId, page, size, below);
    }

    @PostMapping("/class-subjects/{classSubjectId}/sessions") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.AttendanceSessionView create(@PathVariable UUID classSubjectId, @Valid @RequestBody Dtos.AttendanceCreateRequest req, Authentication auth) {
        UUID userId=CurrentUser.id(auth); access.requireTeach(userId,classSubjectId);
        ClassSubject cs=classSubjects.findById(classSubjectId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Class subject not found"));
        String topic=req.topic()==null?"":req.topic().trim().toLowerCase(Locale.ROOT);
        if(sessions.duplicate(classSubjectId,req.sessionDate(),topic))
            throw new ApiException(HttpStatus.CONFLICT,"An attendance session already exists for this date and topic");
        Set<UUID> enrolled=enrollments.findByClassroomIdAndStatus(cs.getClassroom().getId(),EnrollmentStatus.ACTIVE).stream().map(e->e.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
        Set<UUID> marked = new HashSet<>();
        for (var mark : req.records()) {
            if (!marked.add(mark.studentId()))
                throw new ApiException(HttpStatus.BAD_REQUEST, "A student may only be marked once per session");
        }
        for(var mark:req.records()) if(!enrolled.contains(mark.studentId())) throw new ApiException(HttpStatus.BAD_REQUEST,"Attendance contains a student not enrolled in this classroom");
        AttendanceSession s=new AttendanceSession();s.setClassSubject(cs);s.setSessionDate(req.sessionDate());s.setTopic(req.topic());
        s.setSessionKey(RecoveryService.hash(req.sessionDate()+"|"+topic));
        s.setCreatedBy(users.getReferenceById(userId));s=sessions.save(s);
        List<Dtos.AttendanceRecordView> result=new ArrayList<>();
        for(var mark:req.records()){
            User student=users.getReferenceById(mark.studentId()); AttendanceRecord r=new AttendanceRecord();r.setSession(s);r.setStudent(student);r.setStatus(mark.status());records.save(r);
            result.add(new Dtos.AttendanceRecordView(student.getId(),student.getFirstName()+" "+student.getLastName(),mark.status()));
            if(mark.status()==AttendanceStatus.ABSENT) notifications.notifyUser(student,"Attendance marked absent",cs.getSubject().getName()+" - "+req.sessionDate(),NotificationType.ATTENDANCE,"ATTENDANCE_SESSION",s.getId());
        }
        audit.record(cs.getClassroom().getProgram().getDepartment().getId(),userId,"CREATE_ATTENDANCE","AttendanceSession",s.getId(),cs.getSubject().getName()+" "+req.sessionDate());
        return new Dtos.AttendanceSessionView(s.getId(),classSubjectId,s.getSessionDate(),s.getTopic(),result);
    }

    @GetMapping("/class-subjects/{classSubjectId}/sessions")
    public List<Dtos.AttendanceSessionView> sessions(@PathVariable UUID classSubjectId, Authentication auth){
        UUID userId=CurrentUser.id(auth); access.requireViewClassSubject(userId,classSubjectId);
        return sessions.findByClassSubjectIdOrderBySessionDateDesc(classSubjectId).stream().map(s->sessionView(s,userId)).toList();
    }

    @GetMapping("/me")
    public List<Dtos.AttendanceSessionView> mine(Authentication auth){
        UUID userId=CurrentUser.id(auth);
        return records.findByStudentId(userId).stream().map(AttendanceRecord::getSession).distinct().map(s->sessionView(s,userId)).toList();
    }

    private Dtos.AttendanceSessionView sessionView(AttendanceSession s, UUID viewer){
        boolean teacher=access.canTeach(viewer,s.getClassSubject().getId());
        List<AttendanceRecord> source=records.findBySessionId(s.getId());
        if(!teacher) source=source.stream().filter(r->r.getStudent().getId().equals(viewer)).toList();
        var views=source.stream().map(r->new Dtos.AttendanceRecordView(r.getStudent().getId(),r.getStudent().getFirstName()+" "+r.getStudent().getLastName(),r.getStatus())).toList();
        return new Dtos.AttendanceSessionView(s.getId(),s.getClassSubject().getId(),s.getSessionDate(),s.getTopic(),views);
    }
}
