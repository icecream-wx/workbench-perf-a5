package com.example.perfa5.exception;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }

    /**
     * 业务码前三位即 HTTP 状态（如 40401 → 404、40901 → 409），其余按 400 处理。
     */
    public int getHttpStatus() {
        int candidate = code / 100;
        return candidate >= 400 && candidate <= 599 ? candidate : 400;
    }
}
