package com.weddingevent.admin.auth;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.UserRepository;
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
 * variables are ignored; the password is changed in the app (FN-ADM-AUTH-04).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email, @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!email.isBlank() && !password.isBlank()) {
            createFirstAdmin(email, password);
        }
    }

    /** Returns whether the admin was created; a weak password or a taken e-mail stops the startup */
    @Transactional
    public boolean createFirstAdmin(String email, String password) {
        if (users.existsByRole(Role.ROLE_PLATFORM_ADMIN)) {
            return false;
        }
        if (password.length() < 8 || !password.matches(AuthDtos.PASSWORD_POLICY)) {
            throw new IllegalStateException("ADMIN_PASSWORD must have at least 8 characters with letters and digits");
        }
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
}
