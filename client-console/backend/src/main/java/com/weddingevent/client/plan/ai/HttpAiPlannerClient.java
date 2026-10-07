package com.weddingevent.client.plan.ai;

import com.weddingevent.common.exception.AppException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the in-house AI planner service over its internal REST API. */
public class HttpAiPlannerClient implements AiPlannerClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAiPlannerClient.class);

    private final RestClient rest;

    public HttpAiPlannerClient(String baseUrl, Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        this.rest = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public AiPlan generate(AiPlanInput input) {
        AiPlan plan;
        try {
            plan = rest.post()
                    .uri("/api/v1/predict/wedding-plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(input)
                    .retrieve()
                    .body(AiPlan.class);
        } catch (RestClientException e) {
            log.warn("AI planner call failed: {}", e.getMessage());
            throw unavailable();
        }
        if (plan == null || plan.categories() == null || plan.categories().isEmpty() || plan.tasks() == null) {
            log.warn("AI planner returned no usable plan");
            throw unavailable();
        }
        return plan;
    }

    static AppException unavailable() {
        return new AppException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE",
                "Trợ lý AI đang bận, vui lòng thử lại sau hoặc tạo kế hoạch thủ công");
    }
}
