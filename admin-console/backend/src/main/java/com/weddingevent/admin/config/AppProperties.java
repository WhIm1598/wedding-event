package com.weddingevent.admin.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
/** {@code publicBaseUrl}: origin of the customer's public contract page {@code /c/{token}} (FN-PUB-CONTR-01) */
public record AppProperties(ZoneId zone, Jwt jwt, Cors cors, DemoData demoData, String publicBaseUrl, Zalo zalo) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("app.jwt.secret (env JWT_SECRET) must be set to at least 32 bytes");
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {}

    public record DemoData(boolean enabled) {}

    /** Zalo endpoints; overridable so tests can point them at a stub */
    public record Zalo(String oauthUrl, String znsUrl) {}
}
