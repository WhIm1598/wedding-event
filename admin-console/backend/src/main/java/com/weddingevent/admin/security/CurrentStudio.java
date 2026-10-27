package com.weddingevent.admin.security;

import com.weddingevent.common.tenant.StudioTenant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The studio a request works for (ADR-0001): an explicit {@link StudioTenant} studio, else the access token's claim. */
public final class CurrentStudio {

    private CurrentStudio() {}

    /** The {@code studioId} claim of the signed-in console user; null for anonymous and platform requests */
    public static UUID fromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String studio = jwt.getToken().getClaimAsString(TokenService.CLAIM_STUDIO_ID);
            return studio == null ? null : UUID.fromString(studio);
        }
        return null;
    }

    /** For writes and lookups of shared tables (settings, packages, notifications, users) */
    public static UUID require() {
        UUID explicit = StudioTenant.override();
        if (explicit != null && !StudioTenant.ROOT.equals(explicit) && !StudioTenant.NONE.equals(explicit)) {
            return explicit;
        }
        UUID studio = fromToken();
        if (studio == null) {
            throw new IllegalStateException("No studio for this request");
        }
        return studio;
    }
}
