package com.weddingevent.common.tenant;

import java.util.UUID;
import java.util.function.Supplier;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/** Hibernate's view of {@link StudioTenant}: the explicit studio, else the backend's default, else {@link StudioTenant#NONE}. */
public class StudioTenantResolver implements CurrentTenantIdentifierResolver<UUID> {

    private final Supplier<UUID> fallback;

    public StudioTenantResolver(Supplier<UUID> fallback) {
        this.fallback = fallback;
    }

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID explicit = StudioTenant.override();
        if (explicit != null) {
            return explicit;
        }
        UUID studio = fallback.get();
        return studio == null ? StudioTenant.NONE : studio;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return StudioTenant.ROOT.equals(tenantId);
    }
}
