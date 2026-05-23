package com.tuzki.mall.validation;

import com.tuzki.mall.common.web.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GlobalExceptionHandler.class, ValidationExceptionHandlingTest.ValidationTestController.class})
class ValidationExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidRequestBodyReturnsStandardBadRequestResult() throws Exception {
        mockMvc.perform(post("/test/validation/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":1001,\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("quantity must be greater than or equal to 1"));
    }

    @RestController
    @RequestMapping("/test/validation/orders")
    static class ValidationTestController {

        @PostMapping
        void createOrder(@Valid @RequestBody CreateOrderRequest request) {
        }
    }

    static class CreateOrderRequest {

        @NotNull
        private Long userId;

        @NotNull
        private Long skuId;

        @Min(1)
        private Integer quantity;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getSkuId() {
            return skuId;
        }

        public void setSkuId(Long skuId) {
            this.skuId = skuId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
