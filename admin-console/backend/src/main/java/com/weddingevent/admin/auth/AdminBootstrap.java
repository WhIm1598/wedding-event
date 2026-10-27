package com.weddingevent.admin.auth;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * First login of a fresh production database (#36): with env ADMIN_EMAIL + ADMIN_PASSWORD set and no platform admin
 * yet, creates that ROLE_PLATFORM_ADMIN (ADR-0001), who then creates the studios and their admins. Afterwards the
 * variables are ignored; the password is changed in the app (FN-ADM-AUTH-04). A forgotten password: also set
 * ADMIN_RESET_PASSWORD=true and restart, which overwrites that platform admin's password (docs/setup/deploy-linux.md).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;
    private final boolean resetPassword;

    public AdminBootstrap(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.bootstrap-admin.email:}") String email, @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.reset-password:false}") boolean resetPassword) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.password = password;
        this.resetPassword = resetPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (resetPassword) {
            resetPlatformAdminPassword(email, password);
        } else {
            createFirstAdmin(email, password);
        }
    }

    /** Returns whether the admin was created; a weak password or a taken e-mail stops the startup */
    @Transactional
    public boolean createFirstAdmin(String email, String password) {
        if (users.existsByRole(Role.ROLE_PLATFORM_ADMIN)) {
            return false;
        }
        requireStrong(password);
        if (users.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalStateException("ADMIN_EMAIL already belongs to another account");
        }
        User admin = new User();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Quản Trị Nền Tảng");
        admin.setRole(Role.ROLE_PLATFORM_ADMIN);
        admin.setVerified(true);
        users.save(admin);
        log.info("Created the platform admin account from ADMIN_EMAIL");
        return true;
    }

    /** Unlocks the account and ends its sessions; a wrong e-mail stops the startup rather than touching another account */
    @Transactional
    public void resetPlatformAdminPassword(String email, String password) {
        requireStrong(password);
        User admin = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(u -> u.getRole() == Role.ROLE_PLATFORM_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN_EMAIL is not a platform admin account"));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        refreshTokens.revokeAll(admin.getId(), clock.instant());
        log.warn("Reset the platform admin password from ADMIN_PASSWORD; remove ADMIN_RESET_PASSWORD and the ADMIN_* lines from .env");
    }

    private static void requireStrong(String password) {
        if (password.length() < 8 || !password.matches(AuthDtos.PASSWORD_POLICY)) {
            throw new IllegalStateException("ADMIN_PASSWORD must have at least 8 characters with letters and digits");
        }
    }
}
