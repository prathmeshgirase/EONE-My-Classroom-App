package com.eone;

import com.eone.api.ApiException;
import com.eone.domain.*;
import com.eone.repository.UserRepository;
import com.eone.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RefreshRotationTest {
    @Autowired RefreshTokenService tokens;
    @Autowired UserRepository users;

    @Test void tokenCanOnlyBeConsumedOnceAcrossConcurrentTransactions() throws Exception {
        var user = new User(); user.setEmail(UUID.randomUUID() + "@test.invalid");
        user.setPasswordHash("test-only"); user.setFirstName("Refresh"); user.setLastName("Test");
        user.setAccountType(AccountType.STAFF); user.setStatus(UserStatus.ACTIVE);
        var raw = tokens.issue(users.save(user));
        var start = new CountDownLatch(1);
        Callable<Boolean> consume = () -> {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
            try { tokens.consume(raw); return true; }
            catch (ApiException ex) { assertThat(ex.getStatus().value()).isEqualTo(401); return false; }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(consume); var second = executor.submit(consume);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThatThrownBy(() -> tokens.consume(raw)).isInstanceOf(ApiException.class);
    }
}
