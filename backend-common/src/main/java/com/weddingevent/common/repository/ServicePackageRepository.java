package com.weddingevent.common.repository;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.enums.ServiceCategory;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    /** A studio's own packages (ADR-0001), most expensive first */
    List<ServicePackage> findAllByStudioIdOrderByPriceDesc(UUID studioId);

    Optional<ServicePackage> findByIdAndStudioId(UUID id, UUID studioId);

    /** A vendor's own packages, active or not (FN-VND-05), newest first */
    List<ServicePackage> findAllByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    Optional<ServicePackage> findByIdAndVendorId(UUID id, UUID vendorId);

    /**
     * Mobile catalog (FN-SVC-01): active packages of the given categories whose name or provider name (vendor
     * business name or studio name) matches {@code keyword} (a lower-case LIKE pattern, already escaped with
     * {@code \}). Packages of a suspended or expired studio are hidden (ADR-0001).
     */
    @Query(value = """
            select p from ServicePackage p
              left join VendorProfile v on v.id = p.vendorId
              left join Studio st on st.id = p.studioId
              left join StudioSettings s on s.studioId = p.studioId
            where p.active = true and p.category in :categories
              and (p.vendorId is not null
                   or (st.status = com.weddingevent.common.domain.enums.StudioStatus.ACTIVE
                       and (st.subscriptionExpiresAt is null or st.subscriptionExpiresAt >= :today)))
              and (lower(p.name) like :keyword escape '\\'
                   or lower(v.businessName) like :keyword escape '\\'
                   or lower(s.name) like :keyword escape '\\')
            order by p.createdAt desc, p.id
            """,
            countQuery = """
            select count(p) from ServicePackage p
              left join VendorProfile v on v.id = p.vendorId
              left join Studio st on st.id = p.studioId
              left join StudioSettings s on s.studioId = p.studioId
            where p.active = true and p.category in :categories
              and (p.vendorId is not null
                   or (st.status = com.weddingevent.common.domain.enums.StudioStatus.ACTIVE
                       and (st.subscriptionExpiresAt is null or st.subscriptionExpiresAt >= :today)))
              and (lower(p.name) like :keyword escape '\\'
                   or lower(v.businessName) like :keyword escape '\\'
                   or lower(s.name) like :keyword escape '\\')
            """)
    Page<ServicePackage> searchCatalog(@Param("categories") Collection<ServiceCategory> categories,
            @Param("keyword") String keyword, @Param("today") LocalDate today, Pageable pageable);
}
