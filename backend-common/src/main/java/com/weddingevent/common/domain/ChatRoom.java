package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Conversation between two users (FN-MSG-01). The pair is stored ordered: {@code userAId < userBId}. */
@Getter
@Setter
@Entity
@Table(name = "chat_rooms")
public class ChatRoom extends BaseEntity {

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public boolean hasParticipant(UUID userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }
}
