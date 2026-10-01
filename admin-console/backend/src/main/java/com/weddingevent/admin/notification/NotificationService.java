package com.weddingevent.admin.notification;

import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    public record NotificationDto(String id, String title, String description, Instant createdAt, boolean read) {
        static NotificationDto of(Notification n) {
            return new NotificationDto(n.getId().toString(), n.getTitle(), n.getDescription(), n.getCreatedAt(), n.isRead());
        }
    }

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> latest() {
        return notifications.findTop30ByOrderByCreatedAtDesc().stream().map(NotificationDto::of).toList();
    }

    /** Called by other services when something needs the team's attention */
    @Transactional
    public void notify(String title, String description) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setDescription(description);
        notifications.save(n);
    }

    @Transactional
    public void markRead(UUID id) {
        notifications.findById(id).orElseThrow(() -> new ResourceNotFoundException("thông báo", id)).setRead(true);
    }

    @Transactional
    public void markAllRead() {
        notifications.markAllRead();
    }
}
