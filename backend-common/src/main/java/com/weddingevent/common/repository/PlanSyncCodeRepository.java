package com.weddingevent.common.repository;

import com.weddingevent.common.domain.PlanSyncCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanSyncCodeRepository extends JpaRepository<PlanSyncCode, UUID> {

    /** Row-locked so two partners can't redeem the same code concurrently */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PlanSyncCode c where c.code = :code and c.used = false")
    Optional<PlanSyncCode> findUnusedForUpdate(@Param("code") String code);

    boolean existsByCodeAndUsedFalse(String code);

    /** A new code replaces the plan's previous unused ones */
    @Modifying
    @Query("delete from PlanSyncCode c where c.plan.id = :planId and c.used = false")
    int deleteUnusedByPlanId(@Param("planId") UUID planId);
}