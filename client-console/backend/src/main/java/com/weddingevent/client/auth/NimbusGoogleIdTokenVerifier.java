package com.weddingevent.client.auth;

import com.weddingevent.common.exception.AppException;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validates Google ID tokens locally: RS256 signature against Google's published keys (fetched and cached on first
 * use), issuer, expiry and audience = one of the configured OAuth client ids. No client id configured = Google login off.
 */
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final List<String> clientIds;
    private final NimbusJwtDecoder decoder;

    public NimbusGoogleIdTokenVerifier(List<String> clientIds) {
        this.clientIds = List.copyOf(clientIds);
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> issuer = jwt -> ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(this.clientIds::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), issuer, audience));
    }

    @Override
    public GoogleProfile verify(String idToken) {
        if (clientIds.isEmpty()) {
            throw invalid("Đăng nhập Google chưa được cấu hình");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw invalid("Tài khoản Google không có email");
            }
            return new GoogleProfile(email, jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
        } catch (JwtException | IllegalArgumentException e) {
            throw invalid("Không thể xác thực tài khoản Google");
        }
    }

    private static AppException invalid(String message) {
        return AppException.unauthorized("INVALID_GOOGLE_TOKEN", message);
    }
}
