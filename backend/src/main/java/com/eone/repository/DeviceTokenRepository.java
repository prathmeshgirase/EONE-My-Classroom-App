package com.eone.repository;
import com.eone.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByToken(String token);
    List<DeviceToken> findByUserId(UUID userId);
    void deleteByUserIdAndToken(UUID userId, String token);
}
