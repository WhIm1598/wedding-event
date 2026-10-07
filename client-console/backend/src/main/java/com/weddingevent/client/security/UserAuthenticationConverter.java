package com.weddingevent.client.security;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Turns a valid access token into an authentication whose authority is the user's <b>current</b> role, loaded from
 * the database: the role changes after sign-in (FN-AUTH-03) and a locked account must stop working immediately.
 * Only mobile accounts are accepted — studio accounts (admin/staff) use the admin console.
 */
@Component
public class UserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final Set<Role> MOBILE_ROLES = Set.of(Role.ROLE_COUPLE, Role.ROLE_VENDOR);

    private final UserRepository users;

    public UserAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = activeMobileUser(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new InvalidBearerTokenException("Account is locked or unknown"));
        return new JwtAuthenticationToken(jwt, authorities(user), user.getId().toString());
    }

    public Optional<User> activeMobileUser(UUID userId) {
        return users.findById(userId).filter(User::isActive).filter(UserAuthenticationConverter::isMobileUser);
    }

    /** No role yet (null) counts as a mobile account */
    public static boolean isMobileUser(User user) {
        return user.getRole() == null || MOBILE_ROLES.contains(user.getRole());
    }

    public static List<GrantedAuthority> authorities(User user) {
        return user.getRole() == null ? List.of() : List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }
}
