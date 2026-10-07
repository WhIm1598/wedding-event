package com.weddingevent.client.user;

import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.user.UserService.ProfileRequest;
import com.weddingevent.client.user.UserService.RoleRequest;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    /** FN-AUTH-03 */
    @PutMapping("/role")
    public ApiResponse<UserDto> selectRole(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RoleRequest req) {
        return ApiResponse.ok(users.selectRole(CurrentUser.id(jwt), req), "Đã chọn vai trò");
    }

    /** FN-AUTH-04 */
    @PutMapping("/me")
    public ApiResponse<UserDto> updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest req) {
        return ApiResponse.ok(users.updateProfile(CurrentUser.id(jwt), req), "Đã cập nhật hồ sơ");
    }
}
