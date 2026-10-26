package com.weddingevent.client.notification;

import com.weddingevent.client.notification.DeviceService.RegisterDeviceRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FN-NOTI-02 — any signed-in user, also before a role is chosen */
@RestController
@RequestMapping("/api/v1/users/me/devices")
public class DeviceController {

    private final DeviceService devices;

    public DeviceController(DeviceService devices) {
        this.devices = devices;
    }

    @PutMapping
    public ApiResponse<Void> register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterDeviceRequest req) {
        devices.register(CurrentUser.id(jwt), req);
        return ApiResponse.ok(null, "Đã đăng ký thiết bị nhận thông báo");
    }

    @DeleteMapping
    public ApiResponse<Void> unregister(@AuthenticationPrincipal Jwt jwt, @RequestParam String token) {
        devices.unregister(CurrentUser.id(jwt), token);
        return ApiResponse.ok(null, "Đã hủy đăng ký thiết bị");
    }
}
