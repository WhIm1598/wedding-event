package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import com.weddingevent.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** A couple asks a vendor to quote one of its service packages (GLOSSARY "Quote Request", FN-VND-01..03). */
@Getter
@Setter
@Entity
@Table(name = "quote_requests")
public class QuoteRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_user_id", nullable = false)
    private User couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_profile_id", nullable = false)
    private VendorProfile vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_package_id", nullable = false)
    private ServicePackage servicePackage;

    /** Package name when the request was sent; renaming the package later doesn't change it */
    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "event_date")
    private LocalDate eventDate;

    private String venue;

    @Column(name = "guest_count")
    private Integer guestCount;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteRequestStatus status = QuoteRequestStatus.PENDING;

    @Column(name = "quoted_price", precision = 15, scale = 0)
    private BigDecimal quotedPrice;

    @Column(name = "vendor_note")
    private String vendorNote;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Version
    private long version;

    /** Vendor quotes; quoting again replaces the price and the note (PENDING/QUOTED → QUOTED) */
    public void quote(BigDecimal price, String note, Instant at) {
        requireOpen();
        quotedPrice = price;
        vendorNote = note;
        respondedAt = at;
        status = QuoteRequestStatus.QUOTED;
    }

    /** Vendor declines (PENDING/QUOTED → DECLINED) */
    public void decline(String note, Instant at) {
        requireOpen();
        vendorNote = note;
        respondedAt = at;
        status = QuoteRequestStatus.DECLINED;
    }

    /** Couple withdraws the request (PENDING/QUOTED → CANCELLED) */
    public void cancel() {
        requireOpen();
        status = QuoteRequestStatus.CANCELLED;
    }

    /** Couple accepts the quote (QUOTED → ACCEPTED); the caller then creates the Vendor Project */
    public void accept() {
        if (status != QuoteRequestStatus.QUOTED) {
            throw refused();
        }
        status = QuoteRequestStatus.ACCEPTED;
    }

    private void requireOpen() {
        if (!status.isOpen()) {
            throw refused();
        }
    }

    private AppException refused() {
        return AppException.conflict("INVALID_STATUS_TRANSITION", "Yêu cầu báo giá đang ở trạng thái " + status + ", không thể thực hiện thao tác này");
    }
}
