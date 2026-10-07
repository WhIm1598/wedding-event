package com.weddingevent.client.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /** Business dates ("today", "at least 7 days from today") are evaluated in Asia/Ho_Chi_Minh. */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(props.zone());
    }
}
