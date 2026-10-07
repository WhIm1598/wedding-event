package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Full-stack tests: real PostgreSQL + Flyway (the same migrations as the admin backend). One database and one Spring
 * context (on a random port, for the STOMP tests) are shared by all test classes; every test creates its own users,
 * so tests don't interfere. External integrations (e-mail OTP, Google, push) are replaced by {@link TestDoubles}.
 *
 * <p>By default PostgreSQL 16 runs in Docker via Testcontainers (CI). Without Docker, point the tests at an empty
 * database: {@code TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/wedding_client_test}
 * (+ {@code TEST_DATABASE_USERNAME} / {@code TEST_DATABASE_PASSWORD}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDoubles.class)
public abstract class ClientIntegrationTest {

    private static final String EXTERNAL_DB_URL = System.getenv("TEST_DATABASE_URL");

    private static final PostgreSQLContainer POSTGRES = EXTERNAL_DB_URL == null ? new PostgreSQLContainer("postgres:16-alpine") : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DATABASE_USERNAME", "postgres"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
        }
    }

    protected static final String PASSWORD = "Password123@";
    protected static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final AtomicInteger UNIQUE = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TestDoubles.RecordingOtpSender otpSender;

    @Autowired
    protected TestDoubles.RecordingPushSender pushSender;

    /** A new active account; {@code role} may be null (mobile user who hasn't picked a role yet) */
    protected User createUser(Role role, boolean verified) {
        User u = new User();
        u.setEmail("user" + UNIQUE.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setFullName("Người dùng " + UNIQUE.get());
        u.setRole(role);
        u.setVerified(verified);
        return users.save(u);
    }

    protected String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'" + email + "','password':'" + PASSWORD + "'}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.accessToken");
    }

    protected String token(User user) throws Exception {
        return login(user.getEmail());
    }

    protected String coupleToken() throws Exception {
        return token(createUser(Role.ROLE_COUPLE, true));
    }

    /** Creates a plan for the caller (wedding in ~6 months, 300M budget, 2 categories) and returns its id */
    protected String createPlan(String token) throws Exception {
        String body = mvc.perform(authed(post("/api/v1/client/plans"), token)
                        .content(planBody(today().plusMonths(6), 300_000_000L)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(body, "$.data.planId");
    }

    protected static String planBody(LocalDate weddingDate, long totalBudget) {
        return json("{'weddingDate':'" + weddingDate + "','engagementDate':'" + weddingDate.minusDays(5) + "','totalBudget':" + totalBudget
                + ",'categories':[{'name':'Nhà hàng & Tiệc','allocatedAmount':150000000},"
                + "{'name':'Chụp ảnh & Quay phim','allocatedAmount':30000000}]}");
    }

    protected static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    protected static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String token) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected static String json(String body) {
        return body.replace('\'', '"');
    }

    protected static String read(String body, String path) {
        return String.valueOf((Object) JsonPath.read(body, path));
    }

    protected static String body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Distinct valid Vietnamese mobile number per call */
    protected static String uniquePhone() {
        return "09" + String.format("%08d", 20_000_000 + UNIQUE.incrementAndGet());
    }

    protected static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
