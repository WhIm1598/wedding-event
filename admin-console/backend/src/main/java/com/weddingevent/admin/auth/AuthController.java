package com.weddingevent.admin.auth;

import com.weddingevent.admin.auth.AuthDtos.AuthUserDto;
import com.weddingevent.admin.auth.AuthDtos.LoginRequest;
import com.weddingevent.admin.auth.AuthDtos.LoginResponse;
import com.weddingevent.admin.auth.AuthDtos.RefreshRequest;
import com.weddingevent.admin.security.CurrentUser;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin console auth lives under /api/v1/admin/** so the nginx gateway routes it to this service. */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(auth.login(req.email(), req.password()), "Đăng nhập thành công");
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(auth.refresh(req.refreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(auth.me(CurrentUser.from(jwt).id()));
    }
}
