package com.weddingevent.client.catalog;

import com.weddingevent.client.catalog.CatalogService.ServiceDto;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FN-SVC-01..02 — couples and vendors */
@RestController
@RequestMapping("/api/v1/client/services")
@PreAuthorize(SecurityConfig.COUPLE_OR_VENDOR)
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public ApiResponse<PageResponse<ServiceDto>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(catalog.search(category, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.get(id));
    }
}
