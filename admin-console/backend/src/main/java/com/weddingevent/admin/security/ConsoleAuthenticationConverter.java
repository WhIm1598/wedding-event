package com.weddingevent.admin.security;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.UserRepository;
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

    public static final Set<Role> CONSOLE_ROLES = Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF);

    private final UserRepository users;

    public ConsoleAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        CurrentUser claims = CurrentUser.from(jwt);
        User user = users.findById(claims.id())
                .filter(User::isActive)
                .filter(u -> u.getRole() == claims.role() && isConsoleUser(u))
                .filter(u -> Objects.equals(staffId(u), claims.staffMemberId()))
                .orElseThrow(() -> new InvalidBearerTokenException("Account locked or changed"));
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(user.getRole().name())), user.getId().toString());
    }

    /** Mobile accounts (couple/vendor, or no role yet) can't use the admin console. Set.of(...).contains(null) throws. */
    public static boolean isConsoleUser(User user) {
        return user.getRole() != null && CONSOLE_ROLES.contains(user.getRole());
    }

    private static UUID staffId(User user) {
        return user.getStaffMember() == null ? null : user.getStaffMember().getId();
    }
}
