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

@RestController @RequestMapping("/api/v1/announcements") @RequiredArgsConstructor @Transactional
public class AnnouncementController {
    private final AnnouncementRepository announcements;
    private final DepartmentRepository departments;
    private final ClassroomRepository classrooms;
    private final ClassSubjectRepository classSubjects;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final RoleAssignmentRepository roles;
    private final AccessService access;
    private final NotificationService notifications;
    private final AuditService audit;

    @GetMapping("/page")
    @Transactional(readOnly = true)
    public Dtos.PageView<Dtos.AnnouncementView> page(@RequestParam UUID scope,
            @RequestParam TargetType target, @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            Authentication auth) {
        UUID actor = CurrentUser.id(auth);
        var pageable = Paging.of(page, size, "createdAt");
        if (search.length() > 200) throw new ApiException(HttpStatus.BAD_REQUEST, "Search must be at most 200 characters");
        switch (target) {
            case CLASSROOM -> access.requireViewClassroom(actor, scope);
            case SUBJECT -> access.requireViewClassSubject(actor, scope);
            case DEPARTMENT -> {
                User viewer = users.findById(actor).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Account unavailable"));
                if (!access.isHod(actor, scope) && (viewer.getDepartment() == null || !scope.equals(viewer.getDepartment().getId())))
                    throw new ApiException(HttpStatus.FORBIDDEN, "Department access required");
            }
        }
        return Dtos.PageView.of(announcements.search(scope, target, search.trim(), pageable).map(this::view));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Dtos.AnnouncementView create(@Valid @RequestBody Dtos.AnnouncementCreateRequest req, Authentication auth){
        UUID userId=CurrentUser.id(auth); Announcement a=new Announcement();a.setTitle(req.title());a.setBody(req.body());a.setTargetType(req.targetType());a.setCreatedBy(users.getReferenceById(userId));
        switch(req.targetType()){
            case DEPARTMENT -> { if(req.departmentId()==null) bad(); access.requireHod(userId,req.departmentId()); a.setDepartment(departments.getReferenceById(req.departmentId())); }
            case CLASSROOM -> { if(req.classroomId()==null) bad(); access.requireManageClassroom(userId,req.classroomId()); a.setClassroom(classrooms.getReferenceById(req.classroomId())); }
            case SUBJECT -> { if(req.classSubjectId()==null) bad(); access.requireTeach(userId,req.classSubjectId()); a.setClassSubject(classSubjects.getReferenceById(req.classSubjectId())); }
        }
        a=announcements.save(a); notifyAudience(a);
        UUID department = switch (a.getTargetType()) {
            case DEPARTMENT -> a.getDepartment().getId();
            case CLASSROOM -> a.getClassroom().getProgram().getDepartment().getId();
            case SUBJECT -> a.getClassSubject().getClassroom().getProgram().getDepartment().getId();
        };
        audit.record(department,userId,"CREATE_ANNOUNCEMENT","Announcement",a.getId(),a.getTargetType().name()); return view(a);
    }

    private void notifyAudience(Announcement a){
        if(a.getTargetType()==TargetType.CLASSROOM) notifications.notifyClassroomStudents(a.getClassroom(),a.getTitle(),a.getBody(),NotificationType.ANNOUNCEMENT,"ANNOUNCEMENT",a.getId());
        else if(a.getTargetType()==TargetType.SUBJECT) notifications.notifyClassroomStudents(a.getClassSubject().getClassroom(),a.getTitle(),a.getBody(),NotificationType.ANNOUNCEMENT,"ANNOUNCEMENT",a.getId());
        else {
            Set<UUID> seen=new HashSet<>();
            users.findByDepartmentId(a.getDepartment().getId()).stream().filter(u->u.getStatus()==UserStatus.ACTIVE).forEach(u->{if(seen.add(u.getId()))notifications.notifyUser(u,a.getTitle(),a.getBody(),NotificationType.ANNOUNCEMENT,"ANNOUNCEMENT",a.getId());});
        }
    }
    private void bad(){throw new ApiException(HttpStatus.BAD_REQUEST,"Target id is required for the selected target type");}
    private Dtos.AnnouncementView view(Announcement a){return new Dtos.AnnouncementView(a.getId(),a.getTitle(),a.getBody(),a.getTargetType(),a.getCreatedAt(),a.getCreatedBy().getFirstName()+" "+a.getCreatedBy().getLastName());}
}
