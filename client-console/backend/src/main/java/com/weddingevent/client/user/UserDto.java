package com.weddingevent.client.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.common.domain.User;

/** The {@code user} object of FN-AUTH-01, also returned by FN-AUTH-03/04 and GET /auth/me. {@code role} is null until chosen. */
public record UserDto(
        String id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String role,
        boolean hasPlan,
        @JsonProperty("isVerified") boolean isVerified) {

    public static UserDto of(User u, boolean hasPlan) {
        return new UserDto(u.getId().toString(), u.getEmail(), u.getFullName(), u.getPhone(), u.getAvatarUrl(),
                u.getRole() == null ? null : u.getRole().name(), hasPlan, u.isVerified());
    }
}
