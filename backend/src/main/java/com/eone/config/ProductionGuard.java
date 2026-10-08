package com.eone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Profile("production")
public class ProductionGuard {
    public ProductionGuard(@Value("${eone.jwt.secret}") String secret,
                           @Value("${eone.seed.enabled}") boolean seed,
                           @Value("${eone.cors.allowed-origins}") String origins) {
        String normalized = secret.toLowerCase(Locale.ROOT);
        if (seed || secret.getBytes(StandardCharsets.UTF_8).length < 64
                || normalized.contains("development") || normalized.contains("change-this")
                || normalized.contains("replace-with") || normalized.contains("test-secret")) {
            throw new IllegalStateException("Production requires disabled demo seeding and a unique JWT secret of at least 64 bytes");
        }
        for (String origin : origins.split(",", -1)) {
            URI uri = URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                throw new IllegalStateException("Production CORS origins must be explicit HTTPS origins");
            }
        }
    }
}
