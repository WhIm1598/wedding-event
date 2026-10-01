package com.weddingevent.admin.booking;

import com.weddingevent.admin.booking.BookingService.BookingDto;
import com.weddingevent.admin.booking.BookingService.CalendarView;
import com.weddingevent.admin.booking.BookingService.CreateBookingRequest;
import com.weddingevent.admin.security.CurrentUser;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.exception.AppException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class BookingController {

    private final BookingService service;
    private final Clock clock;

    public BookingController(BookingService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /** GET /bookings/calendar?view=day|week|month&date=YYYY-MM-DD&staffId= */
    @GetMapping("/calendar")
    public ApiResponse<List<BookingDto>> calendar(
            @RequestParam(defaultValue = "day") String view,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID staffId,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @AuthenticationPrincipal Jwt jwt) {
        CalendarView calendarView;
        try {
            calendarView = CalendarView.valueOf(view.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("BAD_REQUEST", "view phải là day, week hoặc month");
        }
        LocalDate day = date == null ? LocalDate.now(clock) : date;
        return ApiResponse.ok(service.calendar(calendarView, day, staffId, includeCancelled, CurrentUser.from(jwt)));
    }

    /** Staff and admins can schedule appointments */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingDto> create(@Valid @RequestBody CreateBookingRequest req) {
        return ApiResponse.ok(service.create(req), "Đã thêm lịch trình");
    }
}
