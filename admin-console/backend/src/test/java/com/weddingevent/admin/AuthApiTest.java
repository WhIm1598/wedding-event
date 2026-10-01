package com.weddingevent.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthApiTest extends IntegrationTest {

    @Test
    void adminLoginReturnsTokensAndProfile() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'ADMIN@lumiere.vn','password':'Admin@123'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.user.email").value(ADMIN_EMAIL));
    }

    @Test
    void wrongPasswordAndUnknownEmailLookIdentical() throws Exception {
        for (String email : new String[] {ADMIN_EMAIL, "nobody@lumiere.vn"}) {
            mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'email':'" + email + "','password':'wrong-password'}")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void invalidLoginPayloadReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'not-an-email','password':'123'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void apiRequiresAToken() throws Exception {
        mvc.perform(get("/api/v1/admin/packages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/auth/me"), staffToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(STAFF_EMAIL))
                .andExpect(jsonPath("$.data.role").value("ROLE_STAFF"));
    }

    @Test
    void staffCanReadButNotManageAdminOnlyResources() throws Exception {
        String staff = staffToken();
        mvc.perform(authed(get("/api/v1/admin/packages"), staff)).andExpect(status().isOk());
        mvc.perform(authed(post("/api/v1/admin/packages"), staff)
                        .content(json("{'name':'Gói X','type':'Trọn gói','price':1000000,'features':[]}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(authed(get("/api/v1/admin/financials/transactions"), staff)).andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/admin/dashboard/stats"), staff)).andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenRenewsSessionButCannotCallTheApi() throws Exception {
        String body = mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'" + ADMIN_EMAIL + "','password':'" + ADMIN_PASSWORD + "'}")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String refresh = read(body, "$.data.refreshToken");

        mvc.perform(get("/api/v1/admin/packages").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/admin/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'" + refresh + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());

        mvc.perform(post("/api/v1/admin/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'garbage'}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }
}
