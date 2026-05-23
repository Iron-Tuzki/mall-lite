package com.tuzki.mall.common.api;

/**
 * Standard REST response wrapper for mall-lite APIs.
 *
 * @param <T> response payload type
 */
public final class Result<T> {

    private final int code;

    private final String message;

    private final T data;

    private final boolean success;

    private Result(int code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null, true);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, true);
    }

    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null, false);
    }

    public static Result<Void> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, false);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }
}
