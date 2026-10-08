package com.eone;

import com.eone.config.ProductionGuard;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProductionGuardTest {
    private final String secret = "a".repeat(64);
    @Test void acceptsExplicitHttpsConfiguration() {
        assertThatCode(() -> new ProductionGuard(secret, false, "https://college.example"))
                .doesNotThrowAnyException();
    }
    @Test void rejectsDemoSeedAndPlaceholderSecrets() {
        assertThatThrownBy(() -> new ProductionGuard(secret, true, "https://college.example"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionGuard("development".repeat(8), false, "https://college.example"))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsInsecureAndWildcardOrigins() {
        for (String origin : new String[]{"http://college.example", "*", "https://*.example", "https://college.example/path", ""}) {
            assertThatThrownBy(() -> new ProductionGuard(secret, false, origin))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
