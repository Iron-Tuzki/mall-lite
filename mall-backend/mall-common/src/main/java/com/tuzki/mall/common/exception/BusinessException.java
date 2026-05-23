package com.tuzki.mall.common.exception;

import com.tuzki.mall.common.api.ResultCode;

/**
 * Business exception used to represent predictable domain errors.
 *
 * <p>Controllers and services can throw this exception when a request is valid at
 * the protocol level but cannot be completed because of business rules.</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
