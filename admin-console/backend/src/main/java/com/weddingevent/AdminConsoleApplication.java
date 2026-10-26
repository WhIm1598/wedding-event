package com.weddingevent;

import java.util.TimeZone;
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

    // The business runs on Vietnam time (spec §1.1): pinned here so production behaves the same whatever the
    // server's or container's zone is. Runs when the class loads, so also before tests start their context.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AdminConsoleApplication.class, args);
    }
}
