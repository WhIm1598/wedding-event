package com.weddingevent.admin.auth;

import com.weddingevent.admin.auth.AuthDtos.AuthUserDto;
import com.weddingevent.admin.auth.AuthDtos.LoginResponse;
import com.weddingevent.admin.security.TokenService;
import com.weddingevent.admin.security.TokenService.TokenPair;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<Role> CONSOLE_ROLES = Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    /** Compared against when the email is unknown, so response time doesn't reveal which emails exist */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    /**
     * FN-AUTH-01. Deviation from the spec: an unknown email returns 401 INVALID_CREDENTIALS
     * (not 404 USER_NOT_FOUND) to avoid account enumeration.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash()) || !CONSOLE_ROLES.contains(user.getRole())) {
            throw invalidCredentials();
        }
        if (!user.isActive()) {
            throw AppException.forbidden("ACCOUNT_LOCKED", "Tài khoản đã bị khóa, vui lòng liên hệ quản lý");
        }
        return respond(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        UUID userId = tokens.verifyRefreshToken(refreshToken);
        User user = activeConsoleUser(userId);
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthUserDto me(UUID userId) {
        return AuthUserDto.of(activeConsoleUser(userId));
    }

    private User activeConsoleUser(UUID userId) {
        return users.findById(userId)
                .filter(User::isActive)
                .filter(u -> CONSOLE_ROLES.contains(u.getRole()))
                .orElseThrow(() -> AppException.unauthorized("UNAUTHORIZED", "Phiên đăng nhập không hợp lệ"));
    }

    private LoginResponse respond(User user) {
        TokenPair pair = tokens.issue(user);
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), AuthUserDto.of(user));
    }

    private static AppException invalidCredentials() {
        return AppException.unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác");
    }
}
