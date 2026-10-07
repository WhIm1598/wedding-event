package com.weddingevent.common.repository;

import com.weddingevent.common.domain.ChatMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    long countByRoomId(UUID roomId);
}