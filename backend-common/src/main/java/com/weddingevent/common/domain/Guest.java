package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.GuestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Wedding guest and RSVP state (FN-GUEST-01/02). */
@Getter
@Setter
@Entity
@Table(name = "guests")
public class Guest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wedding_plan_id", nullable = false)
    private WeddingPlan plan;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(name = "group_name")
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuestStatus status = GuestStatus.PENDING;

    private String notes;

    @Column(name = "table_number")
    private Integer tableNumber;
}
