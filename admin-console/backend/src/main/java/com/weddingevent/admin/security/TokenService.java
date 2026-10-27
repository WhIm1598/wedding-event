package com.weddingevent.admin.security;

import com.weddingevent.admin.config.AppProperties;
import com.weddingevent.common.domain.RefreshToken;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * Issues access (2h) and refresh (30d) tokens — spec FN-ADM-AUTH-01 step 4. Every refresh token has a
 * {@code refresh_tokens} row whose id is its {@code jti} (FN-ADM-AUTH-02; same table as the mobile FN-AUTH-07).
 */
@Service
public class TokenService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_STAFF_ID = "staffId";
    /** The console user's studio (ADR-0001); absent for the platform admin */
    public static final String CLAIM_STUDIO_ID = "studioId";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public record TokenPair(String accessToken, String refreshToken) {}

    private final JwtEncoder encoder;
    private final JwtDecoder refreshDecoder;
    private final RefreshTokenRepository refreshTokens;
    private final AppProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SecretKey jwtSigningKey, RefreshTokenRepository refreshTokens, AppProperties props,
            Clock clock) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.refreshDecoder = SecurityConfig.decoder(jwtSigningKey, props, TYPE_REFRESH);
        this.props = props;
        this.clock = clock;
    }

    /** Call inside a transaction: the refresh token's row is saved with the pair */
    public TokenPair issue(User user) {
        RefreshToken row = new RefreshToken();
        row.setUser(user);
        row.setExpiresAt(clock.instant().plus(props.jwt().refreshTokenTtl()));
        refreshTokens.save(row);
        return new TokenPair(
                encode(user, TYPE_ACCESS, props.jwt().accessTokenTtl(), null),
                encode(user, TYPE_REFRESH, props.jwt().refreshTokenTtl(), row.getId().toString()));
    }

    /** The {@code jti} of a valid admin refresh token; anything else (tokens from before rotation have none) → 401 */
    public UUID refreshTokenId(String token) {
        try {
            String jti = refreshDecoder.decode(token).getId();
            if (jti == null) {
                throw invalidRefreshToken();
            }
            return UUID.fromString(jti);
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
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(CLAIM_ROLES, List.of(user.getRole().name()));
        if (jti != null) {
            claims.id(jti);
        }
        if (user.getStaffMember() != null) {
            claims.claim(CLAIM_STAFF_ID, user.getStaffMember().getId().toString());
        }
        if (user.getStudioId() != null) {
            claims.claim(CLAIM_STUDIO_ID, user.getStudioId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
