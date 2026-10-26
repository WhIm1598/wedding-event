package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * #33: the database holds the business time the user typed (Asia/Ho_Chi_Minh), whatever time zone the server runs in.
 * TIME columns are stored as entered; TIMESTAMPTZ columns as absolute instants.
 */
class TimeZoneStorageTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theApplicationRunsOnVietnamTime() {
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    void aBookingTimeIsStoredExactlyAsEntered() throws Exception {
        String admin = adminToken();
        String id = read(body(post("/api/v1/admin/bookings").content(json("""
                {'clientName':'Giờ Chuẩn','type':'Chụp Pre-wedding','eventDate':'%s','eventTime':'15:00','staffIds':[]}
                """.formatted(uniqueFutureDate()))), admin), "$.data.id");

        Map<String, Object> row = jdbc.queryForMap(
                "select event_time::text as t, extract(epoch from created_at) as created from bookings where id = ?", UUID.fromString(id));
        assertThat(row.get("t")).isEqualTo("15:00:00");
        // Instants keep their absolute value (no shift by the server's or 1970's offset)
        assertThat(((Number) row.get("created")).doubleValue()).isCloseTo(Instant.now().getEpochSecond(), within(120.0));
        mvc.perform(authed(post("/api/v1/admin/bookings"), admin).content("{}")).andExpect(status().isBadRequest());
    }
}
