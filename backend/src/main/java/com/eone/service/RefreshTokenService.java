package com.eone.service;

import com.eone.api.ApiException;
import com.eone.domain.*;
import com.eone.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    @Value("${eone.jwt.refresh-days}") private long refreshDays;

    public String issue(User user) {
        String raw = UUID.randomUUID()+"."+UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setUser(user); token.setTokenHash(hash(raw)); token.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshDays))); token.setRevoked(false);
        repo.save(token);
        return raw;
    }

    @Transactional
    public User consume(String raw) {
        RefreshToken token = repo.findByTokenHashAndRevokedFalse(hash(raw))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!token.getExpiresAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        token.setRevoked(true);
        return token.getUser();
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHashAndRevokedFalse(hash(raw)).ifPresent(t -> t.setRevoked(true));
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
