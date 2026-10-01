package com.weddingevent.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Standard response envelope (detailed spec §1.2):
 * {@code { "success": true, "code": "SUCCESS", "message": "...", "data": ... }}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String code, String message, T data, List<?> errors) {

    public static final String SUCCESS = "SUCCESS";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, SUCCESS, "Thao tác thành công", data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, SUCCESS, message, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, List<?> errors) {
        return new ApiResponse<>(false, code, message, null, errors);
    }

    /** Field-level validation error item */
    public record FieldError(String field, String message) {}
}
