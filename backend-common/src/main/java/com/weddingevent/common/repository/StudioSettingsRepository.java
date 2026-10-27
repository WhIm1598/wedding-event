package com.weddingevent.common.repository;

import com.weddingevent.common.domain.StudioSettings;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudioSettingsRepository extends JpaRepository<StudioSettings, UUID> {

    /** Serializes Zalo OA token refreshes: the refresh token is single-use, two concurrent refreshes would kill it */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudioSettings s where s.studioId = :id")
    Optional<StudioSettings> findByIdForUpdate(@Param("id") UUID id);
}
