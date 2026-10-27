package com.weddingevent.admin.user;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.security.CurrentUser;
import com.weddingevent.admin.user.UserAccountService.CreateUserRequest;
import com.weddingevent.admin.user.UserAccountService.CreatedAccountDto;
import com.weddingevent.admin.user.UserAccountService.UpdateUserRequest;
import com.weddingevent.admin.user.UserAccountService.UserAccountDto;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.enums.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-ADM-USER-01: Cài đặt → Tài khoản (admin only) */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize(ADMIN)
public class UserAccountController {

    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserAccountDto>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(service.list(page, size, role, active));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedAccountDto> create(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.ok(service.create(req), "Đã tạo tài khoản");
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserAccountDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest req) {
        return ApiResponse.ok(service.update(CurrentUser.from(jwt).id(), id, req), "Đã cập nhật tài khoản");
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<CreatedAccountDto> resetPassword(@PathVariable UUID id) {
        return ApiResponse.ok(service.resetPassword(id), "Đã đặt lại mật khẩu");
    }
}
