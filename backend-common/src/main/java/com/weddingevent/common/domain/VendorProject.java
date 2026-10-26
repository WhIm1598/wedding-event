package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.VendorProjectStatus;
import com.weddingevent.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** An accepted Quote Request: work a vendor will do for a couple on a date (GLOSSARY "Vendor Project", FN-VND-03/04). */
@Getter
@Setter
@Entity
@Table(name = "vendor_projects")
public class VendorProject extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_request_id", nullable = false, unique = true)
    private QuoteRequest quoteRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_profile_id", nullable = false)
    private VendorProfile vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_user_id", nullable = false)
    private User couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_package_id", nullable = false)
    private ServicePackage servicePackage;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    /** The couple's name when the quote was accepted */
    @Column(name = "couple_name", nullable = false)
    private String coupleName;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    private String venue;

    @Column(name = "agreed_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal agreedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorProjectStatus status = VendorProjectStatus.UPCOMING;

    @Column(name = "cancel_requested_at")
    private Instant cancelRequestedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Version
    private long version;

    /**
     * Vendor moves the project along §1.6 ({@code UPCOMING → IN_PROGRESS → COMPLETED}, {@code UPCOMING → COMPLETED},
     * open → CANCELLED). Moving the work on while the couple asked to cancel rejects that request.
     *
     * @return true when a pending cancellation request was rejected by this move
     */
    public boolean moveTo(VendorProjectStatus next) {
        boolean allowed = switch (status) {
            case UPCOMING -> next != VendorProjectStatus.UPCOMING;
            case IN_PROGRESS -> next == VendorProjectStatus.COMPLETED || next == VendorProjectStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw refused("Không thể chuyển dự án từ " + status + " sang " + next);
        }
        boolean rejected = next != VendorProjectStatus.CANCELLED && hasPendingCancelRequest();
        if (rejected) {
            clearCancelRequest();
        }
        status = next;
        return rejected;
    }

    /** The couple asks to cancel; the status stays until the vendor approves or rejects */
    public void requestCancel(String reason, Instant at) {
        if (!isOpen() || hasPendingCancelRequest()) {
            throw refused("Dự án đã kết thúc hoặc đang chờ Vendor duyệt yêu cầu hủy");
        }
        cancelRequestedAt = at;
        cancelReason = reason;
    }

    public void approveCancel() {
        requirePendingCancel();
        status = VendorProjectStatus.CANCELLED; // the reason stays on record
    }

    public void rejectCancel() {
        requirePendingCancel();
        clearCancelRequest();
    }

    public boolean hasPendingCancelRequest() {
        return cancelRequestedAt != null && isOpen();
    }

    private boolean isOpen() {
        return status == VendorProjectStatus.UPCOMING || status == VendorProjectStatus.IN_PROGRESS;
    }

    private void requirePendingCancel() {
        if (!hasPendingCancelRequest()) {
            throw refused("Dự án không có yêu cầu hủy đang chờ duyệt");
        }
    }

    private void clearCancelRequest() {
        cancelRequestedAt = null;
        cancelReason = null;
    }

    private static AppException refused(String message) {
        return AppException.conflict("INVALID_STATUS_TRANSITION", message);
    }

    /** The project an accepted quote request turns into: the quoted price is the agreed price */
    public static VendorProject from(QuoteRequest q, LocalDate eventDate, String venue) {
        VendorProject p = new VendorProject();
        p.setQuoteRequest(q);
        p.setVendor(q.getVendor());
        p.setCouple(q.getCouple());
        p.setServicePackage(q.getServicePackage());
        p.setPackageName(q.getPackageName());
        p.setCoupleName(q.getCouple().getFullName());
        p.setEventDate(eventDate);
        p.setVenue(venue);
        p.setAgreedPrice(q.getQuotedPrice());
        return p;
    }
}
