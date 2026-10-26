package com.weddingevent.client.project;

import com.weddingevent.client.project.VendorProjectService.CancelRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FN-VND-04, couple side — ROLE_COUPLE, the vendors the caller booked */
@RestController
@RequestMapping("/api/v1/client/vendor-projects")
@PreAuthorize(SecurityConfig.COUPLE)
public class CoupleProjectController {

    private final VendorProjectService projects;

    public CoupleProjectController(VendorProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public ApiResponse<PageResponse<VendorProjectDto>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(projects.listForCouple(CurrentUser.id(jwt), page, size));
    }

    @PostMapping("/{id}/cancel-request")
    public ApiResponse<VendorProjectDto> requestCancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody CancelRequest req) {
        return ApiResponse.ok(projects.requestCancel(CurrentUser.id(jwt), id, req), "Đã gửi yêu cầu hủy, chờ Vendor duyệt");
    }
}
