package com.eone.service;
import com.eone.api.ApiException;
import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class RecoveryService {
    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final RefreshTokenRepository refresh;
    private final PasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    @Value("${eone.mail.enabled:false}") private boolean enabled;
    public record ResetMail(String email,String token) {}
    public void request(String email) {
        if(!enabled) return;
        users.findByEmailIgnoreCase(email.trim()).filter(u->u.getStatus()==UserStatus.ACTIVE).ifPresent(found->{
            var user=users.findLockedById(found.getId()).orElseThrow();
            if(resets.countByUserIdAndCreatedAtAfter(user.getId(),Instant.now().minusSeconds(3600))>=3) return;
            byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
            String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            var reset=new PasswordReset();reset.setUserId(user.getId());reset.setTokenHash(hash(token));reset.setExpiresAt(Instant.now().plusSeconds(1800));resets.save(reset);
            events.publishEvent(new ResetMail(user.getEmail(),token));
        });
    }
    public void reset(String token,String password) {
        validate(password);
        var candidate=resets.findByTokenHash(hash(token)).orElseThrow(()->invalid());
        var user=users.findLockedById(candidate.getUserId()).orElseThrow(()->invalid());
        var reset=resets.findByTokenHashAndUsedFalse(hash(token)).orElseThrow(()->invalid());
        if(!reset.getExpiresAt().isAfter(Instant.now())) throw invalid();
        if(user.getStatus()!=UserStatus.ACTIVE) throw invalid();
        update(user,password); reset.setUsed(true);
    }
    public void change(UUID actor,String current,String next) {
        validate(next);
        var user=users.findLockedById(actor).orElseThrow(()->invalid());
        if(!passwords.matches(current,user.getPasswordHash())) throw new ApiException(HttpStatus.BAD_REQUEST,"Current password is incorrect");
        update(user,next);
    }
    private void update(User user,String next) {
        user.setPasswordHash(passwords.encode(next));
        user.setCredentialsChangedAt(Instant.ofEpochMilli(Instant.now().toEpochMilli()));
        refresh.revokeAll(user.getId());resets.invalidate(user.getId());
    }
    private void validate(String password) {
        if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(HttpStatus.BAD_REQUEST,"Use at least 12 characters and at most 72 UTF-8 bytes");
    }
    private ApiException invalid(){return new ApiException(HttpStatus.BAD_REQUEST,"Reset code is invalid or expired");}
    public static String hash(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
