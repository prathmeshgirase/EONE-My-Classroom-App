package com.eone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(@Value("${eone.jwt.secret}") String secret,
                      @Value("${eone.jwt.access-minutes}") long accessMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessMinutes);
    }

    public String createAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("issuedAtMillis", now.toEpochMilli())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public UUID parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
    public boolean issuedAfter(String token, Instant changedAt) {
        if (changedAt==null) return true;
        Claims claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long millis=claims.get("issuedAtMillis",Long.class);
        Instant issued=millis!=null ? Instant.ofEpochMilli(millis) : claims.getIssuedAt()==null ? Instant.EPOCH : claims.getIssuedAt().toInstant();
        return !issued.isBefore(changedAt);
    }
}
