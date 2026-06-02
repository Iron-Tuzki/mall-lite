package com.tuzki.mall.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.cart.entity.CartItem;
import com.tuzki.mall.cart.mapper.CartItemMapper;
import com.tuzki.mall.cart.message.CartChangeMessageSender;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 购物车接口集成测试，验证登录用户的加购、查询、修改、删除和批量清理流程。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class CartApiIntegrationTest {

    private static final String CART_KEY = "mall:cart:user:" + TestSeedData.USER_ID;

    private static final String LOADED_KEY = "mall:cart:loaded:" + TestSeedData.USER_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private RedissonClient redissonClient;

    @MockitoBean
    private CartChangeMessageSender cartChangeMessageSender;

    private String token;

    @BeforeEach
    void setUp() {
        token = loginSessionService.createSession(TestSeedData.USER_ID);
        doAnswer(invocation -> null).when(cartChangeMessageSender).send(any());
    }

    @AfterEach
    void clearCart() {
        redissonClient.getKeys().delete(CART_KEY, LOADED_KEY);
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, TestSeedData.USER_ID));
        loginSessionService.deleteSession(token);
    }

    @Test
    void loggedInUserCanManageCartItems() throws Exception {
        add(TestSeedData.SKU_ID, 2);
        add(TestSeedData.SKU_ID, 3);

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skuId").value(TestSeedData.SKU_ID))
                .andExpect(jsonPath("$.data[0].quantity").value(5))
                .andExpect(jsonPath("$.data[0].available").value(true));

        mockMvc.perform(put("/api/cart/items/{skuId}", TestSeedData.SKU_ID)
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/cart/items/{skuId}", TestSeedData.SKU_ID)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void loggedInUserCanBatchDeleteCartItems() throws Exception {
        add(TestSeedData.SKU_ID, 1);
        add(TestSeedData.SKU_ID_2, 1);

        mockMvc.perform(delete("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuIds\":[" + TestSeedData.SKU_ID + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skuId").value(TestSeedData.SKU_ID_2));
    }

    @Test
    void batchDeleteIgnoresItemsThatWereAlreadyRemoved() throws Exception {
        add(TestSeedData.SKU_ID, 1);
        add(TestSeedData.SKU_ID_2, 1);
        mockMvc.perform(delete("/api/cart/items/{skuId}", TestSeedData.SKU_ID)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuIds\":[" + TestSeedData.SKU_ID + "," + TestSeedData.SKU_ID_2 + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void cartRejectsMissingTokenAndInvalidQuantity() throws Exception {
        mockMvc.perform(get("/api/cart/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuId\":" + TestSeedData.SKU_ID + ",\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        add(TestSeedData.SKU_ID, 99);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuId\":" + TestSeedData.SKU_ID + ",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void failedMessageSendRollsBackRedisMutation() throws Exception {
        doThrow(new IllegalStateException("rabbit unavailable")).when(cartChangeMessageSender).send(any());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuId\":" + TestSeedData.SKU_ID + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(503));

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void missingRedisCacheIsRebuiltFromMysql() throws Exception {
        CartItem item = new CartItem();
        item.setUserId(TestSeedData.USER_ID);
        item.setSkuId(TestSeedData.SKU_ID);
        item.setQuantity(6);
        item.setVersion(3L);
        item.setDeleted(0);
        cartItemMapper.insert(item);

        mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skuId").value(TestSeedData.SKU_ID))
                .andExpect(jsonPath("$.data[0].quantity").value(6));
    }

    @Test
    void unavailableSkuRemainsVisibleInCart() throws Exception {
        add(TestSeedData.SKU_ID, 1);
        Sku sku = skuMapper.selectById(TestSeedData.SKU_ID);
        sku.setStatus(0);
        skuMapper.updateById(sku);
        try {
            mockMvc.perform(get("/api/cart/items").header("Authorization", bearerToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].skuId").value(TestSeedData.SKU_ID))
                    .andExpect(jsonPath("$.data[0].available").value(false));
        } finally {
            sku.setStatus(1);
            skuMapper.updateById(sku);
        }
    }

    private void add(Long skuId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("{\"skuId\":" + skuId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String bearerToken() {
        return "Bearer " + token;
    }
}
