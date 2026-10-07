package com.weddingevent.client.auth;

import com.weddingevent.client.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Adapters to external identity services; swap the beans to plug in a real e-mail provider. */
@Configuration
public class AuthConfig {

    @Bean
    OtpSender otpSender(AppProperties props) {
        return new LoggingOtpSender(props.otp().logCode());
    }

    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(AppProperties props) {
        return new NimbusGoogleIdTokenVerifier(props.google() == null ? java.util.List.of() : props.google().clientIdsOrEmpty());
    }
}
