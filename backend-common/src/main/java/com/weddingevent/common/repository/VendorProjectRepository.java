package com.weddingevent.common.repository;

import com.weddingevent.common.domain.VendorProject;
import com.weddingevent.common.domain.enums.VendorProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** FN-VND-03..05. Every read is scoped to the project's vendor or couple. */
public interface VendorProjectRepository extends JpaRepository<VendorProject, UUID> {

    Page<VendorProject> findAllByVendorIdOrderByEventDateAsc(UUID vendorId, Pageable pageable);

    Page<VendorProject> findAllByVendorIdAndStatusOrderByEventDateAsc(UUID vendorId, VendorProjectStatus status, Pageable pageable);

    /** Finished projects: most recent first */
    Page<VendorProject> findAllByVendorIdAndStatusOrderByEventDateDesc(UUID vendorId, VendorProjectStatus status, Pageable pageable);

    Page<VendorProject> findAllByCoupleIdOrderByEventDateAsc(UUID coupleId, Pageable pageable);

    Optional<VendorProject> findByIdAndVendorId(UUID id, UUID vendorId);

    Optional<VendorProject> findByIdAndCoupleId(UUID id, UUID coupleId);

    /** FN-VND-05 dashboard: the next projects with this status, earliest first */
    List<VendorProject> findTop5ByVendorIdAndStatusOrderByEventDateAsc(UUID vendorId, VendorProjectStatus status);

    /** FN-VND-05 monthlyRevenue: agreed prices of COMPLETED projects with an event date in [from, to] */
    @Query("""
            select coalesce(sum(p.agreedPrice), 0) from VendorProject p
            where p.vendor.id = :vendorId and p.status = com.weddingevent.common.domain.enums.VendorProjectStatus.COMPLETED
              and p.eventDate between :from and :to
            """)
    BigDecimal completedRevenue(@Param("vendorId") UUID vendorId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** FN-VND-05 projectCount per package: projects not CANCELLED */
    @Query("""
            select p.servicePackage.id as packageId, count(p) as projects from VendorProject p
            where p.vendor.id = :vendorId and p.status <> com.weddingevent.common.domain.enums.VendorProjectStatus.CANCELLED
            group by p.servicePackage.id
            """)
    List<PackageProjectCount> countByPackage(@Param("vendorId") UUID vendorId);

    interface PackageProjectCount {
        UUID getPackageId();

        long getProjects();
    }
}
