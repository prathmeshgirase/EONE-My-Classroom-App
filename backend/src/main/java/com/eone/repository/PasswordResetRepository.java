package com.eone.repository;
import com.eone.domain.PasswordReset;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface PasswordResetRepository extends JpaRepository<PasswordReset,UUID> {
    Optional<PasswordReset> findByTokenHash(String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<PasswordReset> findByTokenHashAndUsedFalse(String hash);
    long countByUserIdAndCreatedAtAfter(UUID userId,Instant after);
    @Modifying @Query("update PasswordReset p set p.used=true where p.userId=:user") void invalidate(UUID user);
}
