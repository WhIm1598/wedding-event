package com.weddingevent.client.notification;

import java.util.UUID;

/** Push delivery to a user's devices (Firebase Cloud Messaging in production). */
public interface PushSender {

    void send(UUID userId, String title, String body);
}
