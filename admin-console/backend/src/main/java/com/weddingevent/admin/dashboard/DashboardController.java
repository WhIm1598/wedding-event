package com.weddingevent.admin.dashboard;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.dashboard.DashboardService.RevenuePoint;
import com.weddingevent.admin.dashboard.DashboardService.StatsDto;
import com.weddingevent.common.api.ApiResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize(ADMIN)
public class DashboardController {

    private final DashboardService service;
    private final Clock clock;

    public DashboardController(DashboardService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/stats")
    public ApiResponse<StatsDto> stats() {
        return ApiResponse.ok(service.stats());
    }

    @GetMapping("/revenue-chart")
    public ApiResponse<List<RevenuePoint>> revenueChart(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.revenueChart(year == null ? LocalDate.now(clock).getYear() : year));
    }
}
