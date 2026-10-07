package com.weddingevent.admin;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.NotificationRepository;
import com.weddingevent.common.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** The admin and client backends share one database; mobile data must not leak into the admin console. */
class SharedDatabaseApiTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    PasswordEncoder passwordEncoder;

    private User mobileUser(Role role) {
        User u = new User();
        u.setEmail("mobile-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash(passwordEncoder.encode("Password123@"));
        u.setFullName("Khách Mobile");
        u.setRole(role);
        u.setVerified(true);
        return users.save(u);
    }

    @Test
    void mobileAccountsCannotSignInToTheAdminConsole() throws Exception {
        for (Role role : new Role[] {null, Role.ROLE_COUPLE, Role.ROLE_VENDOR}) {
            User u = mobileUser(role);
            mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'email':'" + u.getEmail() + "','password':'Password123@'}")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void mobileInboxNotificationsStayOutOfTheStudioFeed() throws Exception {
        User couple = mobileUser(Role.ROLE_COUPLE);
        Notification n = new Notification();
        n.setTitle("Riêng tư " + UUID.randomUUID());
        n.setDescription("Thông báo trong app của cặp đôi");
        n.setRecipientUserId(couple.getId());
        n.setKind(NotificationKind.SYNC);
        notifications.save(n);

        String admin = adminToken();
        mvc.perform(authed(get("/api/v1/admin/notifications"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", not(hasItem(n.getTitle()))));
        mvc.perform(authed(patch("/api/v1/admin/notifications/" + n.getId() + "/read"), admin))
                .andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/admin/notifications/read-all"), admin)).andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(notifications.findById(n.getId()).orElseThrow().isRead()).isFalse();
    }
}
