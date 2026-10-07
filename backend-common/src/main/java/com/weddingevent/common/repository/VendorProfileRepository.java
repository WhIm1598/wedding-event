package com.weddingevent.common.repository;

import com.weddingevent.common.domain.VendorProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {

    Optional<VendorProfile> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<VendorProfile> findAllByIdIn(Collection<UUID> ids);
}