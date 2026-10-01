package com.weddingevent.admin.notification;

import com.weddingevent.admin.notification.NotificationService.NotificationDto;
import com.weddingevent.common.api.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NotificationDto>> list() {
        return ApiResponse.ok(service.latest());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        service.markRead(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        service.markAllRead();
        return ApiResponse.ok(null);
    }
}
