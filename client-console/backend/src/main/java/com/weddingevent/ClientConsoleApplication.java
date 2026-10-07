package com.weddingevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * WedPlanner mobile API (couples and vendors). Lives in the root package so component, entity and repository
 * scanning also covers the shared {@code com.weddingevent.common} module.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ClientConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientConsoleApplication.class, args);
    }
}
