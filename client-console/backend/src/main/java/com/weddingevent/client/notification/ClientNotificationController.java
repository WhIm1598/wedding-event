package com.weddingevent.client.notification;

import com.weddingevent.client.notification.ClientNotificationService.NotificationDto;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FN-NOTI-01 — any signed-in user, each sees only their own inbox */
@RestController
@RequestMapping("/api/v1/client/notifications")
public class ClientNotificationController {

    private final ClientNotificationService service;

    public ClientNotificationController(ClientNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationDto>> inbox(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.inbox(CurrentUser.id(jwt), page, size));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(service.markRead(CurrentUser.id(jwt), id));
    }
}
