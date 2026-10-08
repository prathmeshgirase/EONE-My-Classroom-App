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

import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/v1/me") @RequiredArgsConstructor @Transactional(readOnly=true)
public class MeController {
    private final UserRepository users;
    private final ContextService contexts;
    private final NotificationRepository notifications;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final AccessService access;

    @GetMapping
    public Dtos.UserView me(Authentication auth) {
        return DtoMapper.user(users.findById(CurrentUser.id(auth)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"User not found")));
    }

    @GetMapping("/contexts")
    public List<Dtos.ContextView> contexts(Authentication auth) { return contexts.contexts(CurrentUser.id(auth)); }

    @GetMapping("/dashboard")
    public Dtos.DashboardView dashboard(Authentication auth) {
        UUID userId = CurrentUser.id(auth);
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"User not found"));
        List<Dtos.ClassroomView> cs = contexts.visibleClassrooms(userId).stream().map(DtoMapper::classroom).toList();
        List<Dtos.AssignmentView> upcoming = assignments.dashboard(userId,Instant.now().minusSeconds(86400),
                org.springframework.data.domain.PageRequest.of(0,20)).stream().map(a -> assignmentView(a,u)).toList();
        return new Dtos.DashboardView(DtoMapper.user(u), contexts.contexts(userId), cs, notifications.countByUserIdAndReadAtIsNull(userId), upcoming);
    }

    private Dtos.AssignmentView assignmentView(Assignment a, User u) {
        Dtos.SubmissionView mine = u.getAccountType() == AccountType.STUDENT
                ? submissions.findByAssignmentIdAndStudentId(a.getId(), u.getId()).map(DtoMapper::submission).orElse(null) : null;
        return new Dtos.AssignmentView(a.getId(), a.getClassSubject().getId(), a.getClassSubject().getClassroom().getId(), a.getClassSubject().getClassroom().getName(), a.getClassSubject().getSubject().getName(), a.getTitle(),
                a.getDescription(), a.getDueAt(), a.getMaxMarks(), a.isPublished(), mine,a.getClosedAt());
    }
}
