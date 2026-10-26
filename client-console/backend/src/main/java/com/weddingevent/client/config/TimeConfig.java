package com.weddingevent.client.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Business clock (Asia/Ho_Chi_Minh) and scheduled jobs (RefreshTokenCleanup) */
@Configuration
@EnableScheduling
public class TimeConfig {

    /** Business dates ("today", "at least 7 days from today") are evaluated in Asia/Ho_Chi_Minh. */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(props.zone());
    }
}
