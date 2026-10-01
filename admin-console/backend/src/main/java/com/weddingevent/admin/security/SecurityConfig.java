package com.weddingevent.admin.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.weddingevent.admin.config.AppProperties;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT (HS256) security. Every /api/v1/admin/** call needs ROLE_ADMIN or ROLE_STAFF;
 * admin-only operations are guarded with @PreAuthorize on the controllers.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ADMIN = "hasRole('ADMIN')";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder accessTokenDecoder, SecurityErrorHandler errors) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/auth/login", "/api/v1/admin/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        // Admin-only operations are enforced here, before request parsing/validation, so
                        // staff get 403 rather than validation details. @PreAuthorize repeats them as defense in depth.
                        .requestMatchers("/api/v1/admin/dashboard/**", "/api/v1/admin/financials/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/staff/*/tasks", "/api/v1/admin/staff/tasks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/packages", "/api/v1/admin/assets",
                                "/api/v1/admin/staff", "/api/v1/admin/contracts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/packages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/settings/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(accessTokenDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    /** The "roles" claim already holds ROLE_* authorities */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.CLAIM_ROLES);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecretKey jwtSigningKey(AppProperties props) {
        return new SecretKeySpec(props.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Resource-server decoder: accepts only access tokens (refresh tokens can't call the API). */
    @Bean
    JwtDecoder accessTokenDecoder(SecretKey jwtSigningKey, AppProperties props) {
        return decoder(jwtSigningKey, props, TokenService.TYPE_ACCESS);
    }

    static NimbusJwtDecoder decoder(SecretKey key, AppProperties props, String requiredType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> type = jwt -> requiredType.equals(jwt.getClaimAsString(TokenService.CLAIM_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong token type", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(props.jwt().issuer()), type));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Only needed when the SPA is served from another origin; the Vite proxy / nginx gateway avoid CORS. */
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
