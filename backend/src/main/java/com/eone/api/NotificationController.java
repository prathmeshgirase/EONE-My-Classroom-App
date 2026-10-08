package com.eone.api;

import com.eone.repository.NotificationRepository;
import com.eone.security.CurrentUser;
import com.eone.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor @Transactional
public class NotificationController {
    private final NotificationRepository notifications;

    @GetMapping("/page")
    public Dtos.PageView<Dtos.NotificationView> page(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="false") boolean unread, Authentication auth) {
        return Dtos.PageView.of(notifications.inbox(CurrentUser.id(auth), unread, Paging.of(page,size,"createdAt")).map(DtoMapper::notification));
    }

    @GetMapping
    public List<Dtos.NotificationView> list(Authentication auth){
        return notifications.findTop100ByUserIdOrderByCreatedAtDesc(CurrentUser.id(auth)).stream().map(DtoMapper::notification).toList();
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable UUID id, Authentication auth){
        notifications.findById(id).filter(n->n.getUser().getId().equals(CurrentUser.id(auth))).ifPresent(n->n.setReadAt(Instant.now()));
    }

    @PostMapping("/read-all")
    public void readAll(Authentication auth){
        notifications.markAllRead(CurrentUser.id(auth), Instant.now());
    }
}
