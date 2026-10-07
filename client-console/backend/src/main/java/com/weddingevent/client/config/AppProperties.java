package com.weddingevent.client.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(ZoneId zone, Jwt jwt, Cors cors, Otp otp, Google google, AiPlanner aiPlanner, DemoData demoData) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("app.jwt.secret (env JWT_SECRET) must be set to at least 32 bytes");
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {}

    /** FN-AUTH-02: validity and attempt limit; {@code logCode} prints codes in dev (no e-mail sender yet) */
    public record Otp(Duration ttl, int maxAttempts, boolean logCode) {}

    /** OAuth client ids the mobile app's Google ID tokens must be issued for; empty disables Google login */
    public record Google(List<String> clientIds) {
        public List<String> clientIdsOrEmpty() {
            return clientIds == null ? List.of() : clientIds.stream().filter(id -> !id.isBlank()).toList();
        }
    }

    /** FN-PLAN-03 in-house AI planner; blank base URL = built-in rule-based planner */
    public record AiPlanner(String baseUrl, Duration timeout) {}

    public record DemoData(boolean enabled) {}
}
