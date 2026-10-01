package com.weddingevent.common.exception;

import org.springframework.http.HttpStatus;

/** Business error carrying an HTTP status and a spec error code (e.g. 409 STAFF_SCHEDULE_CONFLICT). */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static AppException badRequest(String code, String message) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AppException conflict(String code, String message) {
        return new AppException(HttpStatus.CONFLICT, code, message);
    }

    public static AppException unauthorized(String code, String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static AppException forbidden(String code, String message) {
        return new AppException(HttpStatus.FORBIDDEN, code, message);
    }
}
