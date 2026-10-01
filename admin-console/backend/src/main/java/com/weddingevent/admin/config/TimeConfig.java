package com.weddingevent.admin.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /** Business dates ("today", "this month") are evaluated in the studio's time zone. */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(props.zone());
    }
}
