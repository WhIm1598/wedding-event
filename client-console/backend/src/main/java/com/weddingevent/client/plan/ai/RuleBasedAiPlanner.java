package com.weddingevent.client.plan.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in stand-in for the AI planner service (dev, tests, or when {@code app.ai-planner.base-url} is empty). It
 * reproduces the spec's example: a fixed percentage split of the budget (HALF_UP to whole VND, remainder on the last
 * category so the total always matches — spec §1.1) and the five standard milestones. It ignores the free-text prompt.
 */
public class RuleBasedAiPlanner implements AiPlannerClient {

    static final long DEFAULT_BUDGET = 250_000_000L;

    private record Share(String name, int percent, String notes) {}

    private static final List<Share> SHARES = List.of(
            new Share("Nhà hàng & Tiệc", 50, "50% tổng ngân sách cho tiệc cưới"),
            new Share("Chụp ảnh & Quay phim", 14, "Gói Pre-wedding & Phóng sự cưới"),
            new Share("Trang phục & Makeup", 10, "Váy cưới, vest, makeup ngày cưới"),
            new Share("Trang trí tiệc (Decor)", 14, "Hoa tươi backdrop & bàn gallery"),
            new Share("Thiệp cưới & Dự phòng", 12, "Thiệp cưới và chi phí phát sinh"));

    private static final List<Task> TASKS = List.of(
            new Task("Khảo sát và đặt cọc sảnh tiệc", "Trước 6 tháng", 6),
            new Task("Chọn studio và chụp ảnh Pre-wedding", "Trước 4 tháng", 4),
            new Task("Thử váy cưới và chọn vest chú rể", "Trước 2 tháng", 2),
            new Task("Chốt danh sách khách mời và gửi thiệp", "Trước 1 tháng", 1),
            new Task("Tổng duyệt kịch bản tiệc và xác nhận nhà xe", "Trước 1 tuần", 0));

    @Override
    public AiPlan generate(AiPlanInput input) {
        long budget = input.totalBudget() == null ? DEFAULT_BUDGET : input.totalBudget();
        List<Category> categories = new ArrayList<>();
        long allocated = 0;
        for (int i = 0; i < SHARES.size(); i++) {
            Share share = SHARES.get(i);
            long amount = i == SHARES.size() - 1
                    ? budget - allocated
                    : BigDecimal.valueOf(budget).multiply(BigDecimal.valueOf(share.percent()))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact();
            allocated += amount;
            categories.add(new Category(share.name(), amount, share.notes()));
        }
        return new AiPlan(budget, categories, TASKS);
    }
}
