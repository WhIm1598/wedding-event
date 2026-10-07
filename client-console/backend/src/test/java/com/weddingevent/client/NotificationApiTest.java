package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FN-NOTI-01 */
class NotificationApiTest extends ClientIntegrationTest {

    @Autowired
    NotificationRepository notifications;

    private Notification save(User recipient, String title, NotificationKind kind) {
        Notification n = new Notification();
        n.setRecipientUserId(recipient == null ? null : recipient.getId());
        n.setTitle(title);
        n.setDescription("Nội dung " + title);
        n.setKind(kind);
        Notification saved = notifications.save(n);
        try {
            Thread.sleep(5); // distinct created_at for a stable "newest first" order
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return saved;
    }

    @Test
    void inboxIsPaginatedNewestFirstAndPrivate() throws Exception {
        User me = createUser(null, true); // any signed-in user has an inbox, even before picking a role
        User other = createUser(null, true);
        save(me, "Một", NotificationKind.REMINDER);
        save(me, "Hai", null);
        Notification third = save(me, "Ba", NotificationKind.MESSAGE);
        Notification othersNote = save(other, "Của người khác", NotificationKind.AI);
        save(null, "Thông báo studio", null);
        String token = token(me);

        mvc.perform(authed(get("/api/v1/client/notifications?page=0&size=2"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(third.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("Ba"))
                .andExpect(jsonPath("$.data.items[0].body").value("Nội dung Ba"))
                .andExpect(jsonPath("$.data.items[0].kind").value("message"))
                .andExpect(jsonPath("$.data.items[0].isRead").value(false))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString())
                .andExpect(jsonPath("$.data.items[1].kind").value("reminder")); // no kind stored → generic icon
        mvc.perform(authed(get("/api/v1/client/notifications"), token))
                .andExpect(jsonPath("$.data.size").value(20));

        mvc.perform(authed(patch("/api/v1/client/notifications/" + third.getId() + "/read"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));
        mvc.perform(authed(patch("/api/v1/client/notifications/" + othersNote.getId() + "/read"), token))
                .andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/client/notifications/" + UUID.randomUUID() + "/read"), token))
                .andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/client/notifications?size=0"), token)).andExpect(status().isBadRequest());
    }
}
