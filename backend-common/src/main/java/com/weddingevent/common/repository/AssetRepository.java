package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Asset;
import com.weddingevent.common.domain.enums.AssetCategory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findAllByOrderByCodeAsc();

    List<Asset> findByCategoryOrderByCodeAsc(AssetCategory category);

    Optional<Asset> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /** Row lock so two concurrent rentals of the same item can't both pass the overlap check */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.id = :id")
    Optional<Asset> findByIdForUpdate(@Param("id") UUID id);
}
