package com.weddingevent.admin.auth;

import com.weddingevent.common.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    /** FN-AUTH-01 validation: RFC email, password >= 6 chars */
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthUserDto(String id, String name, String email, String role) {
        public static AuthUserDto of(User u) {
            return new AuthUserDto(u.getId().toString(), u.getFullName(), u.getEmail(), u.getRole().name());
        }
    }

    public record LoginResponse(String accessToken, String refreshToken, AuthUserDto user) {}
}
