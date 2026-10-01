package com.weddingevent.admin.security;

import com.weddingevent.admin.config.AppProperties;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/** Issues access (2h) and refresh (30d) tokens — spec FN-AUTH-01 step 5. */
@Service
public class TokenService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_STAFF_ID = "staffId";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public record TokenPair(String accessToken, String refreshToken) {}

    private final JwtEncoder encoder;
    private final JwtDecoder refreshDecoder;
    private final AppProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, SecretKey jwtSigningKey, AppProperties props, Clock clock) {
        this.encoder = encoder;
        this.refreshDecoder = SecurityConfig.decoder(jwtSigningKey, props, TYPE_REFRESH);
        this.props = props;
        this.clock = clock;
    }

    public TokenPair issue(User user) {
        return new TokenPair(
                encode(user, TYPE_ACCESS, props.jwt().accessTokenTtl()),
                encode(user, TYPE_REFRESH, props.jwt().refreshTokenTtl()));
    }

    /** Validates a refresh token and returns the user id it was issued for. */
    public UUID verifyRefreshToken(String token) {
        try {
            Jwt jwt = refreshDecoder.decode(token);
            return UUID.fromString(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw AppException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }
    }

    private String encode(User user, String type, Duration ttl) {
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
        if (user.getStaffMember() != null) {
            claims.claim(CLAIM_STAFF_ID, user.getStaffMember().getId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
