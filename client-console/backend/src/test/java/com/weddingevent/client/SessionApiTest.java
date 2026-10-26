package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** FN-AUTH-07: rotating refresh tokens, reuse detection, logout */
class SessionApiTest extends ClientIntegrationTest {

    /** {accessToken, refreshToken} of a fresh login */
    private String[] signIn(User user) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'" + user.getEmail() + "','password':'" + PASSWORD + "'}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new String[] {JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken")};
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'refreshToken':'" + refreshToken + "'}")));
    }

    @Test
    void refreshingRotatesThePairAndTheNewAccessTokenWorks() throws Exception {
        User user = createUser(Role.ROLE_COUPLE, true);
        String[] first = signIn(user);

        String body = body(refresh(first[1])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.email").value(user.getEmail())));
        String access = read(body, "$.data.accessToken");
        String next = read(body, "$.data.refreshToken");
        org.assertj.core.api.Assertions.assertThat(next).isNotEqualTo(first[1]);

        mvc.perform(authed(get("/api/v1/auth/me"), access)).andExpect(status().isOk());
        refresh(next).andExpect(status().isOk());
    }

    @Test
    void aReusedRefreshTokenRevokesEverySessionOfTheUser() throws Exception {
        User user = createUser(Role.ROLE_COUPLE, true);
        String stolen = signIn(user)[1];
        String otherDevice = signIn(user)[1];
        String rotated = read(body(refresh(stolen).andExpect(status().isOk())), "$.data.refreshToken");

        // The old token comes back: someone else has it
        refresh(stolen)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        refresh(rotated).andExpect(status().isUnauthorized());
        refresh(otherDevice).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyValidRefreshTokensAreAccepted() throws Exception {
        String[] pair = signIn(createUser(Role.ROLE_COUPLE, true));
        refresh(pair[0]) // an access token
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        refresh("not-a-jwt").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
        // A refresh token can't call the API
        mvc.perform(authed(get("/api/v1/auth/me"), pair[1])).andExpect(status().isUnauthorized());
    }

    @Test
    void aLockedAccountCannotRefresh() throws Exception {
        User user = createUser(Role.ROLE_COUPLE, true);
        String token = signIn(user)[1];
        user.setActive(false);
        users.save(user);
        refresh(token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    com.weddingevent.common.repository.RefreshTokenRepository refreshTokens;

    @org.springframework.beans.factory.annotation.Autowired
    com.weddingevent.client.auth.RefreshTokenCleanup cleanup;

    @Test
    void theDailyJobDropsTokensAWeekAfterTheyExpired() {
        User user = createUser(Role.ROLE_COUPLE, true);
        com.weddingevent.common.domain.RefreshToken old = new com.weddingevent.common.domain.RefreshToken();
        old.setUser(user);
        old.setExpiresAt(java.time.Instant.now().minus(java.time.Duration.ofDays(8)));
        com.weddingevent.common.domain.RefreshToken recent = new com.weddingevent.common.domain.RefreshToken();
        recent.setUser(user);
        recent.setExpiresAt(java.time.Instant.now().minus(java.time.Duration.ofDays(2)));
        refreshTokens.saveAll(java.util.List.of(old, recent));

        org.assertj.core.api.Assertions.assertThat(cleanup.run()).isPositive();
        org.assertj.core.api.Assertions.assertThat(refreshTokens.findById(old.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(refreshTokens.findById(recent.getId())).isPresent();
    }

    @Test
    void logoutRevokesOneSessionAndLogoutAllRevokesTheRest() throws Exception {
        User user = createUser(Role.ROLE_VENDOR, true);
        String[] phone = signIn(user);
        String tablet = signIn(user)[1];
        String laptop = signIn(user)[1];

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'" + tablet + "'}")))
                .andExpect(status().isOk());
        refresh(tablet).andExpect(status().isUnauthorized());
        // Idempotent and silent about bad tokens
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'" + tablet + "'}")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'garbage'}")))
                .andExpect(status().isOk());

        mvc.perform(authed(post("/api/v1/auth/logout-all"), phone[0])).andExpect(status().isOk());
        refresh(laptop).andExpect(status().isUnauthorized());
        refresh(phone[1]).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout-all")).andExpect(status().isUnauthorized());
    }
}
