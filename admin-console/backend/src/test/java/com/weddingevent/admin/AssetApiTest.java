package com.weddingevent.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.util.VndFormatter;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class AssetApiTest extends IntegrationTest {

    @Test
    void maintenanceBufferBlocksBackToBackRentals() throws Exception {
        String admin = adminToken();
        String code = "TST-" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        mvc.perform(authed(post("/api/v1/admin/assets"), admin).content(json(
                        "{'code':'" + code + "','name':'Váy thử nghiệm','category':'DRESS','size':'M','maintenanceBufferDays':3}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        LocalDate start = uniqueFutureDate();
        // Rent days 0..2 -> locked until day 5 (3-day buffer)
        mvc.perform(authed(post("/api/v1/admin/assets/booking"), admin).content(json(
                        "{'assetId':'" + code + "','rentalStartDate':'" + start + "','rentalEndDate':'" + start.plusDays(2) + "'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lockEndDate").value(start.plusDays(5).toString()));

        // Day 4 falls inside the cleaning buffer
        mvc.perform(authed(post("/api/v1/admin/assets/booking"), admin).content(json(
                        "{'assetId':'" + code + "','rentalStartDate':'" + start.plusDays(4) + "','rentalEndDate':'" + start.plusDays(4) + "'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSET_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString(VndFormatter.date(start.plusDays(5)))));

        // A rental that would end inside an existing one's window is also rejected
        mvc.perform(authed(post("/api/v1/admin/assets/booking"), admin).content(json(
                        "{'assetId':'" + code + "','rentalStartDate':'" + start.minusDays(4) + "','rentalEndDate':'" + start.minusDays(1) + "'}")))
                .andExpect(status().isConflict());

        // Day 6 is free again
        mvc.perform(authed(post("/api/v1/admin/assets/booking"), admin).content(json(
                        "{'assetId':'" + code + "','rentalStartDate':'" + start.plusDays(6) + "','rentalEndDate':'" + start.plusDays(7) + "'}")))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateAssetCodeIsAConflict() throws Exception {
        mvc.perform(authed(post("/api/v1/admin/assets"), adminToken()).content(json(
                        "{'code':'VAY-001','name':'Trùng mã','category':'DRESS','maintenanceBufferDays':1}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSET_CODE_EXISTS"));
    }

    @Test
    void seededDataShowsDerivedStatusesAndConflictBanner() throws Exception {
        String staff = staffToken();
        mvc.perform(authed(get("/api/v1/admin/assets?category=SUIT"), staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='VST-002')].status", hasItem("IN_USE")));
        mvc.perform(authed(get("/api/v1/admin/assets"), staff))
                .andExpect(jsonPath("$.data[?(@.code=='VAY-003')].status", hasItem("MAINTENANCE")));
        mvc.perform(authed(get("/api/v1/admin/assets/conflicts"), staff))
                .andExpect(jsonPath("$.data[*].assetCode", hasItem("VAY-001")));
    }
}
