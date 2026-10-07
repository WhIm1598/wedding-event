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

/**
 * One table, two audiences: {@code recipientUserId IS NULL} is the studio team's feed (admin console);
 * otherwise a mobile user's inbox (FN-NOTI-01). Always filter by one of the two.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ---- Studio team (admin console) ----

    List<Notification> findTop30ByRecipientUserIdIsNullOrderByCreatedAtDesc();

    Optional<Notification> findByIdAndRecipientUserIdIsNull(UUID id);

    @Modifying
    @Query("update Notification n set n.read = true where n.read = false and n.recipientUserId is null")
    int markAllRead();

    // ---- Mobile user inbox ----

    Page<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
}
