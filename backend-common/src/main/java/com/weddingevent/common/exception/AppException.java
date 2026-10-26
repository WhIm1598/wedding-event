package com.weddingevent.common.exception;

import com.weddingevent.common.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Business error carrying an HTTP status and a spec error code (e.g. 409 STAFF_SCHEDULE_CONFLICT). */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ApiResponse.FieldError> errors;
    private final Object data;

    public AppException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AppException(HttpStatus status, String code, String message, List<ApiResponse.FieldError> errors) {
        this(status, code, message, errors, null);
    }

    private AppException(HttpStatus status, String code, String message, List<ApiResponse.FieldError> errors, Object data) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
        this.data = data;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    /** Field errors for validation rules that annotations can't express (e.g. "at least 7 days from today") */
    public List<ApiResponse.FieldError> getErrors() {
        return errors;
    }

    /** Extra payload for the client, sent as the envelope's {@code data} (e.g. {@code existingId}) */
    public Object getData() {
        return data;
    }

    public static AppException badRequest(String code, String message) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message);
    }

    /** 400 BAD_REQUEST with one field error, same shape as a Jakarta Validation failure */
    public static AppException invalidField(String field, String message) {
        return new AppException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Dữ liệu không hợp lệ",
                List.of(new ApiResponse.FieldError(field, message)));
    }

    public static AppException conflict(String code, String message) {
        return new AppException(HttpStatus.CONFLICT, code, message);
    }

    /** 409 that points the client at the existing row, e.g. {@code data: {existingId}} */
    public static AppException conflict(String code, String message, Object data) {
        return new AppException(HttpStatus.CONFLICT, code, message, null, data);
    }

    public static AppException unauthorized(String code, String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static AppException forbidden(String code, String message) {
        return new AppException(HttpStatus.FORBIDDEN, code, message);
    }
}
