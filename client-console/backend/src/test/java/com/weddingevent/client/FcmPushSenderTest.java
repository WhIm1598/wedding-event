package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.weddingevent.client.notification.FcmPushSender;
import com.weddingevent.client.notification.PushSender.PushMessage;
import com.weddingevent.common.domain.DeviceToken;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.DevicePlatform;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.DeviceTokenRepository;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

/** FN-NOTI-02: FCM HTTP v1 against a fake Google (OAuth token endpoint + messages:send) */
class FcmPushSenderTest extends ClientIntegrationTest {

    @Autowired
    DeviceTokenRepository devices;

    @TempDir
    Path dir;

    private HttpServer google;
    private final List<String> tokenRequests = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> sent = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private FcmPushSender fcm;

    @BeforeEach
    void startFakeGoogle() throws Exception {
        google = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        google.createContext("/token", ex -> {
            tokenRequests.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"access_token\":\"ya29.test\",\"expires_in\":3600}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        google.createContext("/v1/projects/wedplanner-test/messages:send", ex -> {
            authHeaders.add(ex.getRequestHeaders().getFirst("Authorization"));
            Map<String, Object> request;
            try {
                request = JSONObjectUtils.parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            } catch (java.text.ParseException e) {
                throw new java.io.IOException(e);
            }
            sent.add(request);
            @SuppressWarnings("unchecked")
            String token = (String) ((Map<String, Object>) request.get("message")).get("token");
            boolean dead = token.startsWith("dead");
            // A wrong project id: FCM says 404 without naming the token
            boolean misconfigured = token.startsWith("misconfigured");
            byte[] body = (dead ? "{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"
                    : misconfigured ? "{\"error\":{\"code\":404,\"message\":\"Requested entity was not found.\",\"status\":\"NOT_FOUND\"}}"
                    : "{\"name\":\"ok\"}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(dead || misconfigured ? 404 : 200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        google.start();
        String base = "http://127.0.0.1:" + google.getAddress().getPort();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keys = gen.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(keys.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Path credentials = dir.resolve("service-account.json");
        Files.writeString(credentials, JSONObjectUtils.toJSONString(Map.of(
                "type", "service_account", "client_email", "push@wedplanner-test.iam.gserviceaccount.com",
                "private_key", pem, "token_uri", base + "/token")));
        // Synchronous executor: the test sees the deliveries as soon as send() returns
        fcm = new FcmPushSender("wedplanner-test", credentials, base, devices, Clock.systemUTC(), Runnable::run);
    }

    @AfterEach
    void stopFakeGoogle() {
        google.stop(0);
    }

    private void device(User user, String token) {
        DeviceToken d = new DeviceToken();
        d.setUser(user);
        d.setToken(token);
        d.setPlatform(DevicePlatform.ANDROID);
        devices.save(d);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsToEveryDeviceWithOneCachedAccessTokenAndDropsDeadTokens() throws Exception {
        User user = createUser(Role.ROLE_COUPLE, true);
        String live = "live-" + UUID.randomUUID();
        String dead = "dead-" + UUID.randomUUID();
        device(user, live);
        device(user, dead);

        fcm.send(user.getId(), new PushMessage("Đã có báo giá", "Dreamy Decor đã báo giá", Map.of("type", "notification", "kind", "message")));
        fcm.send(user.getId(), PushMessage.dataOnly(Map.of("type", "plan_updated", "planId", "p-1")));

        // One OAuth exchange (JWT bearer grant signed by the service account), reused for every message
        assertThat(tokenRequests).hasSize(1);
        String assertion = URLDecoder.decode(tokenRequests.get(0), StandardCharsets.UTF_8).replaceAll(".*assertion=", "");
        assertThat(SignedJWT.parse(assertion).getJWTClaimsSet().getStringClaim("scope"))
                .isEqualTo("https://www.googleapis.com/auth/firebase.messaging");
        assertThat(authHeaders).allMatch("Bearer ya29.test"::equals);

        Map<String, Object> shown = (Map<String, Object>) sent.get(0).get("message");
        assertThat((Map<String, Object>) shown.get("notification")).containsEntry("title", "Đã có báo giá");
        assertThat((Map<String, Object>) shown.get("data")).containsEntry("kind", "message");

        // The dead token was dropped after the first message, so the data message only went to the live one
        assertThat(devices.findByToken(dead)).isEmpty();
        Map<String, Object> silent = (Map<String, Object>) sent.get(sent.size() - 1).get("message");
        assertThat(silent).doesNotContainKey("notification").containsKey("apns").containsEntry("token", live);
        assertThat((Map<String, Object>) silent.get("data")).containsEntry("type", "plan_updated");
        assertThat(sent).hasSize(3);
    }

    /** Only a token FCM names as dead is dropped: a 404 for another reason (wrong project id) must not wipe devices */
    @Test
    void aFailureThatIsNotAboutTheTokenKeepsTheDevice() {
        User user = createUser(Role.ROLE_COUPLE, true);
        String token = "misconfigured-" + UUID.randomUUID();
        device(user, token);
        fcm.send(user.getId(), PushMessage.dataOnly(Map.of("type", "plan_updated")));
        assertThat(sent).hasSize(1);
        assertThat(devices.findByToken(token)).isPresent();
    }

    @Test
    void aUserWithoutDevicesSendsNothing() {
        fcm.send(createUser(Role.ROLE_COUPLE, true).getId(), PushMessage.dataOnly(Map.of("type", "plan_updated")));
        assertThat(sent).isEmpty();
        assertThat(tokenRequests).isEmpty();
    }
}
