package com.weddingevent.common.repository;

import com.weddingevent.common.domain.QuoteRequest;
import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** FN-VND-01..03. Every read is scoped to the couple who sent the request or the vendor who received it. */
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, UUID> {

    Optional<QuoteRequest> findFirstByCoupleIdAndServicePackageIdAndStatusIn(UUID coupleId, UUID packageId,
            Collection<QuoteRequestStatus> statuses);

    @EntityGraph(attributePaths = {"couple", "vendor"})
    Page<QuoteRequest> findAllByCoupleIdOrderByCreatedAtDesc(UUID coupleId, Pageable pageable);

    @EntityGraph(attributePaths = {"couple", "vendor"})
    Page<QuoteRequest> findAllByCoupleIdAndStatusOrderByCreatedAtDesc(UUID coupleId, QuoteRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"couple", "vendor"})
    Page<QuoteRequest> findAllByVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"couple", "vendor"})
    Page<QuoteRequest> findAllByVendorIdAndStatusOrderByCreatedAtDesc(UUID vendorId, QuoteRequestStatus status, Pageable pageable);

    long countByVendorIdAndStatus(UUID vendorId, QuoteRequestStatus status);

    /** FN-VND-05 dashboard: newest requests with this status */
    @EntityGraph(attributePaths = {"couple", "vendor"})
    List<QuoteRequest> findTop5ByVendorIdAndStatusOrderByCreatedAtDesc(UUID vendorId, QuoteRequestStatus status);

    Optional<QuoteRequest> findByIdAndCoupleId(UUID id, UUID coupleId);

    Optional<QuoteRequest> findByIdAndVendorId(UUID id, UUID vendorId);
}
