package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

/**
 * A row that belongs to one studio (ADR-0001). Hibernate adds {@code studio_id = <current studio>} to every query and
 * fills it on insert, from the backend's {@link com.weddingevent.common.tenant.StudioTenantResolver}.
 * Only for tables the mobile app never writes; shared tables (users, service_packages, notifications) filter by hand.
 */
@Getter
@MappedSuperclass
public abstract class StudioOwnedEntity extends BaseEntity {

    @TenantId
    @Column(name = "studio_id", nullable = false, updatable = false)
    private UUID studioId;
}
