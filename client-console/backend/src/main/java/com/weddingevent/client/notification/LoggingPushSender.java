package com.weddingevent.client.notification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used while FCM isn't configured (dev, test: {@code app.fcm.credentials-file} empty). Notifications are still stored in
 * the inbox, so nothing is lost; this only logs that a push would have been sent.
 */
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(UUID userId, PushMessage message) {
        log.info("Push to user {}: {} {}", userId, message.title() == null ? "(data)" : message.title(), message.data());
    }
}
