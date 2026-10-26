package com.weddingevent.admin.platform;

import static com.weddingevent.admin.security.SecurityConfig.PLATFORM_ADMIN;

import com.weddingevent.admin.platform.PlatformStudioService.CreateStudioAdminRequest;
import com.weddingevent.admin.platform.PlatformStudioService.CreateStudioRequest;
import com.weddingevent.admin.platform.PlatformStudioService.CreatedStudioDto;
import com.weddingevent.admin.platform.PlatformStudioService.StudioDto;
import com.weddingevent.admin.platform.PlatformStudioService.UpdateStudioRequest;
import com.weddingevent.admin.user.UserAccountService.CreatedAccountDto;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-PLT-01..03: studios of the platform (ROLE_PLATFORM_ADMIN only, ADR-0001) */
@RestController
@RequestMapping("/api/v1/platform/studios")
@PreAuthorize(PLATFORM_ADMIN)
public class PlatformStudioController {

    private final PlatformStudioService service;

    public PlatformStudioController(PlatformStudioService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<StudioDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedStudioDto> create(@Valid @RequestBody CreateStudioRequest req) {
        return ApiResponse.ok(service.create(req), "Đã tạo studio");
    }

    @PatchMapping("/{id}")
    public ApiResponse<StudioDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateStudioRequest req) {
        return ApiResponse.ok(service.update(id, req), "Đã cập nhật studio");
    }

    @PostMapping("/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedAccountDto> createAdmin(@PathVariable UUID id, @Valid @RequestBody CreateStudioAdminRequest req) {
        return ApiResponse.ok(service.createAdmin(id, req), "Đã tạo tài khoản quản lý");
    }
}
