package com.weddingevent.admin.user;

import com.weddingevent.admin.security.ConsoleAuthenticationConverter;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.StaffMember;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.StaffRepository;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-ADM-USER-01: admin-console login accounts. Accounts are never deleted, only locked. */
@Service
public class UserAccountService {

    static final int TEMP_PASSWORD_LENGTH = 12;
    /** No look-alike characters (0/O, 1/l/I), since the password is read out or copied by hand */
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    public record UserAccountDto(String id, String email, String name, Role role, String staffId, String staffName,
            boolean isActive, Instant lastLoginAt) {
        static UserAccountDto of(User u) {
            StaffMember s = u.getStaffMember();
            return new UserAccountDto(u.getId().toString(), u.getEmail(), u.getFullName(), u.getRole(),
                    s == null ? null : s.getId().toString(), s == null ? null : s.getFullName(), u.isActive(), u.getLastLoginAt());
        }
    }

    /** The temporary password is only ever shown in this response */
    public record CreatedAccountDto(UserAccountDto user, String temporaryPassword) {}

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String name,
            @NotNull Role role,
            UUID staffId) {}

    /** The edit form always sends every field; {@code staffId = null} unlinks the staff profile */
    public record UpdateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Role role,
            @NotNull Boolean isActive,
            UUID staffId) {}

    private final UserRepository users;
    private final StaffRepository staff;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public UserAccountService(UserRepository users, StaffRepository staff, RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.staff = staff;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAccountDto> list(int page, int size, Role role, Boolean active) {
        return PageResponse.of(users.findConsoleUsers(ConsoleAuthenticationConverter.CONSOLE_ROLES, role, active,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100))), UserAccountDto::of);
    }

    @Transactional
    public CreatedAccountDto create(CreateUserRequest req) {
        requireConsoleRole(req.role());
        String email = req.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("EMAIL_EXISTS", "Email đã được dùng cho một tài khoản khác");
        }
        User u = new User();
        u.setEmail(email);
        u.setFullName(req.name().trim());
        u.setRole(req.role());
        u.setVerified(true);
        u.setStaffMember(linkableStaff(req.staffId(), null));
        String temporary = temporaryPassword(u);
        return new CreatedAccountDto(UserAccountDto.of(users.save(u)), temporary);
    }

    @Transactional
    public UserAccountDto update(UUID actorId, UUID id, UpdateUserRequest req) {
        requireConsoleRole(req.role());
        User u = consoleUser(id);
        boolean staysActiveAdmin = req.isActive() && req.role() == Role.ROLE_ADMIN;
        boolean wasActiveAdmin = u.isActive() && u.getRole() == Role.ROLE_ADMIN;
        if (u.getId().equals(actorId) && !staysActiveAdmin) {
            throw AppException.conflict("CANNOT_MODIFY_SELF", "Không thể tự khóa hoặc tự hạ quyền tài khoản của mình");
        }
        if (wasActiveAdmin && !staysActiveAdmin
                && users.findActiveAdminsForUpdate().stream().noneMatch(a -> !a.getId().equals(u.getId()))) {
            throw AppException.conflict("LAST_ADMIN", "Phải còn ít nhất một quản lý đang hoạt động");
        }
        if (u.isActive() && !req.isActive()) {
            refreshTokens.revokeAll(u.getId(), clock.instant());
        }
        u.setFullName(req.name().trim());
        u.setRole(req.role());
        u.setActive(req.isActive());
        u.setStaffMember(linkableStaff(req.staffId(), u.getId()));
        return UserAccountDto.of(u);
    }

    /** New temporary password; every session of the account ends */
    @Transactional
    public CreatedAccountDto resetPassword(UUID id) {
        User u = consoleUser(id);
        String temporary = temporaryPassword(u);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        refreshTokens.revokeAll(u.getId(), clock.instant());
        return new CreatedAccountDto(UserAccountDto.of(u), temporary);
    }

    private String temporaryPassword(User u) {
        String password;
        do {
            StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
            for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
                sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
            }
            password = sb.toString();
        } while (!password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit));
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setMustChangePassword(true);
        return password;
    }

    /** The staff row lock serializes two requests linking the same profile */
    private StaffMember linkableStaff(UUID staffId, UUID accountId) {
        if (staffId == null) {
            return null;
        }
        List<StaffMember> locked = staff.findAllByIdForUpdate(List.of(staffId));
        if (locked.isEmpty()) {
            throw new ResourceNotFoundException("Nhân sự", staffId);
        }
        users.findByStaffMemberId(staffId).filter(other -> !other.getId().equals(accountId)).ifPresent(other -> {
            throw AppException.conflict("STAFF_ALREADY_LINKED", "Nhân sự này đã có tài khoản đăng nhập");
        });
        return locked.getFirst();
    }

    private User consoleUser(UUID id) {
        return users.findById(id).filter(ConsoleAuthenticationConverter::isConsoleUser)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", id));
    }

    private static void requireConsoleRole(Role role) {
        if (!ConsoleAuthenticationConverter.CONSOLE_ROLES.contains(role)) {
            throw AppException.invalidField("role", "Vai trò phải là ROLE_ADMIN hoặc ROLE_STAFF");
        }
    }
}
