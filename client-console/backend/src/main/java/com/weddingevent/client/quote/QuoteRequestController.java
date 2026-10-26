package com.weddingevent.client.quote;

import com.weddingevent.client.quote.QuoteRequestService.QuoteRequestDto;
import com.weddingevent.client.quote.QuoteRequestService.SendQuoteRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-VND-01..02, couple side — ROLE_COUPLE */
@RestController
@RequestMapping("/api/v1/client/quote-requests")
@PreAuthorize(SecurityConfig.COUPLE)
public class QuoteRequestController {

    private final QuoteRequestService quotes;

    public QuoteRequestController(QuoteRequestService quotes) {
        this.quotes = quotes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuoteRequestDto> send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendQuoteRequest req) {
        return ApiResponse.ok(quotes.send(CurrentUser.id(jwt), req), "Đã gửi yêu cầu báo giá");
    }

    @GetMapping
    public ApiResponse<PageResponse<QuoteRequestDto>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(quotes.listSent(CurrentUser.id(jwt), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuoteRequestDto> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(quotes.getSent(CurrentUser.id(jwt), id));
    }
}
