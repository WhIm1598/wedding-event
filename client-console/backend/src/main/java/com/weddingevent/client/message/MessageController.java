package com.weddingevent.client.message;

import com.weddingevent.client.message.MessageService.MessageDto;
import com.weddingevent.client.message.MessageService.SendMessageRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-MSG-01 (REST). The same operation is available over STOMP: {@code SEND /app/chat}. */
@RestController
@RequestMapping("/api/v1/messages")
@PreAuthorize(SecurityConfig.COUPLE_OR_VENDOR)
public class MessageController {

    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageDto> send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendMessageRequest req) {
        return ApiResponse.ok(messages.send(CurrentUser.id(jwt), req), "Đã gửi tin nhắn");
    }
}
