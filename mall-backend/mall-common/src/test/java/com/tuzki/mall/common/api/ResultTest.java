package com.tuzki.mall.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void successWithoutDataUsesSuccessCodeAndMessage() {
        Result<Void> result = Result.success();

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void successWithDataKeepsPayload() {
        Result<String> result = Result.success("created");

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertTrue(result.isSuccess());
        assertEquals("created", result.getData());
    }

    @Test
    void failWithCodeAndMessageMarksResponseAsFailed() {
        Result<Void> result = Result.fail(400, "sku id is required");

        assertEquals(400, result.getCode());
        assertEquals("sku id is required", result.getMessage());
        assertFalse(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void failWithResultCodeUsesEnumCodeAndMessage() {
        Result<Void> result = Result.fail(ResultCode.NOT_FOUND);

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals(ResultCode.NOT_FOUND.getMessage(), result.getMessage());
        assertFalse(result.isSuccess());
        assertNull(result.getData());
    }
}
