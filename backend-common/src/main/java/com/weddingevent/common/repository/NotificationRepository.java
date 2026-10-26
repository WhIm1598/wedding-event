package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * One table, two audiences: {@code studioId} set is that studio team's feed (admin console, ADR-0001);
 * {@code recipientUserId} set is a mobile user's inbox (FN-NOTI-01). Always filter by one of the two.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ---- Studio team (admin console) ----

    List<Notification> findTop30ByStudioIdOrderByCreatedAtDesc(UUID studioId);

    Optional<Notification> findByIdAndStudioId(UUID id, UUID studioId);

    @Modifying
    @Query("update Notification n set n.read = true where n.read = false and n.studioId = :studioId")
    int markAllRead(@Param("studioId") UUID studioId);

    // ---- Mobile user inbox ----

    Page<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
}
