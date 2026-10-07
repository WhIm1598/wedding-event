package com.weddingevent.client.message;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Who has an open STOMP session (FN-MSG-01: push only when the recipient is not online). In-memory, so it is exact for
 * a single instance; with several instances use a shared store (e.g. Redis) or a broker relay instead.
 */
@Component
public class PresenceTracker {

    private final Map<String, UUID> userBySession = new ConcurrentHashMap<>();

    void connected(String sessionId, UUID userId) {
        userBySession.put(sessionId, userId);
    }

    @EventListener
    void onDisconnect(SessionDisconnectEvent event) {
        userBySession.remove(event.getSessionId());
    }

    public boolean isOnline(UUID userId) {
        return userBySession.containsValue(userId);
    }
}
