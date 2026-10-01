package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.AssetCategory;
import com.weddingevent.common.domain.enums.AssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Wardrobe item or camera equipment (feature spec §5.5). */
@Getter
@Setter
@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

    /** e.g. VAY-001, MAY-004 */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    private String size;

    /** Manually set status; rental dates override it (see AssetService#effectiveStatus). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.AVAILABLE;

    /** Days locked after return for cleaning/maintenance (FN-ADM-ASSET-01) */
    @Column(name = "maintenance_buffer_days", nullable = false)
    private int maintenanceBufferDays;
}
