package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.ServiceCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Studio or vendor service package (feature spec §6.2 ServicePackage). */
@Getter
@Setter
@Entity
@Table(name = "service_packages")
public class ServicePackage extends BaseEntity {

    /** {@link VendorProfile} id; null for the studio's own packages */
    @Column(name = "vendor_id")
    private UUID vendorId;

    /** Owning studio (ADR-0001) for a studio package; null for a vendor's. Exactly one of the two is set */
    @Column(name = "studio_id")
    private UUID studioId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    /** Mobile catalog category (FN-SVC-01); the studio's own packages are photography */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category = ServiceCategory.PHOTO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    /** Price after the vendor's discount, below {@link #price}; null = no discount (FN-VND-05) */
    @Column(name = "discount_price", precision = 15, scale = 0)
    private BigDecimal discountPrice;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_package_features", joinColumns = @JoinColumn(name = "package_id"))
    @OrderColumn(name = "position")
    @Column(name = "feature", nullable = false)
    private List<String> features = new ArrayList<>();

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
