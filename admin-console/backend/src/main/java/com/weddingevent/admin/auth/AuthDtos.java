package com.weddingevent.admin.auth;

import com.weddingevent.common.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    /** FN-AUTH-01 validation: RFC email, password >= 6 chars */
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    /** FN-ADM-AUTH-04 password policy: 8–100 characters with letters and digits */
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_POLICY, message = "Mật khẩu mới phải có cả chữ và số") String newPassword) {}

    public static final String PASSWORD_POLICY = "^(?=.*\\p{L})(?=.*\\d).*$";

    public record AuthUserDto(String id, String name, String email, String role, boolean mustChangePassword) {
        public static AuthUserDto of(User u) {
            return new AuthUserDto(u.getId().toString(), u.getFullName(), u.getEmail(), u.getRole().name(), u.isMustChangePassword());
        }
    }

    public record LoginResponse(String accessToken, String refreshToken, AuthUserDto user) {}
}
