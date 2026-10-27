package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.NotificationKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * In-app notification. {@code recipientUserId == null}: the studio team's feed in the admin console ("Thông báo
 * thông minh"); otherwise one mobile user's inbox (FN-NOTI-01).
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    /** Studio feed owner (ADR-0001); null for a mobile user's inbox. Filtered by hand: the table is shared */
    @Column(name = "studio_id")
    private UUID studioId;

    /** Mobile icon hint; null for studio notifications */
    @Enumerated(EnumType.STRING)
    private NotificationKind kind;

    private String link;
}
