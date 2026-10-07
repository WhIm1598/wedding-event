package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.PlanStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * A couple's wedding plan (FN-PLAN-01..05). The owner creates it; a partner joins through a sync code (FN-PLAN-04)
 * and then has the same rights. {@code DRAFT} rows have no date/budget yet (created at role selection).
 */
@Getter
@Setter
@Entity
@Table(name = "wedding_plans")
public class WeddingPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private User partner;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "engagement_date")
    private LocalDate engagementDate;

    @Column(name = "total_budget", precision = 15, scale = 0)
    private BigDecimal totalBudget;

    /** Stored for list/push use; the API recomputes it from the tasks (FN-PLAN-05) */
    @Column(name = "completed_percentage", nullable = false)
    private int completedPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.DRAFT;

    @Version
    private long version;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<BudgetItem> budgetItems = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dueDate, position")
    private List<PlanTask> tasks = new ArrayList<>();
}
