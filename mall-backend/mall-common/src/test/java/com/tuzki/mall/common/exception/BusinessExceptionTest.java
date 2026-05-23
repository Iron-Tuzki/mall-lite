package com.tuzki.mall.common.exception;

import com.tuzki.mall.common.api.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessExceptionTest {

    @Test
    void createBusinessExceptionWithResultCode() {
        BusinessException exception = new BusinessException(ResultCode.NOT_FOUND);

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        assertEquals(ResultCode.NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void createBusinessExceptionWithCustomCodeAndMessage() {
        BusinessException exception = new BusinessException(10001, "sku stock is not enough");

        assertEquals(10001, exception.getCode());
        assertEquals("sku stock is not enough", exception.getMessage());
    }
}
