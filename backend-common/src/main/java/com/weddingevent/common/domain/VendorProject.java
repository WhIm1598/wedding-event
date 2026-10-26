package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.VendorProjectStatus;
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
