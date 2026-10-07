package com.weddingevent.client.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.client.notification.LoggingPushSender;
import com.weddingevent.common.exception.AppException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Production adapters for e-mail OTP, push and Google sign-in (replaced by test doubles in the API tests). */
class IntegrationAdaptersTest {

    @Test
    void googleLoginIsOffUntilAClientIdIsConfigured() {
        assertThatThrownBy(() -> new NimbusGoogleIdTokenVerifier(List.of()).verify("anything"))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCode()).isEqualTo("INVALID_GOOGLE_TOKEN");
                });
    }

    @Test
    void malformedGoogleTokensAreRejectedWithoutCallingGoogle() {
        var verifier = new NimbusGoogleIdTokenVerifier(List.of("client-id.apps.googleusercontent.com"));
        for (String token : new String[] {"not-a-jwt", "", "a.b.c"}) {
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_GOOGLE_TOKEN"));
        }
    }

    @Test
    void loggingSendersNeverFail() {
        assertThatCode(() -> new LoggingOtpSender(true).send("ngoc@example.com", "1234")).doesNotThrowAnyException();
        assertThatCode(() -> new LoggingOtpSender(false).send("ngoc@example.com", "1234")).doesNotThrowAnyException();
        assertThatCode(() -> new LoggingPushSender().send(UUID.randomUUID(), "Tiêu đề", "Nội dung")).doesNotThrowAnyException();
    }

    @Test
    void emailsAreMaskedInLogs() {
        assertThat(LoggingOtpSender.mask("ngoc.hoang@example.com")).isEqualTo("n***@example.com");
        assertThat(LoggingOtpSender.mask("x")).isEqualTo("***");
    }
}
