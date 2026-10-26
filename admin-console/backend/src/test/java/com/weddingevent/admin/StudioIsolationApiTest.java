package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * ADR-0001: studios share one database but never see each other's data (FN-PLT-01..03). Studio A is the demo
 * studio; each test creates its own studio B through the platform API.
 */
class StudioIsolationApiTest extends IntegrationTest {

    private static final String PLATFORM_EMAIL = "platform@lumiere.vn";
    private static final String PLATFORM_PASSWORD = "Platform@123";
    private static final String NEW_PASSWORD = "Matkhaumoi123";

    private record StudioB(String id, String adminEmail, String token) {}

    private String platformToken() throws Exception {
        return login(PLATFORM_EMAIL, PLATFORM_PASSWORD);
    }

    private ResultActions createStudio(String token, String name, String adminEmail) throws Exception {
        return mvc.perform(authed(post("/api/v1/platform/studios"), token)
                .content(json("{'name':'%s','adminEmail':'%s','adminName':'Chủ Studio B'}".formatted(name, adminEmail))));
    }

    /** A new studio with its first admin signed in (temporary password already changed) */
    private StudioB studioB() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@studio-b.vn";
        String created = createStudio(platformToken(), "Studio B " + email.substring(6, 12), email)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String temporary = read(created, "$.data.admin.temporaryPassword");
        String first = login(email, temporary);
        String changed = body(post("/api/v1/admin/auth/change-password")
                .content(json("{'currentPassword':'%s','newPassword':'%s'}".formatted(temporary, NEW_PASSWORD))), first);
        return new StudioB(read(created, "$.data.studio.id"), email, read(changed, "$.data.accessToken"));
    }

    private ResultActions loginAttempt(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'%s','password':'%s'}".formatted(email, password))));
    }

    private ResultActions setStatus(String studioId, String status, LocalDate expires) throws Exception {
        String exp = expires == null ? "null" : "'" + expires + "'";
        return mvc.perform(authed(patch("/api/v1/platform/studios/" + studioId), platformToken())
                .content(json("{'status':'%s','subscriptionExpiresAt':%s}".formatted(status, exp))));
    }

    @SuppressWarnings("unchecked")
    private static List<String> ids(String body, String path) {
        return (List<String>) (List<?>) JsonPath.read(body, path);
    }

    @Test
    void onlyThePlatformAdminManagesStudiosAndTheyDontUseStudioRoutes() throws Exception {
        mvc.perform(authed(get("/api/v1/platform/studios"), adminToken())).andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/platform/studios"), staffToken())).andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/admin/contracts"), platformToken())).andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/admin/auth/me"), platformToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_PLATFORM_ADMIN"));
    }

    @Test
    void aNewStudioStartsEmptyWithItsOwnSettingsAndNumbering() throws Exception {
        StudioB b = studioB();

        mvc.perform(authed(get("/api/v1/admin/settings/studio"), b.token()))
                .andExpect(jsonPath("$.data.name").value(org.hamcrest.Matchers.startsWith("Studio B")));
        mvc.perform(authed(get("/api/v1/admin/packages"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/staff"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/contracts"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/crm/pipeline"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/assets"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/financials/transactions"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/notifications"), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/search?q="), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        String accounts = body(get("/api/v1/admin/users"), b.token());
        assertThat(ids(accounts, "$.data.items[*].email")).containsExactly(b.adminEmail());

        // Codes start over in every studio
        String staff = body(post("/api/v1/admin/staff")
                .content(json("{'name':'Thợ B','phone':'%s','position':'Nhiếp ảnh'}".formatted(uniquePhone()))), b.token());
        assertThat(read(staff, "$.data.code")).isEqualTo("NV-001");
        String pkg = body(post("/api/v1/admin/packages")
                .content(json("{'name':'Gói B','type':'Trọn gói','price':5000000,'features':[]}")), b.token());
        String contract = body(post("/api/v1/admin/contracts").content(json("""
                {'customerName':'Khách B','phone':'%s','servicePackageId':'%s','totalAmount':5000000,'depositAmount':1000000}
                """.formatted(uniquePhone(), read(pkg, "$.data.id")))), b.token());
        assertThat(read(contract, "$.data.contractNumber")).isEqualTo("HD-001");
        String cashBook = body(get("/api/v1/admin/financials/transactions"), b.token());
        assertThat(ids(cashBook, "$.data[*].code")).containsExactly("TRX-001");
    }

    @Test
    void aStudioCannotReadOrTouchAnotherStudiosRows() throws Exception {
        String a = adminToken();
        String phone = uniquePhone();
        String leadA = createLead(a, phone);
        String contractA = createContract(a, phone, 8_000_000, 0);
        String bookingA = createContractBooking(a, contractA);
        String pkgA = read(body(get("/api/v1/admin/packages"), a), "$.data[0].id");
        String staffA = read(body(get("/api/v1/admin/staff"), a), "$.data[0].id");
        String settingsA = read(body(get("/api/v1/admin/settings/studio"), a), "$.data.name");
        StudioB b = studioB();

        mvc.perform(authed(get("/api/v1/admin/contracts/" + contractA + "/preview-html"), b.token())).andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/admin/contracts/" + contractA + "/export-pdf"), b.token())).andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractA + "/deliver"), b.token())).andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/admin/bookings/" + bookingA + "/status"), b.token())
                .content(json("{'status':'CONFIRMED'}"))).andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/admin/crm/customers/" + leadA + "/stage"), b.token())
                .content(json("{'stage':'IN_CONSULTATION'}"))).andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/admin/packages/" + pkgA + "/active"), b.token())
                .content(json("{'isActive':false}"))).andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/admin/staff/" + staffA + "/tasks"), b.token())).andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/admin/contracts"), b.token()).content(json("""
                {'customerName':'Khách B','phone':'%s','servicePackageId':'%s','totalAmount':1000000,'depositAmount':0}
                """.formatted(uniquePhone(), pkgA)))).andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/admin/bookings"), b.token()).content(json("""
                {'clientName':'Khách B','type':'Thử váy','eventDate':'%s','eventTime':'09:00','staffIds':['%s']}
                """.formatted(uniqueFutureDate(), staffA)))).andExpect(status().is4xxClientError());
        mvc.perform(authed(get("/api/v1/admin/search?q=" + phone), b.token())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(authed(get("/api/v1/admin/bookings/calendar?view=month&date=" + LocalDate.now()), b.token()))
                .andExpect(jsonPath("$.data[?(@.id=='%s')]".formatted(bookingA)).isEmpty());

        // B's changes stay in B
        mvc.perform(authed(put("/api/v1/admin/settings/studio"), b.token()).content(json("""
                {'name':'Studio B Đổi Tên','address':'1 Đường B','taxCode':'0101','legalRepresentative':'Bê','bankInfo':'STK B','zaloPhone':''}
                """))).andExpect(status().isOk());
        assertThat(read(body(get("/api/v1/admin/settings/studio"), a), "$.data.name")).isEqualTo(settingsA);
        // A still owns its rows
        mvc.perform(authed(get("/api/v1/admin/contracts/" + contractA + "/preview-html"), a)).andExpect(status().isOk());
    }

    @Test
    void anEmailBelongsToOneAccountAcrossStudios() throws Exception {
        createStudio(platformToken(), "Studio Trùng Email", ADMIN_EMAIL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        StudioB b = studioB();
        mvc.perform(authed(post("/api/v1/admin/users"), b.token())
                        .content(json("{'email':'%s','name':'X','role':'ROLE_STAFF','staffId':null}".formatted(STAFF_EMAIL))))
                .andExpect(status().isConflict());
    }

    @Test
    void aSuspendedOrExpiredStudioIsLockedOut() throws Exception {
        StudioB b = studioB();

        setStatus(b.id(), "SUSPENDED", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.usable").value(false));
        mvc.perform(authed(get("/api/v1/admin/packages"), b.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDIO_SUSPENDED"));
        loginAttempt(b.adminEmail(), NEW_PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("STUDIO_SUSPENDED"));

        setStatus(b.id(), "ACTIVE", LocalDate.now().minusDays(1)).andExpect(jsonPath("$.data.usable").value(false));
        loginAttempt(b.adminEmail(), NEW_PASSWORD).andExpect(status().isForbidden());

        setStatus(b.id(), "ACTIVE", LocalDate.now()).andExpect(jsonPath("$.data.usable").value(true));
        loginAttempt(b.adminEmail(), NEW_PASSWORD).andExpect(status().isOk());
        // Other studios are not affected
        mvc.perform(authed(get("/api/v1/admin/packages"), adminToken())).andExpect(status().isOk());
    }

    @Test
    void thePlatformListsStudiosAndCanAddAnAdmin() throws Exception {
        StudioB b = studioB();
        String list = body(get("/api/v1/platform/studios"), platformToken());
        assertThat(first(list, "$.data[?(@.id=='%s')].adminCount".formatted(b.id()))).isEqualTo(1);
        assertThat(first(list, "$.data[?(@.id=='%s')].name".formatted(demoStudioId()))).isNotNull();

        String email = "second-" + UUID.randomUUID() + "@studio-b.vn";
        String created = mvc.perform(authed(post("/api/v1/platform/studios/" + b.id() + "/admins"), platformToken())
                        .content(json("{'email':'%s','name':'Quản Lý Hai'}".formatted(email))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = login(email, read(created, "$.data.temporaryPassword"));
        mvc.perform(authed(get("/api/v1/admin/auth/me"), token)).andExpect(jsonPath("$.data.mustChangePassword").value(true));
        mvc.perform(authed(post("/api/v1/platform/studios/" + UUID.randomUUID() + "/admins"), platformToken())
                .content(json("{'email':'x-%s@b.vn','name':'X'}".formatted(UUID.randomUUID())))).andExpect(status().isNotFound());
        createStudio(platformToken(), "", "y@b.vn").andExpect(status().isBadRequest());
    }
}
