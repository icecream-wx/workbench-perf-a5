package com.example.perfa5.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fieldErrors) {

    public ApiResponse(int code, String message, T data) {
        this(code, message, data, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, Map<String, String> fieldErrors) {
        return new ApiResponse<>(code, message, null, fieldErrors);
    }
}
