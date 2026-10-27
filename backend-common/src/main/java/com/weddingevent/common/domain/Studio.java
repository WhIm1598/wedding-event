package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.StudioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** A customer studio of the platform (ADR-0001). Its profile (name, address, Zalo OA) lives in {@link StudioSettings}. */
@Getter
@Setter
@Entity
@Table(name = "studios")
public class Studio extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudioStatus status = StudioStatus.ACTIVE;

    /** Last day of the paid subscription; null = no end date */
    @Column(name = "subscription_expires_at")
    private LocalDate subscriptionExpiresAt;

    /** Can its people sign in today? */
    public boolean isUsable(LocalDate today) {
        return status == StudioStatus.ACTIVE && (subscriptionExpiresAt == null || !subscriptionExpiresAt.isBefore(today));
    }
}
