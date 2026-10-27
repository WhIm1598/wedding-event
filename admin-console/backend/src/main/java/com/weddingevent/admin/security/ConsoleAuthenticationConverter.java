package com.weddingevent.admin.security;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.StudioRepository;
import com.weddingevent.common.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Checks the account behind every access token (#42, FN-ADM-AUTH-04): a locked account stops working at once, and a
 * token whose role or staff link no longer matches the database is refused (401), so the app refreshes and gets the
 * current ones. {@link CurrentUser#from(Jwt)} can keep reading the claims.
 */
@Component
public class ConsoleAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Roles that work inside one studio (FN-ADM-USER-01 manages these) */
    public static final Set<Role> STUDIO_ROLES = Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF);
    /** Everyone who signs in to the web console, including the platform admin (ADR-0001) */
    public static final Set<Role> CONSOLE_ROLES = Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF, Role.ROLE_PLATFORM_ADMIN);
    /** The only authority of an account whose studio is suspended or past its subscription */
    public static final String STUDIO_SUSPENDED = "STUDIO_SUSPENDED";
    /** The only authority of an account that still has to change its temporary password (FN-ADM-AUTH-01 step 6) */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private final UserRepository users;
    private final StudioRepository studios;
    private final Clock clock;

    public ConsoleAuthenticationConverter(UserRepository users, StudioRepository studios, Clock clock) {
        this.users = users;
        this.studios = studios;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUser claims = CurrentUser.from(jwt);
        User user = users.findById(claims.id())
                .filter(User::isActive)
                .filter(u -> u.getRole() == claims.role() && isConsoleUser(u))
                .filter(u -> Objects.equals(staffId(u), claims.staffMemberId()))
                .filter(u -> Objects.equals(u.getStudioId(), claims.studioId()))
                .orElseThrow(() -> new InvalidBearerTokenException("Account locked or changed"));
        // A temporary password or a suspended studio grants no role, so only the routes SecurityConfig opens to any
        // signed-in user work; SecurityErrorHandler names the reason
        String authority = user.isMustChangePassword() ? PASSWORD_CHANGE_REQUIRED
                : !studioUsable(user) ? STUDIO_SUSPENDED
                : user.getRole().name();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(authority)), user.getId().toString());
    }

    /** Mobile accounts (couple/vendor, or no role yet) can't use the admin console. Set.of(...).contains(null) throws. */
    public static boolean isConsoleUser(User user) {
        return user.getRole() != null && CONSOLE_ROLES.contains(user.getRole());
    }

    /** The platform admin has no studio; a studio account needs an active, paid-up studio */
    public boolean studioUsable(User user) {
        return user.getStudioId() == null
                || studios.findById(user.getStudioId()).filter(s -> s.isUsable(LocalDate.now(clock))).isPresent();
    }

    private static UUID staffId(User user) {
        return user.getStaffMember() == null ? null : user.getStaffMember().getId();
    }
}
