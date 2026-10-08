package com.eone.repository;

import com.eone.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update RefreshToken r set r.revoked=true where r.user.id=:user")
    void revokeAll(UUID user);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
}
