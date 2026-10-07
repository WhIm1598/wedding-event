package com.weddingevent.client.plan.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.weddingevent.client.plan.ai.AiPlannerClient.AiPlan;
import com.weddingevent.client.plan.ai.AiPlannerClient.AiPlanInput;
import com.weddingevent.common.exception.AppException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

/** FN-PLAN-03: the in-house AI planner contract and the built-in fallback */
class AiPlannerClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(int status, String responseBody, AtomicReference<String> captured) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/predict/wedding-plan", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void httpClientPostsTheRequestAndReadsTheStructuredPlan() throws IOException {
        AtomicReference<String> request = new AtomicReference<>();
        String base = startServer(200, """
                {"totalBudget":250000000,
                 "categories":[{"name":"Nhà hàng & Tiệc","amount":125000000,"notes":"50% tổng ngân sách"}],
                 "tasks":[{"title":"Khảo sát sảnh tiệc","monthGroup":"Trước 6 tháng","dueMonthOffset":6}]}
                """, request);

        AiPlan plan = new HttpAiPlannerClient(base + "/", Duration.ofSeconds(5)).generate(
                new AiPlanInput("Cưới thanh lịch", 250_000_000L, LocalDate.of(2027, 11, 15), 200, "ELEGANT"));

        assertThat(plan.totalBudget()).isEqualTo(250_000_000L);
        assertThat(plan.categories()).singleElement().satisfies(c -> {
            assertThat(c.name()).isEqualTo("Nhà hàng & Tiệc");
            assertThat(c.amount()).isEqualTo(125_000_000L);
            assertThat(c.notes()).isEqualTo("50% tổng ngân sách");
        });
        assertThat(plan.tasks()).singleElement().satisfies(t -> assertThat(t.dueMonthOffset()).isEqualTo(6));
        assertThat(request.get())
                .contains("\"prompt\":\"Cưới thanh lịch\"")
                .contains("\"weddingDate\":\"2027-11-15\"")
                .contains("\"guestCount\":200")
                .contains("\"style\":\"ELEGANT\"");
    }

    @Test
    void serviceErrorsBecome503() throws IOException {
        String base = startServer(500, "{}", new AtomicReference<>());
        assertThatThrownBy(() -> new HttpAiPlannerClient(base, Duration.ofSeconds(5)).generate(new AiPlanInput("x", null, null, null, null)))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getCode()).isEqualTo("AI_SERVICE_UNAVAILABLE");
                });
    }

    @Test
    void emptyPlansAreRejected() throws IOException {
        String base = startServer(200, "{\"totalBudget\":1,\"categories\":[],\"tasks\":[]}", new AtomicReference<>());
        assertThatThrownBy(() -> new HttpAiPlannerClient(base, Duration.ofSeconds(5)).generate(new AiPlanInput("x", null, null, null, null)))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("AI_SERVICE_UNAVAILABLE"));
    }

    @Test
    void unreachableServiceBecomes503() {
        assertThatThrownBy(() -> new HttpAiPlannerClient("http://localhost:1", Duration.ofSeconds(2)).generate(new AiPlanInput("x", null, null, null, null)))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("AI_SERVICE_UNAVAILABLE"));
    }

    @ParameterizedTest
    @CsvSource({"250000000", "10000000", "123456789", "99999999999"})
    void ruleBasedSplitAlwaysAddsUpToTheBudget(long budget) {
        AiPlan plan = new RuleBasedAiPlanner().generate(new AiPlanInput("x", budget, null, null, null));
        assertThat(plan.totalBudget()).isEqualTo(budget);
        assertThat(plan.categories()).hasSize(5);
        assertThat(plan.categories().stream().mapToLong(AiPlannerClient.Category::amount).sum()).isEqualTo(budget);
        assertThat(plan.tasks()).hasSize(5);
    }

    @Test
    void ruleBasedPlanMatchesTheSpecExampleAndDefaultsTheBudget() {
        AiPlan plan = new RuleBasedAiPlanner().generate(new AiPlanInput("x", null, null, null, null));
        assertThat(plan.totalBudget()).isEqualTo(250_000_000L);
        assertThat(plan.categories()).extracting(AiPlannerClient.Category::amount)
                .containsExactly(125_000_000L, 35_000_000L, 25_000_000L, 35_000_000L, 30_000_000L);
        assertThat(plan.tasks()).extracting(AiPlannerClient.Task::dueMonthOffset).containsExactly(6, 4, 2, 1, 0);
    }
}
