package com.weddingevent.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Full-stack tests: real PostgreSQL + Flyway + demo seed data. One database and one Spring context are
 * shared by all test classes; tests use unique data/dates so they don't interfere.
 *
 * <p>By default PostgreSQL 16 runs in Docker via Testcontainers (CI). Without Docker, point the tests at
 * an empty database: {@code TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/wedding_test}
 * (+ {@code TEST_DATABASE_USERNAME} / {@code TEST_DATABASE_PASSWORD}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final String EXTERNAL_DB_URL = System.getenv("TEST_DATABASE_URL");

    private static final PostgreSQLContainer POSTGRES = EXTERNAL_DB_URL == null ? new PostgreSQLContainer("postgres:16-alpine") : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    /** Shared by the whole context (one Spring context for every test class) */
    protected static final ZaloStub ZALO = ZaloStub.start();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("app.zalo.oauth-url", ZALO::oauthUrl);
        registry.add("app.zalo.zns-url", ZALO::znsUrl);
        registry.add("app.public-base-url", () -> "https://studio.test");
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

    private static final AtomicInteger UNIQUE = new AtomicInteger();

    protected static final String ADMIN_EMAIL = "admin@lumiere.vn";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String STAFF_EMAIL = "staff@lumiere.vn";
    protected static final String STAFF_PASSWORD = "Staff@123";

    @Autowired
    protected MockMvc mvc;

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String staffToken() throws Exception {
        return login(STAFF_EMAIL, STAFF_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.accessToken");
    }

    /** Adds the bearer token and JSON content type */
    protected static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String token) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected static String json(String body) {
        return body.replace('\'', '"');
    }

    /** A distinct far-future date per call so tests never collide on schedules */
    protected static LocalDate uniqueFutureDate() {
        return LocalDate.now().plusYears(2).plusDays(UNIQUE.incrementAndGet() * 10L);
    }

    /** Distinct valid Vietnamese mobile number per call */
    protected static String uniquePhone() {
        return "09" + String.format("%08d", 10_000_000 + UNIQUE.incrementAndGet());
    }

    protected static String read(String body, String path) {
        Object value = JsonPath.read(body, path);
        return String.valueOf(value);
    }

    protected String body(MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(authed(req, token)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** CRM customer (lead) with the given phone; returns its id */
    protected String createLead(String admin, String phone) throws Exception {
        String created = mvc.perform(authed(post("/api/v1/admin/crm/customers"), admin)
                        .content(json("{'name':'Khách Hành Trình','phone':'%s','hasZalo':true}".formatted(phone))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(created, "$.data.id");
    }

    /** Contract on the first demo package; a lead with the same phone becomes its customer. Returns its id */
    protected String createContract(String admin, String phone, long total, long deposit) throws Exception {
        String pkgId = read(body(get("/api/v1/admin/packages"), admin), "$.data[0].id");
        String created = mvc.perform(authed(post("/api/v1/admin/contracts"), admin).content(json("""
                        {'customerName':'Phạm Thu Hà','phone':'%s','servicePackageId':'%s','totalAmount':%d,'depositAmount':%d}
                        """.formatted(phone, pkgId, total, deposit))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(created, "$.data.id");
    }

    /** Booking of a contract on a unique future date, no staff assigned. Returns its id */
    protected String createContractBooking(String admin, String contractId) throws Exception {
        String created = mvc.perform(authed(post("/api/v1/admin/bookings"), admin).content(json("""
                        {'clientName':'Phạm Thu Hà','type':'Chụp Pre-wedding','contractId':'%s','eventDate':'%s','eventTime':'09:00','staffIds':[]}
                        """.formatted(contractId, uniqueFutureDate()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(created, "$.data.id");
    }

    protected void moveBooking(String token, String bookingId, String status) throws Exception {
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + bookingId + "/status"), token)
                        .content(json("{'status':'%s'}".formatted(status))))
                .andExpect(status().isOk());
    }

    protected static String income(long amount, String contractId, LocalDate date) {
        return json("""
                {'type':'INCOME','amount':%d,'category':'Doanh thu HĐ','description':'Thanh toán','contractId':'%s','transactionDate':'%s'}
                """.formatted(amount, contractId, date));
    }

    /** A field of one contract from the list (null when the field is null) */
    protected Object contractField(String admin, String contractId, String field) throws Exception {
        return first(body(get("/api/v1/admin/contracts"), admin), "$.data[?(@.id=='%s')].%s".formatted(contractId, field));
    }

    protected Object leadStage(String admin, String leadId) throws Exception {
        return first(body(get("/api/v1/admin/crm/pipeline"), admin), "$.data[?(@.id=='%s')].stage".formatted(leadId));
    }

    /** First match of a filter path, which JsonPath always returns as a list */
    protected static Object first(String body, String path) {
        java.util.List<Object> matches = JsonPath.read(body, path);
        return matches.isEmpty() ? null : matches.get(0);
    }
}
