package com.weddingevent.admin.security;

import com.weddingevent.common.domain.enums.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated caller, read from the access token. */
public record CurrentUser(UUID id, String email, Role role, UUID staffMemberId) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(TokenService.CLAIM_ROLES);
        String staffId = jwt.getClaimAsString(TokenService.CLAIM_STAFF_ID);
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString(TokenService.CLAIM_EMAIL),
                Role.valueOf(roles.getFirst()),
                staffId == null ? null : UUID.fromString(staffId));
    }

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }
}
