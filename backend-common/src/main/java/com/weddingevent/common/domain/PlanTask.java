package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Checklist item of a wedding plan, grouped by milestone ("Trước 6 tháng") — FN-PLAN-01/05. */
@Getter
@Setter
@Entity
@Table(name = "plan_tasks")
public class PlanTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wedding_plan_id", nullable = false)
    private WeddingPlan plan;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "month_group", nullable = false)
    private String monthGroup;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;
}
