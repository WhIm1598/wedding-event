package com.weddingevent.client.plan.ai;

import java.time.LocalDate;
import java.util.List;

/**
 * FN-PLAN-03: the in-house AI planner ({@code POST /api/v1/predict/wedding-plan} on ai-planner-service). Request and
 * response mirror the spec's structured JSON schema.
 */
public interface AiPlannerClient {

    record AiPlanInput(String prompt, Long totalBudget, LocalDate weddingDate, Integer guestCount, String style) {}

    record Category(String name, long amount, String notes) {}

    record Task(String title, String monthGroup, int dueMonthOffset) {}

    record AiPlan(Long totalBudget, List<Category> categories, List<Task> tasks) {}

    /** @throws com.weddingevent.common.exception.AppException 503 AI_SERVICE_UNAVAILABLE when no usable plan comes back */
    AiPlan generate(AiPlanInput input);
}
