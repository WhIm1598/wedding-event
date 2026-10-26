package com.weddingevent.client.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.NotificationRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** FN-NOTI-01: a mobile user's inbox. Everything pushed is also stored here. */
@Service
public class ClientNotificationService {

    public record NotificationDto(String id, String title, String body, String kind, @JsonProperty("isRead") boolean isRead, Instant createdAt) {
        static NotificationDto of(Notification n) {
            NotificationKind kind = n.getKind() == null ? NotificationKind.REMINDER : n.getKind();
            return new NotificationDto(n.getId().toString(), n.getTitle(), n.getDescription(), kind.name().toLowerCase(Locale.ROOT),
                    n.isRead(), n.getCreatedAt());
        }
    }

    private final NotificationRepository notifications;
    private final PushSender push;

    public ClientNotificationService(NotificationRepository notifications, PushSender push) {
        this.notifications = notifications;
        this.push = push;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> inbox(UUID userId, int page, int size) {
        return PageResponse.of(notifications.findAllByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)),
                NotificationDto::of);
    }

    @Transactional
    public NotificationDto markRead(UUID userId, UUID id) {
        Notification n = notifications.findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("thông báo", id));
        n.setRead(true);
        return NotificationDto.of(n);
    }

    /** Stores the notification in the user's inbox and pushes it once the surrounding transaction commits. */
    @Transactional
    public void notify(UUID userId, NotificationKind kind, String title, String body) {
        Notification n = new Notification();
        n.setRecipientUserId(userId);
        n.setKind(kind);
        n.setTitle(title);
        n.setDescription(body.length() > 500 ? body.substring(0, 497) + "..." : body);
        notifications.save(n);
        afterCommit(() -> push.send(userId, new PushSender.PushMessage(title, n.getDescription(), Map.of(
                "type", "notification", "kind", kind.name().toLowerCase(Locale.ROOT), "notificationId", n.getId().toString()))));
    }

    /** Runs {@code action} after the current transaction commits (immediately when there is none) */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
