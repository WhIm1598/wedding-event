package com.weddingevent.admin.finance;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.finance.FinanceService.CreateTransactionRequest;
import com.weddingevent.admin.finance.FinanceService.SummaryDto;
import com.weddingevent.admin.finance.FinanceService.TransactionDto;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Financials are admin-only (spec §3 RBAC). */
@RestController
@RequestMapping("/api/v1/admin/financials")
@PreAuthorize(ADMIN)
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/transactions")
    public ApiResponse<List<TransactionDto>> transactions() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryDto> summary() {
        return ApiResponse.ok(service.currentMonthSummary());
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionDto> create(@Valid @RequestBody CreateTransactionRequest req) {
        return ApiResponse.ok(service.create(req), "Đã ghi nhận giao dịch");
    }
}
