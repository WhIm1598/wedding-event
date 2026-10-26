package com.weddingevent.client.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.weddingevent.client.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT (HS256) security for the mobile API. Roles come from the database on every request
 * ({@link UserAuthenticationConverter}). Rules are enforced here, before body parsing, and repeated with
 * {@code @PreAuthorize} on the controllers.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String COUPLE = "hasRole('COUPLE')";
    public static final String COUPLE_OR_VENDOR = "hasAnyRole('COUPLE', 'VENDOR')";
    public static final String VENDOR = "hasRole('VENDOR')";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder accessTokenDecoder, UserAuthenticationConverter converter,
            SecurityErrorHandler errors) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/google",
                                "/api/v1/auth/send-otp", "/api/v1/auth/verify-otp").permitAll()
                        // WebSocket handshake; the STOMP CONNECT frame carries the token (StompAuthInterceptor)
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers("/api/v1/client/plans/**", "/api/v1/client/guests/**", "/api/v1/client/quote-requests/**")
                                .hasRole("COUPLE")
                        .requestMatchers("/api/v1/client/vendor/**").hasRole("VENDOR")
                        .requestMatchers("/api/v1/client/services/**", "/api/v1/messages/**").hasAnyRole("COUPLE", "VENDOR")
                        // Signed in, any role (or none yet): profile, role picker, inbox
                        .requestMatchers("/api/v1/auth/me", "/api/v1/users/**", "/api/v1/client/notifications/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(accessTokenDecoder).jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    @Bean
    SecretKey jwtSigningKey(AppProperties props) {
        return new SecretKeySpec(props.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /**
     * Accepts only access tokens issued by this service: refresh tokens can't call the API, and admin-console tokens
     * (another issuer) are rejected even when both services share the signing secret.
     */
    @Bean
    JwtDecoder accessTokenDecoder(SecretKey jwtSigningKey, AppProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> accessOnly = jwt -> TokenService.TYPE_ACCESS.equals(jwt.getClaimAsString(TokenService.CLAIM_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong token type", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(props.jwt().issuer()), accessOnly));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The native app doesn't need CORS; this only matters for browser-based tools. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null ? List.of() : props.cors().allowedOrigins();
        config.setAllowedOrigins(origins.stream().filter(o -> !o.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
