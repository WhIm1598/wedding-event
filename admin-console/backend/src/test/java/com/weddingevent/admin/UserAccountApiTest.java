package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/** FN-ADM-USER-01 account management and FN-ADM-AUTH-01 step 6 (temporary password must be changed) */
class UserAccountApiTest extends IntegrationTest {

    private static final String NEW_PASSWORD = "Matkhaumoi123";

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static String uniqueEmail() {
        return "tk-" + UUID.randomUUID() + "@lumiere.vn";
    }

    private ResultActions create(String token, String email, String role, String staffId) throws Exception {
        String staff = staffId == null ? "null" : "'" + staffId + "'";
        return mvc.perform(authed(post("/api/v1/admin/users"), token)
                .content(json("{'email':'%s','name':'Nhân Viên Mới','role':'%s','staffId':%s}".formatted(email, role, staff))));
    }

    private String createdBody(String email, String role, String staffId) throws Exception {
        return create(adminToken(), email, role, staffId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions update(String token, String id, String name, String role, boolean active, String staffId) throws Exception {
        String staff = staffId == null ? "null" : "'" + staffId + "'";
        return mvc.perform(authed(patch("/api/v1/admin/users/" + id), token).content(json(
                "{'name':'%s','role':'%s','isActive':%s,'staffId':%s}".formatted(name, role, active, staff))));
    }

    private String loginBody(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'%s'}".formatted(email, password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Signs in with the temporary password and changes it; returns an access token with the full role */
    private String activate(String email, String temporary) throws Exception {
        String token = read(loginBody(email, temporary), "$.data.accessToken");
        String changed = body(post("/api/v1/admin/auth/change-password")
                .content(json("{'currentPassword':'%s','newPassword':'%s'}".formatted(temporary, NEW_PASSWORD))), token);
        return read(changed, "$.data.accessToken");
    }

    /** A staff profile without a login account */
    private String newStaffId(String admin) throws Exception {
        String created = mvc.perform(authed(post("/api/v1/admin/staff"), admin)
                        .content(json("{'name':'Thợ Ảnh Mới','phone':'%s','position':'Nhiếp ảnh'}".formatted(uniquePhone()))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(created, "$.data.id");
    }

    @Test
    void staffCannotManageAccounts() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/users"), staffToken())).andExpect(status().isForbidden());
        create(staffToken(), uniqueEmail(), "ROLE_STAFF", null).andExpect(status().isForbidden());
    }

    @Test
    void createdAccountGetsAOneTimeTemporaryPasswordAndMustChangeIt() throws Exception {
        String admin = adminToken();
        String staffId = newStaffId(admin);
        String email = uniqueEmail();
        String created = createdBody(email, "ROLE_STAFF", staffId);
        String temporary = read(created, "$.data.temporaryPassword");
        assertThat(temporary).hasSize(12).matches(".*\\d.*").matches(".*[A-Za-z].*");
        assertThat(read(created, "$.data.user.staffId")).isEqualTo(staffId);
        assertThat(read(created, "$.data.user.staffName")).isEqualTo("Thợ Ảnh Mới");

        String login = loginBody(email, temporary);
        assertThat(read(login, "$.data.user.mustChangePassword")).isEqualTo("true");
        String token = read(login, "$.data.accessToken");

        // Only me / change-password / logout work with a temporary password
        mvc.perform(authed(get("/api/v1/admin/bookings"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        mvc.perform(authed(get("/api/v1/admin/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));

        String changed = body(post("/api/v1/admin/auth/change-password")
                .content(json("{'currentPassword':'%s','newPassword':'%s'}".formatted(temporary, NEW_PASSWORD))), token);
        assertThat(read(changed, "$.data.user.mustChangePassword")).isEqualTo("false");
        mvc.perform(authed(get("/api/v1/admin/packages"), read(changed, "$.data.accessToken"))).andExpect(status().isOk());
    }

    @Test
    void listShowsConsoleAccountsWithFiltersAndLastLogin() throws Exception {
        String email = uniqueEmail();
        String id = read(createdBody(email, "ROLE_STAFF", null), "$.data.user.id");
        String admin = adminToken();

        String staffPage = body(get("/api/v1/admin/users").param("role", "ROLE_STAFF").param("size", "100"), admin);
        assertThat(first(staffPage, "$.data.items[?(@.id=='%s')].email".formatted(id))).isEqualTo(email);
        assertThat((java.util.List<String>) com.jayway.jsonpath.JsonPath.read(staffPage, "$.data.items[*].role")).containsOnly("ROLE_STAFF");
        assertThat(read(staffPage, "$.data.size")).isEqualTo("100");

        String adminPage = body(get("/api/v1/admin/users").param("role", "ROLE_ADMIN").param("active", "true"), admin);
        assertThat(first(adminPage, "$.data.items[?(@.email=='%s')].lastLoginAt".formatted(ADMIN_EMAIL))).isNotNull();
        assertThat(first(adminPage, "$.data.items[?(@.id=='%s')].id".formatted(id))).isNull();
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        create(adminToken(), ADMIN_EMAIL.toUpperCase(), "ROLE_ADMIN", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        create(adminToken(), "khong-phai-email", "ROLE_STAFF", null).andExpect(status().isBadRequest());
        create(adminToken(), uniqueEmail(), "ROLE_COUPLE", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
        create(adminToken(), uniqueEmail(), "ROLE_STAFF", UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    @Test
    void aStaffProfileHasAtMostOneAccount() throws Exception {
        String admin = adminToken();
        String staffId = newStaffId(admin);
        createdBody(uniqueEmail(), "ROLE_STAFF", staffId);
        create(admin, uniqueEmail(), "ROLE_STAFF", staffId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAFF_ALREADY_LINKED"));
    }

    @Test
    void lockingAnAccountEndsItsSessionsAndBlocksLogin() throws Exception {
        String email = uniqueEmail();
        String created = createdBody(email, "ROLE_STAFF", null);
        String id = read(created, "$.data.user.id");
        String token = activate(email, read(created, "$.data.temporaryPassword"));
        String refresh = read(loginBody(email, NEW_PASSWORD), "$.data.refreshToken");

        update(adminToken(), id, "Nhân Viên Nghỉ", "ROLE_STAFF", false, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.name").value("Nhân Viên Nghỉ"));

        mvc.perform(authed(get("/api/v1/admin/packages"), token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'refreshToken':'%s'}".formatted(refresh)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'%s'}".formatted(email, NEW_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void roleAndStaffLinkCanBeChangedAndUnlinked() throws Exception {
        String admin = adminToken();
        String staffId = newStaffId(admin);
        String id = read(createdBody(uniqueEmail(), "ROLE_STAFF", null), "$.data.user.id");

        update(admin, id, "Trưởng Nhóm", "ROLE_ADMIN", true, staffId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.staffId").value(staffId));
        update(admin, id, "Trưởng Nhóm", "ROLE_STAFF", true, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffId").doesNotExist());
        update(admin, UUID.randomUUID().toString(), "X", "ROLE_STAFF", true, null).andExpect(status().isNotFound());
    }

    @Test
    void mobileAccountsAreNotManagedHere() throws Exception {
        User couple = new User();
        couple.setEmail(uniqueEmail());
        couple.setPasswordHash(passwordEncoder.encode(NEW_PASSWORD));
        couple.setFullName("Cặp Đôi");
        couple.setRole(Role.ROLE_COUPLE);
        String id = users.save(couple).getId().toString();

        mvc.perform(authed(post("/api/v1/admin/users/" + id + "/reset-password"), adminToken())).andExpect(status().isNotFound());
    }

    @Test
    void anAdminCannotLockOrDemoteThemselves() throws Exception {
        String email = uniqueEmail();
        String created = createdBody(email, "ROLE_ADMIN", null);
        String id = read(created, "$.data.user.id");
        String self = activate(email, read(created, "$.data.temporaryPassword"));

        update(self, id, "Quản Lý", "ROLE_STAFF", true, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
        update(self, id, "Quản Lý", "ROLE_ADMIN", false, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
        // Renaming oneself is fine
        update(self, id, "Quản Lý Đổi Tên", "ROLE_ADMIN", true, null).andExpect(status().isOk());
    }

    @Test
    void resetPasswordIssuesANewTemporaryPasswordAndEndsSessions() throws Exception {
        String email = uniqueEmail();
        String created = createdBody(email, "ROLE_STAFF", null);
        String id = read(created, "$.data.user.id");
        String token = activate(email, read(created, "$.data.temporaryPassword"));
        String refresh = read(loginBody(email, NEW_PASSWORD), "$.data.refreshToken");

        String reset = mvc.perform(authed(post("/api/v1/admin/users/" + id + "/reset-password"), adminToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String temporary = read(reset, "$.data.temporaryPassword");

        mvc.perform(post("/api/v1/admin/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'refreshToken':'%s'}".formatted(refresh)))).andExpect(status().isUnauthorized());
        // The access token still decodes but the account now needs a password change
        mvc.perform(authed(get("/api/v1/admin/packages"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        assertThat(read(loginBody(email, temporary), "$.data.user.mustChangePassword")).isEqualTo("true");
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'%s'}".formatted(email, NEW_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }
}
