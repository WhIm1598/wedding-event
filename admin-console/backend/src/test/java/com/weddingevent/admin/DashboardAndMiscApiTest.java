package com.weddingevent.admin;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DashboardAndMiscApiTest extends IntegrationTest {

    @Test
    void dashboardStatsAndRevenueChart() throws Exception {
        String admin = adminToken();
        mvc.perform(authed(get("/api/v1/admin/dashboard/stats"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyRevenue.amount").isNumber())
                .andExpect(jsonPath("$.data.monthlyRevenue.formatted").isString())
                .andExpect(jsonPath("$.data.activeContractsCount.value", greaterThan(0)))
                .andExpect(jsonPath("$.data.pendingLeadsCount.isIncrease").isBoolean())
                .andExpect(jsonPath("$.data.costumeRentalRate.percentage").isNumber());
        mvc.perform(authed(get("/api/v1/admin/dashboard/revenue-chart"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12))
                .andExpect(jsonPath("$.data[0].month").value(1));
    }

    @Test
    void crmLeadMovesThroughPipeline() throws Exception {
        String staff = staffToken();
        String phone = uniquePhone();
        String body = mvc.perform(authed(post("/api/v1/admin/crm/customers"), staff).content(json(
                        "{'name':'Khoa & Vy','phone':'" + phone + "','hasZalo':true,'interest':'Gói Kim Cương'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stage").value("NEW_LEAD"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = read(body, "$.data.id");
        String stageUrl = "/api/v1/admin/crm/customers/" + id + "/stage";

        mvc.perform(authed(patch(stageUrl), staff).content(json("{'stage':'AWAITING_DEPOSIT'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("AWAITING_DEPOSIT"));
        mvc.perform(authed(get("/api/v1/admin/search?q=khoa"), staff))
                .andExpect(jsonPath("$.data[*].id", hasItem(id)));

        // Contract stages need a contract first (§1.6)
        mvc.perform(authed(patch(stageUrl), staff).content(json("{'stage':'IN_PROGRESS'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        // Signing a contract (same phone) moves the lead to IN_PROGRESS; COMPLETED is then allowed
        String admin = adminToken();
        String pkgs = mvc.perform(authed(get("/api/v1/admin/packages"), admin)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mvc.perform(authed(post("/api/v1/admin/contracts"), admin).content(json("""
                        {'customerName':'Khoa & Vy','phone':'%s','servicePackageId':'%s','totalAmount':6000000,'depositAmount':0}
                        """.formatted(phone, read(pkgs, "$.data[0].id")))))
                .andExpect(status().isCreated());
        mvc.perform(authed(patch(stageUrl), staff).content(json("{'stage':'COMPLETED'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("COMPLETED"));
    }

    @Test
    void staffTasksLifecycle() throws Exception {
        String admin = adminToken();
        String staffList = mvc.perform(authed(get("/api/v1/admin/staff"), admin)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String staffId = read(staffList, "$.data[1].id");
        String task = mvc.perform(authed(post("/api/v1/admin/staff/" + staffId + "/tasks"), admin).content(json(
                        "{'title':'Chuẩn bị váy cưới','dueDate':'2030-05-01'}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String taskId = read(task, "$.data.id");
        mvc.perform(authed(patch("/api/v1/admin/staff/tasks/" + taskId), admin).content(json("{'completed':true}")))
                .andExpect(jsonPath("$.data.completed").value(true));
        mvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/admin/staff/tasks/" + taskId), admin))
                .andExpect(status().isOk());
    }

    @Test
    void settingsReadableByStaffEditableByAdmin() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/settings/studio"), staffToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("LUMIÈRE STUDIOS"));
        mvc.perform(authed(put("/api/v1/admin/settings/studio"), staffToken()).content(json(
                        "{'name':'X','address':'Y','taxCode':'1','legalRepresentative':'Z','bankInfo':'B'}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificationsCanBeMarkedRead() throws Exception {
        String admin = adminToken();
        mvc.perform(authed(patch("/api/v1/admin/notifications/read-all"), admin)).andExpect(status().isOk());
        mvc.perform(authed(get("/api/v1/admin/notifications"), admin))
                .andExpect(jsonPath("$.data[*].read", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    void unknownIdReturnsNotFoundEnvelope() throws Exception {
        mvc.perform(authed(patch("/api/v1/admin/notifications/00000000-0000-0000-0000-000000000000/read"), adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
