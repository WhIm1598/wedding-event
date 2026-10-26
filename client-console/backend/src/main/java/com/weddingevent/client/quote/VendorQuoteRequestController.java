package com.weddingevent.client.quote;

import com.weddingevent.client.quote.QuoteRequestService.QuoteRequestDto;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FN-VND-02, vendor side — ROLE_VENDOR, requests sent to the caller's vendor profile */
@RestController
@RequestMapping("/api/v1/client/vendor/quote-requests")
@PreAuthorize(SecurityConfig.VENDOR)
public class VendorQuoteRequestController {

    private final QuoteRequestService quotes;

    public VendorQuoteRequestController(QuoteRequestService quotes) {
        this.quotes = quotes;
    }

    @GetMapping
    public ApiResponse<PageResponse<QuoteRequestDto>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(quotes.listReceived(CurrentUser.id(jwt), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuoteRequestDto> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(quotes.getReceived(CurrentUser.id(jwt), id));
    }
}
