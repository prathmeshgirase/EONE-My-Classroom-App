package com.eone.api;
import com.eone.repository.AuditLogRepository;
import com.eone.service.AccessService;
import com.eone.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hod/departments/{department}/audit") @RequiredArgsConstructor @Transactional(readOnly=true)
public class AuditController {
    private final AuditLogRepository logs;
    private final AccessService access;
    public record Entry(UUID id,UUID actor,String action,String entityType,UUID entityId,String details,Instant createdAt) {}
    @GetMapping public Dtos.PageView<Entry> list(@PathVariable UUID department,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="") String action,Authentication auth) {
        access.requireHod(CurrentUser.id(auth),department);
        return Dtos.PageView.of(logs.scoped(department,action.trim(),Paging.of(page,size,"createdAt"))
            .map(a -> new Entry(a.getId(),a.getActorUserId(),a.getAction(),a.getEntityType(),a.getEntityId(),a.getMetadata(),a.getCreatedAt())));
    }
}
