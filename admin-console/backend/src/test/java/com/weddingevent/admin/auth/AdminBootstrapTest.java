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
import com.weddingevent.common.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** #36: the empty-database path, which the shared test database (seeded with an admin) can't reach */
class AdminBootstrapTest {

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(users, encoder, email, password);
    }

    @Test
    void createsTheAdminFromTheEnvironmentOnAnEmptyDatabase() {
        bootstrap(" Owner@Studio.vn ", "Chusohuu2026").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@studio.vn");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ROLE_ADMIN);
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
    void anExistingAdminWins() {
        when(users.existsByRole(Role.ROLE_ADMIN)).thenReturn(true);
        assertThat(bootstrap("a@studio.vn", "Chusohuu2026").createFirstAdmin("a@studio.vn", "Chusohuu2026")).isFalse();
        verify(users, never()).save(any());
    }
}
