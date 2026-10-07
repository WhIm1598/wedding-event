package com.weddingevent.client.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    /** Replace with an FCM-backed sender once device tokens are registered */
    @Bean
    PushSender pushSender() {
        return new LoggingPushSender();
    }
}
