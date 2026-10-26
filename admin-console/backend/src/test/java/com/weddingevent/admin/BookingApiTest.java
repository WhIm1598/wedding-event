package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.repository.BookingRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
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
    void contractBookingsBelongToTheContractAndItsCustomer() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        String contractId = createContract(admin, phone, 10_000_000, 3_000_000);
        LocalDate date = uniqueFutureDate();

        // Phone and package come from the contract when left empty
        String created = body(post("/api/v1/admin/bookings").content(json("""
                {'clientName':'Phạm Thu Hà','type':'Thử váy cưới','contractId':'%s','eventDate':'%s','eventTime':'10:00','staffIds':[]}
                """.formatted(contractId, date))), admin);
        assertThat(read(created, "$.data.contractId")).isEqualTo(contractId);
        assertThat(read(created, "$.data.phone")).isEqualTo(phone);
        assertThat(read(created, "$.data.packageName")).isNotEqualTo("—");
        Booking saved = bookings.findById(UUID.fromString(read(created, "$.data.id"))).orElseThrow();
        assertThat(saved.getLead().getId()).hasToString(leadId);

        // A contract has many bookings
        createContractBooking(admin, contractId);
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=day&date=" + date), admin))
                .andExpect(jsonPath("$.data[*].contractId", hasItem(contractId)));
    }

    @Test
    void consultationBookingBelongsToACustomerWithoutAContract() throws Exception {
        String admin = adminToken();
        String leadId = createLead(admin, uniquePhone());
        String created = body(post("/api/v1/admin/bookings").content(json("""
                {'clientName':'Khách Tư Vấn','type':'Tư vấn hợp đồng','leadId':'%s','eventDate':'%s','eventTime':'15:00','staffIds':[]}
                """.formatted(leadId, uniqueFutureDate()))), admin);
        assertThat(JsonPath.<Object>read(created, "$.data.contractId")).isNull();
        Booking saved = bookings.findById(UUID.fromString(read(created, "$.data.id"))).orElseThrow();
        assertThat(saved.getLead().getId()).hasToString(leadId);
    }

    @Test
    void bookingLinksAreValidated() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        createLead(admin, phone);
        String contractId = createContract(admin, phone, 1_000_000, 0);
        String otherLead = createLead(admin, uniquePhone());
        String bookingFor = """
                {'clientName':'X','type':'Chụp Pre-wedding','%s':'%s','eventDate':'%s','eventTime':'09:00','staffIds':[]}""";

        mvc.perform(authed(post("/api/v1/admin/bookings"), admin)
                        .content(json(bookingFor.formatted("contractId", UUID.randomUUID(), uniqueFutureDate()))))
                .andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/admin/bookings"), admin)
                        .content(json(bookingFor.formatted("leadId", UUID.randomUUID(), uniqueFutureDate()))))
                .andExpect(status().isNotFound());
        // A customer that isn't the contract's customer
        mvc.perform(authed(post("/api/v1/admin/bookings"), admin).content(json("""
                        {'clientName':'X','type':'Chụp Pre-wedding','contractId':'%s','leadId':'%s','eventDate':'%s','eventTime':'09:00','staffIds':[]}
                        """.formatted(contractId, otherLead, uniqueFutureDate()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("leadId")));
    }

    @Test
    void bookingStatusFollowsTheStateMachine() throws Exception {
        String admin = adminToken();
        String id = createContractBooking(admin, createContract(admin, uniquePhone(), 1_000_000, 0));
        String url = "/api/v1/admin/bookings/" + id + "/status";

        mvc.perform(authed(patch(url), admin).content(json("{'status':'CONFIRMED'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        mvc.perform(authed(patch(url), admin).content(json("{'status':'COMPLETED'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        moveBooking(admin, id, "IN_PROGRESS");
        moveBooking(admin, id, "COMPLETED");
        // A completed booking can't change any more
        mvc.perform(authed(patch(url), admin).content(json("{'status':'IN_PROGRESS'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void bookingStatusRequestIsValidated() throws Exception {
        String admin = adminToken();
        String id = createContractBooking(admin, createContract(admin, uniquePhone(), 1_000_000, 0));
        // Cancelling has its own endpoint (FN-ADM-CAL-03)
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + id + "/status"), admin).content(json("{'status':'CANCELLED'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("status")));
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + id + "/status"), admin).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + UUID.randomUUID() + "/status"), admin).content(json("{'status':'CONFIRMED'}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffOnlyUpdateTheirOwnBookings() throws Exception {
        String admin = adminToken();
        String staffJson = body(get("/api/v1/admin/staff"), admin);
        Object huongId = first(staffJson, "$.data[?(@.name=='Hương Sale')].id");
        Object tuanId = first(staffJson, "$.data[?(@.name=='Tuấn Photo')].id");
        String booking = """
                {'clientName':'Lịch Nhân Sự','type':'Chụp Pre-wedding','eventDate':'%s','eventTime':'13:00','staffIds':['%s']}""";
        String mine = read(body(post("/api/v1/admin/bookings").content(json(booking.formatted(uniqueFutureDate(), huongId))), admin), "$.data.id");
        String theirs = read(body(post("/api/v1/admin/bookings").content(json(booking.formatted(uniqueFutureDate(), tuanId))), admin), "$.data.id");

        String staff = staffToken();
        moveBooking(staff, mine, "CONFIRMED");
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + theirs + "/status"), staff).content(json("{'status':'CONFIRMED'}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void firstBookingInProgressStartsTheContract() throws Exception {
        String admin = adminToken();
        String deposited = createContract(admin, uniquePhone(), 10_000_000, 3_000_000);
        moveBooking(admin, createContractBooking(admin, deposited), "IN_PROGRESS");
        assertThat(contractField(admin, deposited, "status")).isEqualTo("IN_PROGRESS");

        // A draft contract is not effective yet, so a shoot doesn't start it
        String draft = createContract(admin, uniquePhone(), 10_000_000, 0);
        moveBooking(admin, createContractBooking(admin, draft), "IN_PROGRESS");
        assertThat(contractField(admin, draft, "status")).isEqualTo("DRAFT");
    }

    @Test
    void unknownCalendarViewIsABadRequest() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=year"), adminToken()))
                .andExpect(status().isBadRequest());
    }
}
