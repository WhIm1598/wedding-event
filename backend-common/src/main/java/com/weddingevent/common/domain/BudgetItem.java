package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.BudgetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** One budget category of a wedding plan (FN-PLAN-01/02). */
@Getter
@Setter
@Entity
@Table(name = "budget_items")
public class BudgetItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wedding_plan_id", nullable = false)
    private WeddingPlan plan;

    /** Display order as entered */
    @Column(nullable = false)
    private int position;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "estimated_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal estimatedAmount;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetStatus status = BudgetStatus.PLANNED;

    private String notes;
}
