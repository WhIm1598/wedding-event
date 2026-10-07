package com.weddingevent.client.guest;

import com.weddingevent.client.guest.GuestService.GuestDto;
import com.weddingevent.client.guest.GuestService.GuestListDto;
import com.weddingevent.client.guest.GuestService.GuestRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-GUEST-01..02 — ROLE_COUPLE */
@RestController
@RequestMapping("/api/v1/client/guests")
@PreAuthorize(SecurityConfig.COUPLE)
public class GuestController {

    private final GuestService guests;

    public GuestController(GuestService guests) {
        this.guests = guests;
    }

    @GetMapping
    public ApiResponse<GuestListDto> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String search) {
        return ApiResponse.ok(guests.list(CurrentUser.id(jwt), status, group, search));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GuestDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GuestRequest req) {
        return ApiResponse.ok(guests.create(CurrentUser.id(jwt), req), "Đã thêm khách mời");
    }

    @PutMapping("/{id}")
    public ApiResponse<GuestDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody GuestRequest req) {
        return ApiResponse.ok(guests.update(CurrentUser.id(jwt), id, req), "Đã cập nhật khách mời");
    }
}
