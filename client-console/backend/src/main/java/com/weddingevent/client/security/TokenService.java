package com.weddingevent.client.security;

import com.weddingevent.client.config.AppProperties;
import com.weddingevent.common.domain.RefreshToken;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Issues access (2 h) and refresh (30 d) tokens — FN-AUTH-01 step 4. Tokens carry no role: a mobile user picks the
 * role after signing in (FN-AUTH-03), so {@link UserAuthenticationConverter} reads it from the database per request.
 * Every refresh token has a {@code refresh_tokens} row whose id is its {@code jti} (FN-AUTH-07).
 */
@Service
public class TokenService {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_EMAIL = "email";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public record TokenPair(String accessToken, String refreshToken) {}

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder refreshDecoder;
    private final RefreshTokenRepository refreshTokens;
    private final AppProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SecretKey jwtSigningKey, RefreshTokenRepository refreshTokens, AppProperties props,
            Clock clock) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.props = props;
        this.clock = clock;
        // Not a bean: the API's only JwtDecoder must stay the access-token one (SecurityConfig, StompAuthInterceptor)
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        this.refreshDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.jwt().issuer()),
                jwt -> TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TYPE)) && jwt.getId() != null
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not a refresh token", null))));
    }

    /** Call inside a transaction: the refresh token's row is saved with the pair */
    public TokenPair issue(User user) {
        Instant now = clock.instant();
        RefreshToken row = new RefreshToken();
        row.setUser(user);
        row.setExpiresAt(now.plus(props.jwt().refreshTokenTtl()));
        refreshTokens.save(row);
        return new TokenPair(
                encode(user, TYPE_ACCESS, props.jwt().accessTokenTtl(), null),
                encode(user, TYPE_REFRESH, props.jwt().refreshTokenTtl(), row.getId().toString()));
    }

    /** The {@code jti} of a valid refresh token of this service; anything else → 401 INVALID_REFRESH_TOKEN */
    public UUID refreshTokenId(String token) {
        try {
            Jwt jwt = refreshDecoder.decode(token);
            return UUID.fromString(jwt.getId());
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidRefreshToken();
        }
    }

    public static AppException invalidRefreshToken() {
        return AppException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
    }

    private String encode(User user, String type, Duration ttl, String jti) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_EMAIL, user.getEmail());
        if (jti != null) {
            claims.id(jti);
        }
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
}
