package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.DeviceTokenRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FN-NOTI-02: registering the devices that receive pushes */
class DeviceApiTest extends ClientIntegrationTest {

    @Autowired
    DeviceTokenRepository devices;

    private void register(String token, String fcmToken) throws Exception {
        mvc.perform(authed(put("/api/v1/users/me/devices"), token).content(json("{'token':'" + fcmToken + "','platform':'ANDROID'}")))
                .andExpect(status().isOk());
    }

    @Test
    void aDeviceBelongsToTheLastAccountThatRegisteredIt() throws Exception {
        User first = createUser(Role.ROLE_COUPLE, true);
        User second = createUser(null, true); // no role yet: may still register
        String fcm = "fcm-" + UUID.randomUUID();

        register(token(first), fcm);
        register(token(first), fcm); // idempotent
        assertThat(devices.findAllByUserIdOrderByUpdatedAtAsc(first.getId())).hasSize(1);

        register(token(second), fcm);
        assertThat(devices.findAllByUserIdOrderByUpdatedAtAsc(first.getId())).isEmpty();
        assertThat(devices.findByToken(fcm)).get().satisfies(d -> assertThat(d.getUser().getId()).isEqualTo(second.getId()));
    }

    @Test
    void aUserKeepsTheTenNewestDevices() throws Exception {
        User user = createUser(Role.ROLE_VENDOR, true);
        String token = token(user);
        String oldest = "fcm-old-" + UUID.randomUUID();
        register(token, oldest);
        for (int i = 0; i < 10; i++) {
            register(token, "fcm-" + i + "-" + UUID.randomUUID());
        }
        assertThat(devices.findAllByUserIdOrderByUpdatedAtAsc(user.getId())).hasSize(10);
        assertThat(devices.findByToken(oldest)).isEmpty();
    }

    @Test
    void unregisteringOnlyTouchesTheCallersOwnDevice() throws Exception {
        String owner = coupleToken();
        String fcm = "fcm-" + UUID.randomUUID();
        register(owner, fcm);

        mvc.perform(authed(delete("/api/v1/users/me/devices").param("token", fcm), coupleToken())).andExpect(status().isOk());
        assertThat(devices.findByToken(fcm)).isPresent();

        mvc.perform(authed(delete("/api/v1/users/me/devices").param("token", fcm), owner)).andExpect(status().isOk());
        assertThat(devices.findByToken(fcm)).isEmpty();
    }

    @Test
    void registrationValidation() throws Exception {
        String token = coupleToken();
        mvc.perform(authed(put("/api/v1/users/me/devices"), token).content(json("{'token':'','platform':'ANDROID'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("token"));
        mvc.perform(authed(put("/api/v1/users/me/devices"), token).content(json("{'token':'abc'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("platform"));
        mvc.perform(authed(put("/api/v1/users/me/devices"), token).content(json("{'token':'abc','platform':'WINDOWS'}")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me/devices").contentType("application/json").content(json("{'token':'abc','platform':'IOS'}")))
                .andExpect(status().isUnauthorized());
    }
}
