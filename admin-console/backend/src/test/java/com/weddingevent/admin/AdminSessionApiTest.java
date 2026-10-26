package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.admin.auth.AdminBootstrap;
import com.weddingevent.admin.auth.RefreshTokenCleanup;
import com.weddingevent.common.domain.RefreshToken;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.RefreshTokenRepository;
import com.weddingevent.common.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/** FN-ADM-AUTH-01 lockout, FN-ADM-AUTH-02 rotation, FN-ADM-AUTH-04 logout / change password, first admin (#36-#42) */
class AdminSessionApiTest extends IntegrationTest {

    private static final String PASSWORD = "Matkhau123";

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository refreshTokens;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AdminBootstrap bootstrap;

    @Autowired
    RefreshTokenCleanup cleanup;

    /** A console account of its own, so changing it can't disturb the other tests */
    private User consoleUser(Role role) {
        User u = new User();
        u.setEmail("console-" + UUID.randomUUID() + "@lumiere.vn");
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setFullName("Nhân Viên Thử");
        u.setRole(role);
        u.setStudioId(demoStudioId());
        u.setVerified(true);
        return users.save(u);
    }

    private ResultActions attempt(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'%s','password':'%s'}".formatted(email, password))));
    }

    private String loginBody(String email) throws Exception {
        return attempt(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'refreshToken':'%s'}".formatted(refreshToken))));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'refreshToken':'%s'}".formatted(refreshToken))));
    }

    // --- FN-ADM-AUTH-01 step 3: lockout ---

    @Test
    void fiveWrongPasswordsLockTheAccountForFifteenMinutes() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        for (int i = 0; i < 4; i++) {
            attempt(u.getEmail(), "sai-mat-khau").andExpect(status().isUnauthorized());
        }
        attempt(u.getEmail(), "sai-mat-khau")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
        // Even the right password is refused while locked
        attempt(u.getEmail(), PASSWORD).andExpect(status().isTooManyRequests());

        Instant lockedUntil = users.findById(u.getId()).orElseThrow().getLockedUntil();
        assertThat(lockedUntil).isBetween(Instant.now().plusSeconds(14 * 60), Instant.now().plusSeconds(15 * 60 + 5));
    }

    @Test
    void anExpiredLockAndASuccessfulLoginResetTheCount() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        u.setLockedUntil(Instant.now().minusSeconds(1));
        u.setFailedLoginCount(4);
        users.save(u);

        attempt(u.getEmail(), PASSWORD).andExpect(status().isOk());
        User after = users.findById(u.getId()).orElseThrow();
        assertThat(after.getFailedLoginCount()).isZero();
        assertThat(after.getLockedUntil()).isNull();
        // The count started over: one wrong attempt doesn't lock
        attempt(u.getEmail(), "sai-mat-khau").andExpect(status().isUnauthorized());
    }

    // --- FN-ADM-AUTH-02: rotation ---

    @Test
    void refreshRotatesAndAReusedTokenEndsEverySessionOfTheUser() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        String r1 = read(loginBody(u.getEmail()), "$.data.refreshToken");
        String r2 = read(refresh(r1).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.refreshToken");
        assertThat(r2).isNotEqualTo(r1);

        // A second tab sending r1 right after the rotation: refused, but nothing revoked
        refresh(r1).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        String r3 = read(refresh(r2).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.refreshToken");

        // r2 again long after it was used: stolen → every session ends, r3 included
        UUID r2Id = refreshTokens.findAll().stream()
                .filter(t -> t.getUser().getId().equals(u.getId()) && t.getUsedAt() != null)
                .max(java.util.Comparator.comparing(t -> t.getUsedAt())).orElseThrow().getId();
        refreshTokens.findById(r2Id).ifPresent(t -> {
            t.setUsedAt(Instant.now().minusSeconds(3600));
            refreshTokens.save(t);
        });
        refresh(r2).andExpect(status().isUnauthorized());
        refresh(r3).andExpect(status().isUnauthorized());
    }

    // --- FN-ADM-AUTH-04 ---

    @Test
    void logoutRevokesTheRefreshTokenAndIsAlways200() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        String r = read(loginBody(u.getEmail()), "$.data.refreshToken");
        logout(r).andExpect(status().isOk());
        logout(r).andExpect(status().isOk());
        logout("garbage").andExpect(status().isOk());
        refresh(r).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllRevokesEverySession() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        String first = loginBody(u.getEmail());
        String second = loginBody(u.getEmail());
        mvc.perform(authed(post("/api/v1/admin/auth/logout-all"), read(first, "$.data.accessToken"))).andExpect(status().isOk());
        refresh(read(first, "$.data.refreshToken")).andExpect(status().isUnauthorized());
        refresh(read(second, "$.data.refreshToken")).andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordChecksTheCurrentOneAndThePolicy() throws Exception {
        User u = consoleUser(Role.ROLE_STAFF);
        String session = loginBody(u.getEmail());
        String access = read(session, "$.data.accessToken");

        mvc.perform(authed(post("/api/v1/admin/auth/change-password"), access)
                        .content(json("{'currentPassword':'sai-mat-khau','newPassword':'MoiHon2026'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        for (String weak : new String[] {"ngan1", "chicochu", "12345678", PASSWORD}) {
            mvc.perform(authed(post("/api/v1/admin/auth/change-password"), access)
                            .content(json("{'currentPassword':'%s','newPassword':'%s'}".formatted(PASSWORD, weak))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
        }

        String renewed = mvc.perform(authed(post("/api/v1/admin/auth/change-password"), access)
                        .content(json("{'currentPassword':'%s','newPassword':'MoiHon2026'}".formatted(PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Other sessions end; the one that changed the password continues with the returned pair
        refresh(read(session, "$.data.refreshToken")).andExpect(status().isUnauthorized());
        refresh(read(renewed, "$.data.refreshToken")).andExpect(status().isOk());
        attempt(u.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        attempt(u.getEmail(), "MoiHon2026").andExpect(status().isOk());
    }

    // --- #42: the account is checked on every request ---

    @Test
    void aLockedOrDemotedAccountLosesAccessImmediately() throws Exception {
        User locked = consoleUser(Role.ROLE_STAFF);
        String lockedToken = read(loginBody(locked.getEmail()), "$.data.accessToken");
        mvc.perform(authed(get("/api/v1/admin/auth/me"), lockedToken)).andExpect(status().isOk());
        locked.setActive(false);
        users.save(locked);
        mvc.perform(authed(get("/api/v1/admin/auth/me"), lockedToken)).andExpect(status().isUnauthorized());

        User demoted = consoleUser(Role.ROLE_ADMIN);
        String adminToken = read(loginBody(demoted.getEmail()), "$.data.accessToken");
        mvc.perform(authed(get("/api/v1/admin/dashboard/stats"), adminToken)).andExpect(status().isOk());
        demoted.setRole(Role.ROLE_STAFF);
        users.save(demoted);
        mvc.perform(authed(get("/api/v1/admin/dashboard/stats"), adminToken)).andExpect(status().isUnauthorized());
    }

    // --- #36: first admin on an empty production database ---

    @Test
    void thePlatformAdminIsCreatedOnlyWhenNoneExists() {
        // The demo seed already has a platform admin, so the env account is ignored
        String email = "owner-" + UUID.randomUUID() + "@lumiere.vn";
        assertThat(bootstrap.createFirstAdmin(email, "Chusohuu2026")).isFalse();
        assertThat(users.existsByEmailIgnoreCase(email)).isFalse();
    }

    @Test
    void cleanupDropsRowsAWeekAfterTheyExpired() {
        User u = consoleUser(Role.ROLE_STAFF);
        RefreshToken old = new RefreshToken();
        old.setUser(u);
        old.setExpiresAt(Instant.now().minus(Duration.ofDays(8)));
        RefreshToken recent = new RefreshToken();
        recent.setUser(u);
        recent.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));
        refreshTokens.saveAll(List.of(old, recent));

        assertThat(cleanup.run()).isPositive();
        assertThat(refreshTokens.existsById(old.getId())).isFalse();
        assertThat(refreshTokens.existsById(recent.getId())).isTrue();
    }
}
