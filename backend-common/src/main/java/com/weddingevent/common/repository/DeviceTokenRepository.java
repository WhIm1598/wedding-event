package com.weddingevent.common.repository;

import com.weddingevent.common.domain.DeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** FN-NOTI-02 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    /** Oldest registration first, so the caller can drop the stalest device above the limit */
    List<DeviceToken> findAllByUserIdOrderByUpdatedAtAsc(UUID userId);

    @Modifying
    @Transactional
    @Query("delete from DeviceToken d where d.token = :token and d.user.id = :userId")
    int deleteOwn(@Param("userId") UUID userId, @Param("token") String token);

    /** FCM said the token is dead (uninstalled app, expired token) */
    @Modifying
    @Transactional
    @Query("delete from DeviceToken d where d.token = :token")
    int deleteByTokenValue(@Param("token") String token);
}
