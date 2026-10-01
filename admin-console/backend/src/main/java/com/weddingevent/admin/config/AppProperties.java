package com.weddingevent.admin.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(ZoneId zone, Jwt jwt, Cors cors, String publicBaseUrl, DemoData demoData) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("app.jwt.secret (env JWT_SECRET) must be set to at least 32 bytes");
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {}

    public record DemoData(boolean enabled) {}
}
