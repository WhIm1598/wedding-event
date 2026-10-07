package com.weddingevent.client.plan;

import java.time.LocalDate;
import java.util.List;

/** FN-PLAN-01 step 4: the standard checklist created with every manual plan. */
public final class DefaultChecklist {

    private DefaultChecklist() {}

    public record Item(String title, String monthGroup, int monthsBefore) {
        public LocalDate dueDate(LocalDate weddingDate) {
            return weddingDate.minusMonths(monthsBefore);
        }
    }

    public static final List<Item> ITEMS = List.of(
            new Item("Khảo sát địa điểm", "Trước 6 tháng", 6),
            new Item("Chọn concept", "Trước 6 tháng", 6),
            new Item("Chụp ảnh Pre-wedding", "Trước 3 tháng", 3),
            new Item("Đặt váy cưới", "Trước 3 tháng", 3),
            new Item("Chốt danh sách khách mời", "Trước 1 tháng", 1),
            new Item("In thiệp cưới", "Trước 1 tháng", 1));
}
