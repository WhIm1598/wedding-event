package com.weddingevent.client.message;

import com.weddingevent.client.security.UserAuthenticationConverter;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.repository.ChatRoomRepository;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Secures STOMP frames (the HTTP handshake on /ws is open):
 * <ul>
 *   <li>CONNECT needs {@code Authorization: Bearer <access token>} of an active couple/vendor;</li>
 *   <li>SUBSCRIBE only to {@code /topic/messages/{roomId}} of a room the user belongs to, or own {@code /user/queue/**};</li>
 *   <li>SEND only to application destinations ({@code /app/**}), never straight to the broker.</li>
 * </ul>
 * A rejected frame ends the session with a STOMP ERROR frame.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder accessTokenDecoder;
    private final UserAuthenticationConverter userLookup;
    private final ChatRoomRepository rooms;
    private final PresenceTracker presence;

    public StompAuthInterceptor(JwtDecoder accessTokenDecoder, UserAuthenticationConverter userLookup, ChatRoomRepository rooms,
            PresenceTracker presence) {
        this.accessTokenDecoder = accessTokenDecoder;
        this.userLookup = userLookup;
        this.rooms = rooms;
        this.presence = presence;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            User user = authenticate(accessor.getFirstNativeHeader("Authorization"));
            accessor.setUser(new UsernamePasswordAuthenticationToken(user.getId().toString(), null, UserAuthenticationConverter.authorities(user)));
            presence.connected(accessor.getSessionId(), user.getId());
        } else if (command == StompCommand.SUBSCRIBE) {
            checkSubscription(requireUser(accessor), accessor.getDestination());
        } else if (command == StompCommand.SEND) {
            requireUser(accessor);
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/app/")) {
                throw denied();
            }
        }
        return message;
    }

    private User authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw denied();
        }
        try {
            UUID userId = UUID.fromString(accessTokenDecoder.decode(header.substring(7)).getSubject());
            return userLookup.activeMobileUser(userId).filter(u -> u.getRole() != null).orElseThrow(StompAuthInterceptor::denied);
        } catch (JwtException | IllegalArgumentException e) {
            throw denied();
        }
    }

    private void checkSubscription(UUID userId, String destination) {
        if (destination == null) {
            throw denied();
        }
        if (destination.startsWith("/user/queue/")) {
            return;
        }
        if (destination.startsWith(MessageService.ROOM_TOPIC)) {
            try {
                UUID roomId = UUID.fromString(destination.substring(MessageService.ROOM_TOPIC.length()));
                if (rooms.findById(roomId).map(r -> r.hasParticipant(userId)).orElse(false)) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                throw denied();
            }
        }
        throw denied();
    }

    private static UUID requireUser(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw denied();
        }
        return UUID.fromString(user.getName());
    }

    private static MessagingException denied() {
        return new MessagingException("Access denied");
    }
}
