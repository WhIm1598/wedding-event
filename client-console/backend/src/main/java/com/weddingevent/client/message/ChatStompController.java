package com.weddingevent.client.message;

import com.weddingevent.client.message.MessageService.SendMessageRequest;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.exception.AppException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * FN-MSG-01 over STOMP: {@code SEND /app/chat} with the same body as {@code POST /api/v1/messages/send}. The message
 * reaches the room topic; errors go back to the sender on {@code /user/queue/errors} in the API envelope.
 */
@Controller
public class ChatStompController {

    private static final Logger log = LoggerFactory.getLogger(ChatStompController.class);

    private final MessageService messages;
    private final Validator validator;

    public ChatStompController(MessageService messages, Validator validator) {
        this.messages = messages;
        this.validator = validator;
    }

    @MessageMapping("/chat")
    public void chat(@Payload SendMessageRequest req, Principal principal) {
        Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            throw new InvalidStompPayload(violations.stream()
                    .map(v -> new ApiResponse.FieldError(v.getPropertyPath().toString(), v.getMessage())).toList());
        }
        messages.send(UUID.fromString(principal.getName()), req);
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ApiResponse<Void> handleError(Exception ex) {
        if (ex instanceof AppException app) {
            return ApiResponse.error(app.getCode(), app.getMessage(), app.getErrors());
        }
        if (ex instanceof InvalidStompPayload invalid) {
            return ApiResponse.error("BAD_REQUEST", "Dữ liệu không hợp lệ", invalid.errors);
        }
        log.error("STOMP message failed", ex);
        return ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã có lỗi hệ thống, vui lòng thử lại sau");
    }

    static final class InvalidStompPayload extends RuntimeException {
        final transient List<ApiResponse.FieldError> errors;

        InvalidStompPayload(List<ApiResponse.FieldError> errors) {
            super("Invalid payload");
            this.errors = errors;
        }
    }
}
