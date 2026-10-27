package com.weddingevent.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** #36: the empty-database path, which the shared test database (seeded with an admin) can't reach */
class AdminBootstrapTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T03:00:00Z"), ZoneOffset.UTC);

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(users, refreshTokens, encoder, clock, email, password, false);
    }

    private AdminBootstrap resetting(String email, String password) {
        return new AdminBootstrap(users, refreshTokens, encoder, clock, email, password, true);
    }

    private User account(Role role) {
        User u = new User();
        u.setEmail("owner@studio.vn");
        u.setPasswordHash(encoder.encode("Quenmatkhau1"));
        u.setRole(role);
        u.setFailedLoginCount(3);
        u.setLockedUntil(clock.instant().plusSeconds(600));
        return u;
    }

    @Test
    void createsThePlatformAdminFromTheEnvironmentOnAnEmptyDatabase() {
        bootstrap(" Owner@Studio.vn ", "Chusohuu2026").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@studio.vn");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ROLE_PLATFORM_ADMIN);
        assertThat(saved.getValue().getStudioId()).isNull();
        assertThat(encoder.matches("Chusohuu2026", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void doesNothingWithoutTheVariables() {
        bootstrap("", "").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void refusesAWeakPasswordOrATakenEmail() {
        assertThatThrownBy(() -> bootstrap("a@studio.vn", "chicochu").run(null)).isInstanceOf(IllegalStateException.class);
        when(users.existsByEmailIgnoreCase("couple@example.com")).thenReturn(true);
        assertThatThrownBy(() -> bootstrap("couple@example.com", "Chusohuu2026").run(null)).isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test
    void anExistingPlatformAdminWins() {
        when(users.existsByRole(Role.ROLE_PLATFORM_ADMIN)).thenReturn(true);
        assertThat(bootstrap("a@studio.vn", "Chusohuu2026").createFirstAdmin("a@studio.vn", "Chusohuu2026")).isFalse();
        verify(users, never()).save(any());
    }

    @Test
    void resetsAForgottenPlatformAdminPasswordAndEndsItsSessions() {
        User admin = account(Role.ROLE_PLATFORM_ADMIN);
        when(users.findByEmailIgnoreCase("owner@studio.vn")).thenReturn(Optional.of(admin));

        resetting(" Owner@Studio.vn ", "Matkhaumoi2026").run(null);

        assertThat(encoder.matches("Matkhaumoi2026", admin.getPasswordHash())).isTrue();
        assertThat(admin.getFailedLoginCount()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
        verify(refreshTokens).revokeAll(admin.getId(), clock.instant());
        verify(users, never()).save(any());
    }

    @Test
    void resetOnlyTouchesAnExistingPlatformAdmin() {
        User studioAdmin = account(Role.ROLE_ADMIN);
        when(users.findByEmailIgnoreCase("owner@studio.vn")).thenReturn(Optional.of(studioAdmin));
        String before = studioAdmin.getPasswordHash();

        assertThatThrownBy(() -> resetting("owner@studio.vn", "Matkhaumoi2026").run(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> resetting("nobody@studio.vn", "Matkhaumoi2026").run(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> resetting("owner@studio.vn", "yeu").run(null)).isInstanceOf(IllegalStateException.class);
        assertThat(studioAdmin.getPasswordHash()).isEqualTo(before);
        verify(refreshTokens, never()).revokeAll(any(), any());
    }
}
