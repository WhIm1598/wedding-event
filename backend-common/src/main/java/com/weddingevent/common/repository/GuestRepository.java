package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Guest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {

    /** Newest first; guest lists are small, so filtering and stats happen in memory (FN-GUEST-01) */
    List<Guest> findAllByPlanIdOrderByCreatedAtDesc(UUID planId);
}