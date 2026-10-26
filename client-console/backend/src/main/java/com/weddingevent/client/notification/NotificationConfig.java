package com.weddingevent.client.notification;

import com.weddingevent.client.config.AppProperties;
import com.weddingevent.common.repository.DeviceTokenRepository;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    /** FCM when {@code app.fcm.credentials-file} is set (FN-NOTI-02), otherwise pushes are only logged */
    @Bean
    PushSender pushSender(AppProperties props, DeviceTokenRepository devices, Clock clock) throws Exception {
        AppProperties.Fcm fcm = props.fcm();
        if (fcm == null || !fcm.enabled()) {
            return new LoggingPushSender();
        }
        return new FcmPushSender(fcm.projectId(), Path.of(fcm.credentialsFile()), fcm.baseUrl(), devices, clock);
    }
}
