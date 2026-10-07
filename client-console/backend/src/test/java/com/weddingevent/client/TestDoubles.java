package com.weddingevent.client;

import com.weddingevent.client.auth.GoogleIdTokenVerifier;
import com.weddingevent.client.auth.OtpSender;
import com.weddingevent.client.notification.PushSender;
import com.weddingevent.common.exception.AppException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the external integrations (e-mail, Google, FCM) with in-memory doubles the tests can inspect. */
@TestConfiguration
public class TestDoubles {

    /** Remembers the last OTP sent to each e-mail address */
    public static class RecordingOtpSender implements OtpSender {
        private final Map<String, String> lastCode = new ConcurrentHashMap<>();

        @Override
        public void send(String email, String code) {
            lastCode.put(email.toLowerCase(), code);
        }

        public String lastCodeFor(String email) {
            return lastCode.get(email.toLowerCase());
        }

        public void forget(String email) {
            lastCode.remove(email.toLowerCase());
        }
    }

    public record Push(UUID userId, String title, String body) {}

    public static class RecordingPushSender implements PushSender {
        private final List<Push> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(UUID userId, String title, String body) {
            sent.add(new Push(userId, title, body));
        }

        public List<Push> sentTo(UUID userId) {
            return sent.stream().filter(p -> p.userId().equals(userId)).toList();
        }
    }

    @Bean
    @Primary
    RecordingOtpSender recordingOtpSender() {
        return new RecordingOtpSender();
    }

    @Bean
    @Primary
    RecordingPushSender recordingPushSender() {
        return new RecordingPushSender();
    }

    /** Accepts tokens of the form {@code google|<email>|<name>|<verified>} */
    @Bean
    @Primary
    GoogleIdTokenVerifier fakeGoogleVerifier() {
        return idToken -> {
            String[] parts = idToken == null ? new String[0] : idToken.split("\\|");
            if (parts.length != 4 || !parts[0].equals("google")) {
                throw AppException.unauthorized("INVALID_GOOGLE_TOKEN", "Không thể xác thực tài khoản Google");
            }
            return new GoogleIdTokenVerifier.GoogleProfile(parts[1], parts[2], "https://example.com/a.png", Boolean.parseBoolean(parts[3]));
        };
    }
}
