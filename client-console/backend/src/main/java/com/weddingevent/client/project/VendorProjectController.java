package com.weddingevent.client.project;

import com.weddingevent.client.project.VendorProjectService.UpdateStatusRequest;
import com.weddingevent.client.project.VendorProjectService.VendorProjectDto;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RestController;

/** FN-VND-04, vendor side — ROLE_VENDOR, the caller's projects */
@RestController
@RequestMapping("/api/v1/client/vendor/projects")
@PreAuthorize(SecurityConfig.VENDOR)
public class VendorProjectController {

    private final VendorProjectService projects;

    public VendorProjectController(VendorProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public ApiResponse<PageResponse<VendorProjectDto>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(projects.listForVendor(CurrentUser.id(jwt), status, page, size));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<VendorProjectDto> updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.ok(projects.updateStatus(CurrentUser.id(jwt), id, req.status()), "Đã cập nhật trạng thái dự án");
    }

    @PostMapping("/{id}/cancel-request/approve")
    public ApiResponse<VendorProjectDto> approveCancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(projects.approveCancel(CurrentUser.id(jwt), id), "Đã đồng ý hủy dự án");
    }

    @PostMapping("/{id}/cancel-request/reject")
    public ApiResponse<VendorProjectDto> rejectCancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(projects.rejectCancel(CurrentUser.id(jwt), id), "Đã từ chối yêu cầu hủy");
    }
}
