package com.weddingevent.client.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.common.domain.BudgetItem;
import com.weddingevent.common.domain.PlanTask;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.WeddingPlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Response shapes and formulas of FN-PLAN-02 (also returned by create, AI generate and join). */
public final class PlanDtos {

    private PlanDtos() {}

    public record PlanDto(
            String planId,
            LocalDate weddingDate,
            LocalDate engagementDate,
            long daysRemaining,
            long totalBudget,
            long totalSpent,
            double spentPercentage,
            TasksProgress tasksProgress,
            PartnerDto partner,
            List<BudgetItemDto> budgetItems,
            List<TaskDto> tasks) {

        /** {@code viewerId} decides who "the partner" is: the other member of the couple */
        public static PlanDto of(WeddingPlan p, UUID viewerId, LocalDate today) {
            long budget = p.getTotalBudget().longValueExact();
            long spent = p.getBudgetItems().stream().map(BudgetItem::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add).longValueExact();
            int total = p.getTasks().size();
            int completed = (int) p.getTasks().stream().filter(PlanTask::isCompleted).count();
            User other = p.getOwner().getId().equals(viewerId) ? p.getPartner() : p.getOwner();
            return new PlanDto(
                    p.getId().toString(),
                    p.getWeddingDate(),
                    p.getEngagementDate(),
                    Math.max(0, ChronoUnit.DAYS.between(today, p.getWeddingDate())),
                    budget,
                    spent,
                    percent(spent, budget),
                    new TasksProgress(total, completed, percent(completed, total)),
                    other == null ? null : new PartnerDto(other.getId().toString(), other.getFullName(), other.getAvatarUrl()),
                    p.getBudgetItems().stream().sorted(Comparator.comparingInt(BudgetItem::getPosition)).map(BudgetItemDto::of).toList(),
                    p.getTasks().stream()
                            .sorted(Comparator.comparing(PlanTask::getDueDate).thenComparingInt(PlanTask::getPosition))
                            .map(TaskDto::of).toList());
        }
    }

    public record TasksProgress(int totalTasks, int completedTasks, double progressPercentage) {}

    public record PartnerDto(String id, String name, String avatarUrl) {}

    public record BudgetItemDto(String id, String categoryName, long estimatedAmount, long actualAmount, String status, String notes) {
        static BudgetItemDto of(BudgetItem b) {
            return new BudgetItemDto(b.getId().toString(), b.getCategoryName(), b.getEstimatedAmount().longValueExact(),
                    b.getActualAmount().longValueExact(), b.getStatus().name(), b.getNotes());
        }
    }

    public record TaskDto(String id, String title, LocalDate dueDate, String monthGroup, @JsonProperty("isCompleted") boolean isCompleted) {
        static TaskDto of(PlanTask t) {
            return new TaskDto(t.getId().toString(), t.getTitle(), t.getDueDate(), t.getMonthGroup(), t.isCompleted());
        }
    }

    public record SyncCodeDto(String syncCode, Instant expiresAt) {}

    /** {@code part / whole * 100}, HALF_UP to 1 decimal; 0 when {@code whole} is 0 (spec FN-PLAN-02) */
    static double percent(long part, long whole) {
        if (whole == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Stored {@code wedding_plans.completed_percentage}: whole percent, HALF_UP */
    static int wholePercent(long part, long whole) {
        return whole == 0 ? 0 : BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 0, RoundingMode.HALF_UP).intValueExact();
    }
}
