package com.weddingevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Lumière Studios admin API. Lives in the root package so component, entity and repository
 * scanning also covers the shared {@code com.weddingevent.common} module.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminConsoleApplication.class, args);
    }
}
