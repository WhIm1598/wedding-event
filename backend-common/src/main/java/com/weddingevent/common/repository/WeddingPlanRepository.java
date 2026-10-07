package com.weddingevent.common.repository;

import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.PlanStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeddingPlanRepository extends JpaRepository<WeddingPlan, UUID> {

    /** The PLANNING plan the user owns or has joined as partner (FN-PLAN-02) */
    @Query("""
            select p from WeddingPlan p left join fetch p.partner join fetch p.owner
            where p.status = com.weddingevent.common.domain.enums.PlanStatus.PLANNING
              and (p.owner.id = :userId or p.partner.id = :userId)
            """)
    Optional<WeddingPlan> findActiveFor(@Param("userId") UUID userId);

    @Query("""
            select count(p) > 0 from WeddingPlan p
            where p.status = com.weddingevent.common.domain.enums.PlanStatus.PLANNING
              and (p.owner.id = :userId or p.partner.id = :userId)
            """)
    boolean existsActiveFor(@Param("userId") UUID userId);

    Optional<WeddingPlan> findByOwnerIdAndStatus(UUID ownerId, PlanStatus status);
}