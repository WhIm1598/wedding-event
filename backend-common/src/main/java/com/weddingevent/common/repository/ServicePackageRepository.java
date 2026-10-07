package com.weddingevent.common.repository;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.enums.ServiceCategory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    /** Studio-owned packages (vendor_id IS NULL), most expensive first */
    List<ServicePackage> findAllByVendorIdIsNullOrderByPriceDesc();

    /**
     * Mobile catalog (FN-SVC-01): active packages of the given categories whose name or provider name matches
     * {@code keyword} (a lower-case LIKE pattern, already escaped with {@code \}). Studio packages have no vendor
     * row, so the caller decides whether the studio's name matches ({@code studioMatches}).
     */
    @Query(value = """
            select p from ServicePackage p left join VendorProfile v on v.id = p.vendorId
            where p.active = true and p.category in :categories
              and (lower(p.name) like :keyword escape '\\'
                   or lower(v.businessName) like :keyword escape '\\'
                   or (p.vendorId is null and :studioMatches = true))
            order by p.createdAt desc, p.id
            """,
            countQuery = """
            select count(p) from ServicePackage p left join VendorProfile v on v.id = p.vendorId
            where p.active = true and p.category in :categories
              and (lower(p.name) like :keyword escape '\\'
                   or lower(v.businessName) like :keyword escape '\\'
                   or (p.vendorId is null and :studioMatches = true))
            """)
    Page<ServicePackage> searchCatalog(@Param("categories") Collection<ServiceCategory> categories,
            @Param("keyword") String keyword, @Param("studioMatches") boolean studioMatches, Pageable pageable);
}
