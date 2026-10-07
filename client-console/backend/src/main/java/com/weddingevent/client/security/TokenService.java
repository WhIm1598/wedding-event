package com.weddingevent.client.security;

import com.weddingevent.client.config.AppProperties;
import com.weddingevent.common.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues access (2 h) and refresh (30 d) tokens — FN-AUTH-01 step 4. Tokens carry no role: a mobile user picks the
 * role after signing in (FN-AUTH-03), so {@link UserAuthenticationConverter} reads it from the database per request.
 */
@Service
public class TokenService {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_EMAIL = "email";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public record TokenPair(String accessToken, String refreshToken) {}

    private final JwtEncoder encoder;
    private final AppProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, AppProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public TokenPair issue(User user) {
        return new TokenPair(
                encode(user, TYPE_ACCESS, props.jwt().accessTokenTtl()),
                encode(user, TYPE_REFRESH, props.jwt().refreshTokenTtl()));
    }

    private String encode(User user, String type, Duration ttl) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_EMAIL, user.getEmail())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
