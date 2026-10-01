package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.LeadStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** CRM customer / lead moving through the 5-stage pipeline (FN-ADM-CRM-01). */
@Getter
@Setter
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(name = "has_zalo", nullable = false)
    private boolean hasZalo;

    /** Package of interest (free text from the form) */
    private String interest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStage stage = LeadStage.NEW_LEAD;
}
