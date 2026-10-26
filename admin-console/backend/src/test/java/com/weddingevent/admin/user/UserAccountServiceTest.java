package com.weddingevent.admin.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddingevent.admin.user.UserAccountService.UpdateUserRequest;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.StaffRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.tenant.StudioTenant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** LAST_ADMIN can't be reached in the shared integration database without locking the demo admin, so it is unit-tested */
class UserAccountServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserAccountService service = new UserAccountService(users, mock(StaffRepository.class),
            mock(RefreshTokenRepository.class), mock(PasswordEncoder.class), Clock.systemUTC());

    private final UUID studio = UUID.randomUUID();

    private User admin() {
        User u = new User();
        u.setStudioId(studio);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        u.setRole(Role.ROLE_ADMIN);
        u.setFullName("Quản Lý");
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void theLastActiveAdminCannotBeLockedOrDemoted() {
        User last = admin();
        when(users.findActiveAdminsForUpdate(studio)).thenReturn(List.of(last));
        UUID actor = UUID.randomUUID();

        for (UpdateUserRequest req : List.of(new UpdateUserRequest("Quản Lý", Role.ROLE_STAFF, true, null),
                new UpdateUserRequest("Quản Lý", Role.ROLE_ADMIN, false, null))) {
            assertThatThrownBy(() -> StudioTenant.callAs(studio, () -> service.update(actor, last.getId(), req)))
                    .isInstanceOf(AppException.class)
                    .extracting("code").isEqualTo("LAST_ADMIN");
        }
    }

    @Test
    void anAdminCanBeDemotedWhileAnotherStaysActive() {
        User target = admin();
        User other = admin();
        when(users.findActiveAdminsForUpdate(studio)).thenReturn(List.of(target, other));

        assertThat(StudioTenant.callAs(studio, () -> service.update(other.getId(), target.getId(),
                new UpdateUserRequest("Quản Lý", Role.ROLE_STAFF, true, null))).role())
                .isEqualTo(Role.ROLE_STAFF);
    }
}
