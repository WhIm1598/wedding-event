package com.weddingevent.common.tenant;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The studio whose rows the current thread works on (ADR-0001). A backend's default comes from the request (the
 * admin console's JWT); code without a request, or that must cross studios, sets it explicitly here. The value is
 * read when a Hibernate session opens, so set it <em>outside</em> the transaction.
 */
public final class StudioTenant {

    /** Sees and may write every studio's rows: mobile backend, platform admin, public link lookup, jobs */
    public static final UUID ROOT = new UUID(0, 0);
    /** No studio: queries match nothing and inserts fail, the safe default for an anonymous request */
    public static final UUID NONE = new UUID(0, 1);

    private static final ThreadLocal<UUID> OVERRIDE = new ThreadLocal<>();

    private StudioTenant() {}

    /** The explicitly set studio, or null to use the backend's default */
    public static UUID override() {
        return OVERRIDE.get();
    }

    public static <T> T callAs(UUID studioId, Supplier<T> work) {
        UUID previous = OVERRIDE.get();
        OVERRIDE.set(studioId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(previous);
            }
        }
    }

    public static void runAs(UUID studioId, Runnable work) {
        callAs(studioId, () -> {
            work.run();
            return null;
        });
    }

    public static <T> T callAsRoot(Supplier<T> work) {
        return callAs(ROOT, work);
    }
}
