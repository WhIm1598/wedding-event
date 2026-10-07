package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.ChatMessageRepository;
import com.weddingevent.common.repository.NotificationRepository;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** FN-MSG-01 over REST and STOMP (/ws, /app/chat, /topic/messages/{roomId}) */
class MessageApiTest extends ClientIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    ChatMessageRepository messages;

    // ---------------------------------------------------------------- REST

    private String send(String token, User recipient, String content) throws Exception {
        return body(mvc.perform(authed(post("/api/v1/messages/send"), token)
                        .content(json("{'recipientId':'" + recipient.getId() + "','content':'" + content + "','attachmentUrl':null}")))
                .andExpect(status().isCreated()));
    }

    @Test
    void coupleMessagesAVendorWhoGetsAnInboxNotificationWhileOffline() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        User vendor = createUser(Role.ROLE_VENDOR, true);
        String coupleToken = token(couple);

        String first = send(coupleToken, vendor, "Gói Pre-wedding còn lịch cuối tuần tháng 10 không ạ?");
        assertThat(read(first, "$.data.senderId")).isEqualTo(couple.getId().toString());
        assertThat(read(first, "$.data.recipientId")).isEqualTo(vendor.getId().toString());
        assertThat(read(first, "$.data.content")).isEqualTo("Gói Pre-wedding còn lịch cuối tuần tháng 10 không ạ?");
        assertThat(read(first, "$.data.createdAt")).isNotBlank();
        String roomId = read(first, "$.data.roomId");

        // The reply lands in the same room
        String reply = send(token(vendor), couple, "Dạ còn ạ");
        assertThat(read(reply, "$.data.roomId")).isEqualTo(roomId);
        assertThat(messages.countByRoomId(UUID.fromString(roomId))).isEqualTo(2);

        var inbox = notifications.findAllByRecipientUserIdOrderByCreatedAtDesc(vendor.getId(), PageRequest.of(0, 5)).getContent();
        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().getKind()).isEqualTo(NotificationKind.MESSAGE);
        assertThat(inbox.getFirst().getTitle()).isEqualTo(couple.getFullName());
        assertThat(pushSender.sentTo(vendor.getId())).hasSize(1);
    }

    @Test
    void recipientMustBeACounterpart() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        String token = token(couple);
        for (User recipient : new User[] {couple, createUser(Role.ROLE_COUPLE, true), createUser(null, true), createUser(Role.ROLE_STAFF, true)}) {
            mvc.perform(authed(post("/api/v1/messages/send"), token)
                            .content(json("{'recipientId':'" + recipient.getId() + "','content':'Xin chào'}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_RECIPIENT"));
        }
        mvc.perform(authed(post("/api/v1/messages/send"), token)
                        .content(json("{'recipientId':'" + UUID.randomUUID() + "','content':'Xin chào'}")))
                .andExpect(jsonPath("$.code").value("INVALID_RECIPIENT"));

        User locked = createUser(Role.ROLE_VENDOR, true);
        locked.setActive(false);
        users.save(locked);
        mvc.perform(authed(post("/api/v1/messages/send"), token)
                        .content(json("{'recipientId':'" + locked.getId() + "','content':'Xin chào'}")))
                .andExpect(jsonPath("$.code").value("INVALID_RECIPIENT"));
    }

    @Test
    void messageValidation() throws Exception {
        String token = coupleToken();
        mvc.perform(authed(post("/api/v1/messages/send"), token)
                        .content(json("{'content':' ','attachmentUrl':'" + "x".repeat(501) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(authed(post("/api/v1/messages/send"), token)
                        .content(json("{'recipientId':'" + UUID.randomUUID() + "','content':'" + "x".repeat(2001) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    void notificationsListShowsMessageKind() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        User vendor = createUser(Role.ROLE_VENDOR, true);
        send(token(couple), vendor, "Chào studio");
        mvc.perform(authed(get("/api/v1/client/notifications"), token(vendor)))
                .andExpect(jsonPath("$.data.items[0].kind").value("message"))
                .andExpect(jsonPath("$.data.items[0].body").value("Chào studio"));
    }

    // ---------------------------------------------------------------- STOMP

    /** Sends and receives raw JSON strings */
    static final class JsonStringConverter extends AbstractMessageConverter {
        JsonStringConverter() {
            super(MimeTypeUtils.APPLICATION_JSON);
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return String.class == clazz;
        }

        @Override
        protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            Object payload = message.getPayload();
            return payload instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : payload.toString();
        }

        @Override
        protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            return ((String) payload).getBytes(StandardCharsets.UTF_8);
        }
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JsonStringConverter());
        StompHeaders connect = new StompHeaders();
        if (token != null) {
            connect.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connect, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private static BlockingQueue<String> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((String) payload);
            }
        });
        Thread.sleep(300); // let the broker register the subscription
        return queue;
    }

    private static void stompSend(StompSession session, String json) {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/chat");
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(headers, json);
    }

    @Test
    void participantsReceiveMessagesInRealTimeAndOnlineUsersGetNoInboxEntry() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        User vendor = createUser(Role.ROLE_VENDOR, true);
        String coupleToken = token(couple);
        String roomId = read(send(coupleToken, vendor, "Tin đầu tiên"), "$.data.roomId");
        long inboxBefore = notifications.findAllByRecipientUserIdOrderByCreatedAtDesc(vendor.getId(), PageRequest.of(0, 50)).getTotalElements();

        StompSession vendorSession = connect(token(vendor));
        BlockingQueue<String> vendorRoom = subscribe(vendorSession, "/topic/messages/" + roomId);
        StompSession coupleSession = connect(coupleToken);
        BlockingQueue<String> coupleErrors = subscribe(coupleSession, "/user/queue/errors");
        try {
            // REST message is pushed to the room
            send(coupleToken, vendor, "Qua REST");
            String viaRest = vendorRoom.poll(5, TimeUnit.SECONDS);
            assertThat(viaRest).isNotNull();
            assertThat(read(viaRest, "$.content")).isEqualTo("Qua REST");
            assertThat(read(viaRest, "$.roomId")).isEqualTo(roomId);

            // STOMP /app/chat works the same way
            stompSend(coupleSession, json("{'recipientId':'" + vendor.getId() + "','content':'Qua STOMP'}"));
            String viaStomp = vendorRoom.poll(5, TimeUnit.SECONDS);
            assertThat(viaStomp).isNotNull();
            assertThat(read(viaStomp, "$.content")).isEqualTo("Qua STOMP");
            assertThat(read(viaStomp, "$.senderId")).isEqualTo(couple.getId().toString());

            // Errors come back on the sender's error queue in the API envelope
            stompSend(coupleSession, json("{'recipientId':'" + vendor.getId() + "','content':''}"));
            String error = coupleErrors.poll(5, TimeUnit.SECONDS);
            assertThat(error).isNotNull();
            assertThat(read(error, "$.code")).isEqualTo("BAD_REQUEST");
            stompSend(coupleSession, json("{'recipientId':'" + couple.getId() + "','content':'Tự nhắn'}"));
            assertThat(read(coupleErrors.poll(5, TimeUnit.SECONDS), "$.code")).isEqualTo("INVALID_RECIPIENT");

            // The vendor was online: no new inbox notification
            long inboxAfter = notifications.findAllByRecipientUserIdOrderByCreatedAtDesc(vendor.getId(), PageRequest.of(0, 50)).getTotalElements();
            assertThat(inboxAfter).isEqualTo(inboxBefore);
        } finally {
            vendorSession.disconnect();
            coupleSession.disconnect();
        }
    }

    @Test
    void outsidersCannotListenToARoom() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        User vendor = createUser(Role.ROLE_VENDOR, true);
        String coupleToken = token(couple);
        String roomId = read(send(coupleToken, vendor, "Riêng tư"), "$.data.roomId");

        StompSession outsider = connect(token(createUser(Role.ROLE_VENDOR, true)));
        BlockingQueue<String> spy = subscribe(outsider, "/topic/messages/" + roomId);
        send(coupleToken, vendor, "Bí mật");
        assertThat(spy.poll(1, TimeUnit.SECONDS)).isNull();
        if (outsider.isConnected()) {
            outsider.disconnect();
        }
    }

    @Test
    void connectingRequiresAValidToken() {
        assertThatThrownBy(() -> connect(null)).isInstanceOfAny(ExecutionException.class, TimeoutException.class);
        assertThatThrownBy(() -> connect("garbage")).isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    @Test
    void rolelessUsersCannotConnect() throws Exception {
        String token = token(createUser(null, true));
        assertThatThrownBy(() -> connect(token)).isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }
}
