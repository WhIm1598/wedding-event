package com.weddingevent.admin.asset;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.asset.AssetService.AssetConflictDto;
import com.weddingevent.admin.asset.AssetService.AssetDto;
import com.weddingevent.admin.asset.AssetService.CreateAssetRequest;
import com.weddingevent.admin.asset.AssetService.ReservationDto;
import com.weddingevent.admin.asset.AssetService.ReserveRequest;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.enums.AssetCategory;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/assets")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AssetDto>> list(@RequestParam(required = false) AssetCategory category) {
        return ApiResponse.ok(service.list(category));
    }

    @GetMapping("/conflicts")
    public ApiResponse<List<AssetConflictDto>> conflicts() {
        return ApiResponse.ok(service.conflicts());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<AssetDto> create(@Valid @RequestBody CreateAssetRequest req) {
        return ApiResponse.ok(service.create(req), "Đã thêm sản phẩm");
    }

    /** POST /assets/booking — reserve an item for a rental period (staff and admins) */
    @PostMapping("/booking")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationDto> reserve(@Valid @RequestBody ReserveRequest req) {
        return ApiResponse.ok(service.reserve(req), "Khóa lịch thành công");
    }
}
