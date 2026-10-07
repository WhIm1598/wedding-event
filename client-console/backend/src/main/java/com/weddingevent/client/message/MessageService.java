package com.weddingevent.client.message;

import com.weddingevent.client.notification.ClientNotificationService;
import com.weddingevent.common.domain.ChatMessage;
import com.weddingevent.common.domain.ChatRoom;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.ChatMessageRepository;
import com.weddingevent.common.repository.ChatRoomRepository;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-MSG-01: messages between a couple and a vendor. One room per pair; every message is pushed to
 * {@code /topic/messages/{roomId}} after commit, and a recipient without an open STOMP session also gets an inbox
 * notification + push.
 */
@Service
public class MessageService {

    public static final String ROOM_TOPIC = "/topic/messages/";

    public record SendMessageRequest(
            @NotNull(message = "Thiếu người nhận") UUID recipientId,
            @NotBlank(message = "Nội dung tin nhắn không được để trống") @Size(max = 2000, message = "Tin nhắn tối đa 2000 ký tự") String content,
            @Size(max = 500, message = "Đường dẫn tệp đính kèm tối đa 500 ký tự") String attachmentUrl) {}

    public record MessageDto(String id, String roomId, String senderId, String recipientId, String content, String attachmentUrl, Instant createdAt) {}

    private final UserRepository users;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ClientNotificationService notifications;
    private final PresenceTracker presence;
    private final SimpMessageSendingOperations broker;
    private final Clock clock;

    public MessageService(UserRepository users, ChatRoomRepository rooms, ChatMessageRepository messages,
            ClientNotificationService notifications, PresenceTracker presence, SimpMessageSendingOperations broker, Clock clock) {
        this.users = users;
        this.rooms = rooms;
        this.messages = messages;
        this.notifications = notifications;
        this.presence = presence;
        this.broker = broker;
        this.clock = clock;
    }

    @Transactional
    public MessageDto send(UUID senderId, SendMessageRequest req) {
        User sender = users.findById(senderId).orElseThrow(MessageService::invalidRecipient);
        User recipient = users.findById(req.recipientId()).filter(User::isActive).orElseThrow(MessageService::invalidRecipient);
        if (!areCounterparts(sender.getRole(), recipient.getRole())) {
            throw invalidRecipient();
        }

        // Ordered like PostgreSQL orders UUIDs (unsigned bytes = lower-case hex text), matching CHECK (user_a_id < user_b_id)
        boolean senderFirst = sender.getId().toString().compareTo(recipient.getId().toString()) < 0;
        UUID a = senderFirst ? sender.getId() : recipient.getId();
        UUID b = senderFirst ? recipient.getId() : sender.getId();
        rooms.insertIfAbsent(a, b);
        ChatRoom room = rooms.findByUserAIdAndUserBId(a, b).orElseThrow();

        ChatMessage m = new ChatMessage();
        m.setRoomId(room.getId());
        m.setSenderId(sender.getId());
        m.setContent(req.content().trim());
        m.setAttachmentUrl(req.attachmentUrl() == null || req.attachmentUrl().isBlank() ? null : req.attachmentUrl().trim());
        messages.saveAndFlush(m);
        room.setLastMessageAt(clock.instant());

        MessageDto dto = new MessageDto(m.getId().toString(), room.getId().toString(), sender.getId().toString(),
                recipient.getId().toString(), m.getContent(), m.getAttachmentUrl(), m.getCreatedAt());
        if (!presence.isOnline(recipient.getId())) {
            notifications.notify(recipient.getId(), NotificationKind.MESSAGE, sender.getFullName(), m.getContent());
        }
        ClientNotificationService.afterCommit(() -> broker.convertAndSend(ROOM_TOPIC + room.getId(), dto));
        return dto;
    }

    /** Chat is between a couple and a vendor (either direction) */
    static boolean areCounterparts(Role sender, Role recipient) {
        return (sender == Role.ROLE_COUPLE && recipient == Role.ROLE_VENDOR) || (sender == Role.ROLE_VENDOR && recipient == Role.ROLE_COUPLE);
    }

    private static AppException invalidRecipient() {
        return AppException.badRequest("INVALID_RECIPIENT", "Không thể gửi tin nhắn tới người nhận này");
    }
}
