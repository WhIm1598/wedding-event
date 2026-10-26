package com.weddingevent.client.notification;

import java.util.Map;
import java.util.UUID;

/** Push delivery to a user's registered devices (FN-NOTI-02, Firebase Cloud Messaging in production). */
public interface PushSender {

    /** Shown to the user when {@code title} is set; data-only (handled silently by the app) otherwise */
    record PushMessage(String title, String body, Map<String, String> data) {

        public static PushMessage dataOnly(Map<String, String> data) {
            return new PushMessage(null, null, data);
        }
    }

    /** Never throws: delivery problems are logged, the caller's request must not fail because of them */
    void send(UUID userId, PushMessage message);
}
