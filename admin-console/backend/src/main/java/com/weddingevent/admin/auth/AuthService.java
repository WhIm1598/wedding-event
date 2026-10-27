package com.weddingevent.admin.auth;

import com.weddingevent.admin.auth.AuthDtos.AuthUserDto;
import com.weddingevent.admin.auth.AuthDtos.LoginResponse;
import com.weddingevent.admin.security.ConsoleAuthenticationConverter;
import com.weddingevent.admin.security.TokenService;
import com.weddingevent.admin.security.TokenService.TokenPair;
import com.weddingevent.common.domain.RefreshToken;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOGIN_LOCK = Duration.ofMinutes(15);
    /**
     * A rotated refresh token sent again this soon is a second browser tab that refreshed at the same moment, not a
     * stolen copy: refuse it without ending every session (the tab then picks up the new pair from localStorage).
     */
    static final Duration REUSE_GRACE = Duration.ofSeconds(60);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final Clock clock;
    /** Compared against when the email is unknown, so response time doesn't reveal which emails exist */
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
            TokenService tokens, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    /**
     * FN-ADM-AUTH-01. Deviation from the spec: an unknown email returns 401 INVALID_CREDENTIALS
     * (not 404 USER_NOT_FOUND) to avoid account enumeration. The failed-attempt count must survive the 401/429, hence
     * no rollback for business errors; the row lock makes concurrent wrong attempts all count.
     */
    @Transactional(noRollbackFor = AppException.class)
    public LoginResponse login(String email, String password) {
        User user = users.findByEmailForUpdate(email.trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw invalidCredentials();
        }
        Instant now = clock.instant();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw tooManyAttempts();
        }
        boolean passwordOk = passwordEncoder.matches(password, user.getPasswordHash());
        // Only console accounts are locked: a mobile account can't sign in here anyway
        if (!passwordOk && ConsoleAuthenticationConverter.isConsoleUser(user)) {
            int failed = user.getFailedLoginCount() + 1;
            if (failed >= MAX_FAILED_LOGINS) {
                user.setFailedLoginCount(0);
                user.setLockedUntil(now.plus(LOGIN_LOCK));
                throw tooManyAttempts();
            }
            user.setFailedLoginCount(failed);
        }
        if (!passwordOk || !ConsoleAuthenticationConverter.isConsoleUser(user)) {
            throw invalidCredentials();
        }
        if (!user.isActive()) {
            throw AppException.forbidden("ACCOUNT_LOCKED", "Tài khoản đã bị khóa, vui lòng liên hệ quản lý");
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        return respond(user);
    }

    /**
     * FN-ADM-AUTH-02: exchanges a refresh token for a new pair (rotation). A token seen again well after it was
     * exchanged has been copied: every session of the user is revoked, and that must be kept although the call fails.
     */
    @Transactional(noRollbackFor = AppException.class)
    public LoginResponse refresh(String refreshToken) {
        RefreshToken row = refreshTokens.findByIdForUpdate(tokens.refreshTokenId(refreshToken))
                .orElseThrow(TokenService::invalidRefreshToken);
        Instant now = clock.instant();
        User user = row.getUser();
        if (!row.isUsable(now) || !ConsoleAuthenticationConverter.isConsoleUser(user)) {
            throw TokenService.invalidRefreshToken();
        }
        if (row.getUsedAt() != null) {
            if (row.getUsedAt().plus(REUSE_GRACE).isBefore(now)) {
                refreshTokens.revokeAll(user.getId(), now);
            }
            throw TokenService.invalidRefreshToken();
        }
        if (!user.isActive()) {
            throw TokenService.invalidRefreshToken();
        }
        row.setUsedAt(now);
        return respond(user);
    }

    /** FN-ADM-AUTH-04: revokes this refresh token; silent about invalid ones (always 200) */
    @Transactional
    public void logout(String refreshToken) {
        UUID id;
        try {
            id = tokens.refreshTokenId(refreshToken);
        } catch (AppException e) {
            return;
        }
        refreshTokens.findById(id).filter(t -> t.getRevokedAt() == null).ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    /** FN-ADM-AUTH-04: every session of the caller has to sign in again (access tokens run out within 2 h) */
    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokens.revokeAll(userId, clock.instant());
    }

    /** FN-ADM-AUTH-04: ends every other session and returns a fresh pair for this one */
    @Transactional
    public LoginResponse changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = activeConsoleUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw AppException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw AppException.invalidField("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        refreshTokens.revokeAll(userId, clock.instant());
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthUserDto me(UUID userId) {
        return AuthUserDto.of(activeConsoleUser(userId));
    }

    private User activeConsoleUser(UUID userId) {
        return users.findById(userId)
                .filter(User::isActive)
                .filter(ConsoleAuthenticationConverter::isConsoleUser)
                .orElseThrow(() -> AppException.unauthorized("UNAUTHORIZED", "Phiên đăng nhập không hợp lệ"));
    }

    private LoginResponse respond(User user) {
        TokenPair pair = tokens.issue(user);
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), AuthUserDto.of(user));
    }

    private static AppException invalidCredentials() {
        return AppException.unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác");
    }

    private static AppException tooManyAttempts() {
        return new AppException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                "Đăng nhập sai quá " + MAX_FAILED_LOGINS + " lần, vui lòng thử lại sau " + LOGIN_LOCK.toMinutes() + " phút");
    }
}
