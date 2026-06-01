package com.tuzki.mall.product;

import com.tuzki.mall.user.service.LoginSessionService;
import com.tuzki.mall.product.service.ProductFootprintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品浏览足迹接口集成测试，验证 Redis ZSet 足迹的写入、查询和清理行为。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class ProductFootprintApiIntegrationTest {

    private static final Long USER_ID = 1L;

    private static final Long FIRST_PRODUCT_ID = 910037L;

    private static final Long SECOND_PRODUCT_ID = 910024L;

    private static final String FOOTPRINT_KEY = "mall:user:footprints:" + USER_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private ProductFootprintService productFootprintService;

    @Autowired
    private RedissonClient redissonClient;

    private String token;

    @AfterEach
    void clearFootprints() {
        redissonClient.getScoredSortedSet(FOOTPRINT_KEY).delete();
        if (token != null) {
            loginSessionService.deleteSession(token);
        }
    }

    @Test
    void loggedInUserCanQueryDeleteAndClearRecentProductFootprints() throws Exception {
        token = loginSessionService.createSession(USER_ID);

        browseProduct(FIRST_PRODUCT_ID);
        browseProduct(SECOND_PRODUCT_ID);
        browseProduct(FIRST_PRODUCT_ID);

        mockMvc.perform(get("/api/product-footprints")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productId").value(FIRST_PRODUCT_ID))
                .andExpect(jsonPath("$.data[1].productId").value(SECOND_PRODUCT_ID));

        mockMvc.perform(delete("/api/product-footprints/{productId}", FIRST_PRODUCT_ID)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/product-footprints")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(SECOND_PRODUCT_ID));

        mockMvc.perform(delete("/api/product-footprints")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/product-footprints")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void anonymousProductDetailQueryDoesNotCreateFootprint() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", FIRST_PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products/{productId}", FIRST_PRODUCT_ID)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        org.assertj.core.api.Assertions.assertThat(footprints().size()).isZero();

        mockMvc.perform(get("/api/product-footprints")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void footprintSetOnlyKeepsLatestOneHundredProducts() throws Exception {
        token = loginSessionService.createSession(USER_ID);
        RScoredSortedSet<String> footprints = footprints();
        for (long productId = 1; productId <= 101; productId++) {
            productFootprintService.record(USER_ID, productId);
            Thread.sleep(2);
        }

        org.assertj.core.api.Assertions.assertThat(footprints.size()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThat(footprints.contains("1")).isFalse();
        org.assertj.core.api.Assertions.assertThat(footprints.contains("101")).isTrue();
    }

    private void browseProduct(Long productId) throws Exception {
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        Thread.sleep(5);
    }

    private String bearerToken() {
        return "Bearer " + token;
    }

    private RScoredSortedSet<String> footprints() {
        return redissonClient.getScoredSortedSet(FOOTPRINT_KEY, StringCodec.INSTANCE);
    }
}
