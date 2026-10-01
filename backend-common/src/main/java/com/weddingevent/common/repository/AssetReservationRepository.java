package com.weddingevent.common.repository;

import com.weddingevent.common.domain.AssetReservation;
import com.weddingevent.common.domain.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetReservationRepository extends JpaRepository<AssetReservation, UUID> {

    /** Spec FN-ADM-ASSET-01: start <= lockEnd AND end(+buffer) >= lockStart */
    @Query("""
            select r from AssetReservation r
            where r.asset.id = :assetId and r.status = :status
              and r.startDate <= :lockEnd and r.lockEndDate >= :lockStart
            order by r.lockEndDate desc""")
    List<AssetReservation> findOverlapping(
            @Param("assetId") UUID assetId,
            @Param("lockStart") LocalDate lockStart,
            @Param("lockEnd") LocalDate lockEnd,
            @Param("status") ReservationStatus status);

    /** Reservations still relevant today or later (next booking, effective status, conflict banner) */
    @EntityGraph(attributePaths = "asset")
    List<AssetReservation> findByStatusAndLockEndDateGreaterThanEqualOrderByStartDateAsc(ReservationStatus status, LocalDate date);
}
