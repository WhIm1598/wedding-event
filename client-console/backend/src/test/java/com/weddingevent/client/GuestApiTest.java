package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** FN-GUEST-01..02 */
class GuestApiTest extends ClientIntegrationTest {

    private String addGuest(String token, String name, String group, String status) throws Exception {
        return read(body(mvc.perform(authed(post("/api/v1/client/guests"), token)
                        .content(json("{'name':'" + name + "','group':'" + group + "','status':'" + status + "'}")))
                .andExpect(status().isCreated())), "$.data.id");
    }

    @Test
    void listReturnsStatsForTheWholeListAndFiltersTheGuests() throws Exception {
        String token = coupleToken();
        createPlan(token);
        String phone = uniquePhone();
        mvc.perform(authed(post("/api/v1/client/guests"), token)
                        .content(json("{'name':'Phạm Văn An','phone':'" + phone + "','group':'Gia đình nhà Gái','status':'attending',"
                                + "'notes':'Ăn chay','tableNumber':5}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.name").value("Phạm Văn An"))
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.group").value("Gia đình nhà Gái"))
                .andExpect(jsonPath("$.data.status").value("attending"))
                .andExpect(jsonPath("$.data.notes").value("Ăn chay"))
                .andExpect(jsonPath("$.data.tableNumber").value(5));
        addGuest(token, "Nguyễn Thị Bình", "Bạn đại học", "pending");
        addGuest(token, "Lê Cường 100%", "Đồng nghiệp", "declined");

        mvc.perform(authed(get("/api/v1/client/guests"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.total").value(3))
                .andExpect(jsonPath("$.data.stats.attending").value(1))
                .andExpect(jsonPath("$.data.stats.pending").value(1))
                .andExpect(jsonPath("$.data.stats.declined").value(1))
                .andExpect(jsonPath("$.data.guests.length()").value(3))
                .andExpect(jsonPath("$.data.guests[0].name").value("Lê Cường 100%")); // newest first

        mvc.perform(authed(get("/api/v1/client/guests?status=attending"), token))
                .andExpect(jsonPath("$.data.stats.total").value(3))
                .andExpect(jsonPath("$.data.guests.length()").value(1))
                .andExpect(jsonPath("$.data.guests[0].name").value("Phạm Văn An"));
        mvc.perform(authed(get("/api/v1/client/guests?status=all&group=&search="), token))
                .andExpect(jsonPath("$.data.guests.length()").value(3));
        mvc.perform(authed(get("/api/v1/client/guests?search=NGUYỄN"), token))
                .andExpect(jsonPath("$.data.guests.length()").value(1))
                .andExpect(jsonPath("$.data.guests[0].name").value("Nguyễn Thị Bình"));
        mvc.perform(authed(get("/api/v1/client/guests?search=%"), token)) // literal %, not a wildcard
                .andExpect(jsonPath("$.data.guests.length()").value(1));
        mvc.perform(authed(get("/api/v1/client/guests?group=Đồng nghiệp&status=DECLINED"), token))
                .andExpect(jsonPath("$.data.guests.length()").value(1));
    }

    @Test
    void updateChangesTheRsvpOfOwnGuestsOnly() throws Exception {
        String token = coupleToken();
        createPlan(token);
        String id = addGuest(token, "Trần Dũng", "Gia đình nhà Trai", "pending");

        mvc.perform(authed(put("/api/v1/client/guests/" + id), token)
                        .content(json("{'name':'Trần Dũng','group':'Gia đình nhà Trai','status':'attending','tableNumber':3}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("attending"))
                .andExpect(jsonPath("$.data.tableNumber").value(3));

        String stranger = coupleToken();
        createPlan(stranger);
        mvc.perform(authed(put("/api/v1/client/guests/" + id), stranger)
                        .content(json("{'name':'X','status':'declined'}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(put("/api/v1/client/guests/" + UUID.randomUUID()), token)
                        .content(json("{'name':'X'}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void guestValidation() throws Exception {
        String token = coupleToken();
        createPlan(token);
        mvc.perform(authed(post("/api/v1/client/guests"), token)
                        .content(json("{'name':'','phone':'12345','status':'maybe'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(authed(post("/api/v1/client/guests"), token)
                        .content(json("{'name':'" + "a".repeat(101) + "'}")))
                .andExpect(status().isBadRequest());
        mvc.perform(authed(post("/api/v1/client/guests"), token).content(json("{'name':'Chỉ có tên','phone':''}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.phone").doesNotExist());
        mvc.perform(authed(get("/api/v1/client/guests?status=maybe"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void withoutAPlanTheListIsEmptyAndGuestsCannotBeAdded() throws Exception {
        String token = coupleToken();
        mvc.perform(authed(get("/api/v1/client/guests"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.total").value(0))
                .andExpect(jsonPath("$.data.guests.length()").value(0));
        mvc.perform(authed(post("/api/v1/client/guests"), token).content(json("{'name':'A'}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
