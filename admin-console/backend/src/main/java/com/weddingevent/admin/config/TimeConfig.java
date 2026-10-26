package com.weddingevent.admin.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TimeConfig {

    /** Business dates ("today", "this month") are evaluated in the studio's time zone. */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(props.zone());
    }
}
