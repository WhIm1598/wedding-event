package com.weddingevent.admin;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.repository.BookingRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookingApiTest extends IntegrationTest {

    @Autowired
    private BookingRepository bookings;

    @Test
    void doubleBookingTheSameStaffSlotIsRejected() throws Exception {
        String admin = adminToken();
        String staffJson = mvc.perform(authed(get("/api/v1/admin/staff"), admin))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String tuanId = read(staffJson, "$.data[0].id");
        LocalDate date = uniqueFutureDate();
        String body = json("""
                {'clientName':'An & Bình','phone':'','type':'Chụp Pre-wedding','eventDate':'%s','eventTime':'09:30','staffIds':['%s']}
                """.formatted(date, tuanId));

        mvc.perform(authed(post("/api/v1/admin/bookings"), admin).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.time").value("09:30"))
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.assignedStaff[0].id").value(tuanId));

        mvc.perform(authed(post("/api/v1/admin/bookings"), admin).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAFF_SCHEDULE_CONFLICT"));

        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=week&date=" + date), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].clientName", hasItem("An & Bình")));
    }

    @Test
    void invalidBookingReturnsFieldErrors() throws Exception {
        mvc.perform(authed(post("/api/v1/admin/bookings"), adminToken())
                        .content(json("{'clientName':'','type':'X','eventDate':'2030-01-01','eventTime':'25:00'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("clientName")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("eventTime")));
    }

    @Test
    void staffOnlySeeTheirOwnSchedule() throws Exception {
        // The demo staff account is linked to "Hương Sale"
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=month"), staffToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].assignedStaff[*].name", everyItem(org.hamcrest.Matchers.is("Hương Sale"))));
    }

    @Test
    void bookingInThePastIsRejected() throws Exception {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        mvc.perform(authed(post("/api/v1/admin/bookings"), adminToken()).content(json("""
                        {'clientName':'Quá Khứ','type':'Chụp Pre-wedding','eventDate':'%s','eventTime':'10:00','staffIds':[]}
                        """.formatted(yesterday))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void cancelledBookingsAreHiddenUnlessRequested() throws Exception {
        LocalDate date = uniqueFutureDate();
        Booking b = new Booking();
        b.setCustomerName("Đã Hủy & Lịch");
        b.setType("Thử váy cưới");
        b.setEventDate(date);
        b.setEventTime(LocalTime.of(15, 0));
        b.setStatus(BookingStatus.CANCELLED);
        bookings.save(b);
        String admin = adminToken();

        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=day&date=" + date), admin))
                .andExpect(jsonPath("$.data[*].clientName", not(hasItem("Đã Hủy & Lịch"))));
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=day&includeCancelled=true&date=" + date), admin))
                .andExpect(jsonPath("$.data[*].clientName", hasItem("Đã Hủy & Lịch")));
    }

    @Test
    void unknownCalendarViewIsABadRequest() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=year"), adminToken()))
                .andExpect(status().isBadRequest());
    }
}
