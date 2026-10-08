package com.eone.api;

import com.eone.domain.*;
import com.eone.repository.*;
import com.eone.security.CurrentUser;
import com.eone.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/v1/classrooms") @RequiredArgsConstructor @Transactional(readOnly=true)
public class ClassroomController {
    private final ContextService contextService;
    private final AccessService access;
    private final ClassroomRepository classrooms;
    private final ClassSubjectRepository classSubjects;
    private final EnrollmentRepository enrollments;
    private final AnnouncementRepository announcements;

    @GetMapping
    public List<Dtos.ClassroomView> list(Authentication auth) {
        return contextService.visibleClassrooms(CurrentUser.id(auth)).stream().map(DtoMapper::classroom).toList();
    }

    @GetMapping("/{id}")
    public Dtos.ClassroomView get(@PathVariable UUID id, Authentication auth) {
        access.requireViewClassroom(CurrentUser.id(auth), id);
        return DtoMapper.classroom(classrooms.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Classroom not found")));
    }

    @GetMapping("/{id}/subjects")
    public List<Dtos.SubjectView> subjects(@PathVariable UUID id, Authentication auth) {
        access.requireViewClassroom(CurrentUser.id(auth), id);
        return classSubjects.findByClassroomId(id).stream()
                .filter(cs -> access.canViewClassSubject(CurrentUser.id(auth), cs.getId()))
                .map(DtoMapper::subject).toList();
    }

    @GetMapping("/{id}/students")
    public List<Dtos.UserView> students(@PathVariable UUID id, Authentication auth) {
        access.requireViewRoster(CurrentUser.id(auth), id);
        return enrollments.findByClassroomIdAndStatus(id, EnrollmentStatus.ACTIVE).stream().map(e -> DtoMapper.user(e.getStudent())).toList();
    }

    @GetMapping("/{id}/announcements")
    public List<Dtos.AnnouncementView> announcements(@PathVariable UUID id, Authentication auth) {
        access.requireViewClassroom(CurrentUser.id(auth), id);
        return announcements.findByClassroomIdOrderByCreatedAtDesc(id).stream().map(this::view).toList();
    }

    private Dtos.AnnouncementView view(Announcement a) {
        return new Dtos.AnnouncementView(a.getId(), a.getTitle(), a.getBody(), a.getTargetType(), a.getCreatedAt(),
                a.getCreatedBy().getFirstName()+" "+a.getCreatedBy().getLastName());
    }
}
