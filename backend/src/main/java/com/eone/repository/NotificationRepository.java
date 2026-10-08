package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.pushDueAt <= :now and n.pushAttempts < 5 order by n.pushDueAt")
    List<Notification> pendingPush(Instant now, org.springframework.data.domain.Pageable pageable);
    @Query("select n from Notification n where n.user.id = :actor and (:unread = false or n.readAt is null)")
    org.springframework.data.domain.Page<Notification> inbox(UUID actor, boolean unread, org.springframework.data.domain.Pageable pageable);
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now, n.updatedAt = :now where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
