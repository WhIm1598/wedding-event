package com.weddingevent.client.notification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder until FCM is configured (TODO: device-token registration + firebase-admin). The notification is already
 * stored in the user's inbox, so nothing is lost; this only logs that a push would have been sent.
 */
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(UUID userId, String title, String body) {
        log.info("Push to user {}: {}", userId, title);
    }
}
