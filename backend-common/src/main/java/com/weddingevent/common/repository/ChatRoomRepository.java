package com.weddingevent.common.repository;

import com.weddingevent.common.domain.ChatRoom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByUserAIdAndUserBId(UUID userAId, UUID userBId);

    /** Race-free "get or create": concurrent first messages between the same pair create one room */
    @Modifying
    @Query(value = """
            insert into chat_rooms (id, user_a_id, user_b_id, created_at, updated_at)
            values (gen_random_uuid(), :a, :b, now(), now())
            on conflict (user_a_id, user_b_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("a") UUID userAId, @Param("b") UUID userBId);
}