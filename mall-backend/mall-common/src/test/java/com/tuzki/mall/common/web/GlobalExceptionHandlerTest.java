package com.tuzki.mall.common.web;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.api.ResultCode;
import com.tuzki.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionUsesExceptionCodeAndMessage() {
        Result<Void> result = handler.handleBusinessException(new BusinessException(10001, "sku stock is not enough"));

        assertEquals(10001, result.getCode());
        assertEquals("sku stock is not enough", result.getMessage());
        assertFalse(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequest() {
        Result<Void> result = handler.handleIllegalArgumentException(new IllegalArgumentException("sku id is required"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("sku id is required", result.getMessage());
        assertFalse(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void handleUnknownExceptionReturnsInternalError() {
        Result<Void> result = handler.handleException(new RuntimeException("database connection failed"));

        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals(ResultCode.INTERNAL_ERROR.getMessage(), result.getMessage());
        assertFalse(result.isSuccess());
        assertNull(result.getData());
    }
}
