package com.weddingevent.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the Flyway conventions in db/README.md without needing a database: file names, unique and gap-free
 * versions. (Immutability of released migrations is enforced in CI against the base branch.)
 */
class MigrationConventionsTest {

    private static final Pattern VERSIONED = Pattern.compile("V(\\d+)__[a-z0-9]+(_[a-z0-9]+)*\\.sql");
    private static final Pattern REPEATABLE = Pattern.compile("R__[a-z0-9]+(_[a-z0-9]+)*\\.sql");

    @Test
    void migrationsFollowNamingAndVersionRules() throws IOException, URISyntaxException {
        URL location = getClass().getClassLoader().getResource("db/migration");
        assertThat(location).as("classpath:db/migration").isNotNull();

        List<String> files;
        try (Stream<Path> list = Files.list(Path.of(location.toURI()))) {
            files = list.map(p -> p.getFileName().toString()).sorted().toList();
        }

        List<Integer> versions = new ArrayList<>();
        for (String name : files) {
            Matcher v = VERSIONED.matcher(name);
            if (v.matches()) {
                versions.add(Integer.parseInt(v.group(1)));
            } else {
                assertThat(REPEATABLE.matcher(name).matches())
                        .as("'%s' must be V{n}__lower_snake_case.sql or R__lower_snake_case.sql", name)
                        .isTrue();
            }
        }

        assertThat(versions).as("versioned migrations").isNotEmpty();
        List<Integer> sorted = versions.stream().sorted().toList();
        List<Integer> expected = Stream.iterate(1, i -> i + 1).limit(sorted.size()).toList();
        assertThat(sorted).as("versions must be unique and contiguous from 1 (files: %s)", files).isEqualTo(expected);
    }
}
