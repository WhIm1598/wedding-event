package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop30ByOrderByCreatedAtDesc();

    @Modifying
    @Query("update Notification n set n.read = true where n.read = false")
    int markAllRead();
}
