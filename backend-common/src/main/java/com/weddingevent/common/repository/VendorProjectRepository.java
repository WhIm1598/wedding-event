package com.weddingevent.common.repository;

import com.weddingevent.common.domain.VendorProject;
import com.weddingevent.common.domain.enums.VendorProjectStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** FN-VND-03..05. Every read is scoped to the project's vendor or couple. */
public interface VendorProjectRepository extends JpaRepository<VendorProject, UUID> {

    Page<VendorProject> findAllByVendorIdOrderByEventDateAsc(UUID vendorId, Pageable pageable);

    Page<VendorProject> findAllByVendorIdAndStatusOrderByEventDateAsc(UUID vendorId, VendorProjectStatus status, Pageable pageable);

    /** Finished projects: most recent first */
    Page<VendorProject> findAllByVendorIdAndStatusOrderByEventDateDesc(UUID vendorId, VendorProjectStatus status, Pageable pageable);

    Page<VendorProject> findAllByCoupleIdOrderByEventDateAsc(UUID coupleId, Pageable pageable);

    Optional<VendorProject> findByIdAndVendorId(UUID id, UUID vendorId);

    Optional<VendorProject> findByIdAndCoupleId(UUID id, UUID coupleId);
}
