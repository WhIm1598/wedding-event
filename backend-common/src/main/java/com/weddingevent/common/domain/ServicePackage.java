package com.weddingevent.common.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

    /** Null for the studio's own packages */
    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

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
