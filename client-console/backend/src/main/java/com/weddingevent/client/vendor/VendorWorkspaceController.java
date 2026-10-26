package com.weddingevent.client.vendor;

import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.client.vendor.VendorWorkspaceService.ActiveRequest;
import com.weddingevent.client.vendor.VendorWorkspaceService.DashboardDto;
import com.weddingevent.client.vendor.VendorWorkspaceService.VendorServiceDto;
import com.weddingevent.client.vendor.VendorWorkspaceService.VendorServiceRequest;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-VND-05 — ROLE_VENDOR: dashboard and the caller's service packages */
@RestController
@RequestMapping("/api/v1/client/vendor")
@PreAuthorize(SecurityConfig.VENDOR)
public class VendorWorkspaceController {

    private final VendorWorkspaceService workspace;

    public VendorWorkspaceController(VendorWorkspaceService workspace) {
        this.workspace = workspace;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> dashboard(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(workspace.dashboard(CurrentUser.id(jwt)));
    }

    @GetMapping("/services")
    public ApiResponse<List<VendorServiceDto>> services(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(workspace.services(CurrentUser.id(jwt)));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VendorServiceDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VendorServiceRequest req) {
        return ApiResponse.ok(workspace.create(CurrentUser.id(jwt), req), "Đã tạo gói dịch vụ");
    }

    @PutMapping("/services/{id}")
    public ApiResponse<VendorServiceDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody VendorServiceRequest req) {
        return ApiResponse.ok(workspace.update(CurrentUser.id(jwt), id, req), "Đã cập nhật gói dịch vụ");
    }

    @PatchMapping("/services/{id}/active")
    public ApiResponse<VendorServiceDto> setActive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ActiveRequest req) {
        return ApiResponse.ok(workspace.setActive(CurrentUser.id(jwt), id, req.isActive()),
                req.isActive() ? "Đã hiện gói dịch vụ" : "Đã ẩn gói dịch vụ");
    }
}
