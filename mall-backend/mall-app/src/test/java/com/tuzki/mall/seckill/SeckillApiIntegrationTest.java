package com.tuzki.mall.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.seckill.entity.SeckillActivity;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import com.tuzki.mall.seckill.mapper.SeckillSkuMapper;
import com.tuzki.mall.seckill.redis.SeckillRedisService;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 秒杀接口集成测试，验证秒杀活动查询、库存预热、抢购下单和异常补偿流程。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class SeckillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillSkuMapper seckillSkuMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SeckillRedisService seckillRedisService;

    @BeforeEach
    void setUp() {
        ensureSeckillTables();
        jdbcTemplate.update("DELETE FROM sms_seckill_sku");
        jdbcTemplate.update("DELETE FROM sms_seckill_activity");
        redissonClient.getKeys().deleteByPattern("mall:seckill:*");
        resetInventory(100, 0);
    }

    @Test
    void listActiveActivitiesRouteExists() throws Exception {
        mockMvc.perform(get("/api/seckill/activities/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listActiveActivitiesReturnsEnabledCurrentActivity() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 5, 1);

        mockMvc.perform(get("/api/seckill/activities/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skus[0].id").value(seckillSku.getId()))
                .andExpect(jsonPath("$.data[0].skus[0].seckillPrice").value(39.90));
    }

    @Test
    void createSeckillOrderAfterPreheatUsesSeckillPriceAndLocksRealStock() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("success");

        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.totalAmount").value(39.90))
                .andExpect(jsonPath("$.data.payAmount").value(39.90));

        Order order = getOrderByRequestId(seckillRequestId(seckillSku.getId(), requestId));
        assertEquals(new BigDecimal("39.90"), order.getTotalAmount());

        OrderItem orderItem = getOrderItem(order.getId());
        assertEquals(new BigDecimal("39.90"), orderItem.getUnitPrice());
        assertEquals(new BigDecimal("39.90"), orderItem.getTotalAmount());

        Inventory inventory = getInventory();
        assertEquals(99, inventory.getAvailableStock());
        assertEquals(1, inventory.getLockedStock());
        assertEquals("1", readRedisStock(seckillSku.getId()));
    }

    @Test
    void createSeckillOrderRejectsStockNotPreheated() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("not-preheated"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("seckill stock not preheated"));
    }

    @Test
    void createSeckillOrderRejectsSoldOutStock() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 1, 5);
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("sold-out-first"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("sold-out-second"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("seckill stock sold out"));
    }

    @Test
    void createSeckillOrderRejectsPurchaseLimitExceeded() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 5, 1);
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("limit-first"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("limit-second"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("seckill purchase limit exceeded"));
    }

    @Test
    void createSeckillOrderReturnsExistingOrderWhenRequestIdRepeated() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("idempotent");
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order firstOrder = getOrderByRequestId(seckillRequestId(seckillSku.getId(), requestId));

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.data.orderNo").value(firstOrder.getOrderNo()));

        assertEquals(1L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, seckillRequestId(seckillSku.getId(), requestId))));
        assertEquals("1", readRedisStock(seckillSku.getId()));
    }

    @Test
    void createSeckillOrderCompensatesRedisWhenRealInventoryIsInsufficient() throws Exception {
        resetInventory(0, 0);
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 1, 2);
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("compensate-fail"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("insufficient stock"));

        assertEquals("1", readRedisStock(seckillSku.getId()));

        resetInventory(1, 0);
        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("compensate-success"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void ensureSeckillTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sms_seckill_activity
                (
                    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
                    name        VARCHAR(128)    NOT NULL COMMENT '活动名称',
                    start_time  DATETIME        NOT NULL COMMENT '活动开始时间',
                    end_time    DATETIME        NOT NULL COMMENT '活动结束时间',
                    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
                    remark      VARCHAR(255)    NULL COMMENT '活动备注',
                    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
                    PRIMARY KEY (id),
                    KEY idx_time_status (start_time, end_time, status)
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                  COLLATE = utf8mb4_0900_ai_ci
                  COMMENT = '秒杀活动表'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sms_seckill_sku
                (
                    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动商品ID',
                    activity_id    BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
                    sku_id         BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
                    seckill_price  DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价',
                    stock_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '活动库存',
                    limit_quantity INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '每人限购数量',
                    sort           INT             NOT NULL DEFAULT 0 COMMENT '排序值',
                    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
                    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_activity_sku (activity_id, sku_id),
                    KEY idx_activity_status_sort (activity_id, status, sort),
                    KEY idx_sku_id (sku_id)
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                  COLLATE = utf8mb4_0900_ai_ci
                  COMMENT = '秒杀活动商品表'
                """);
    }

    private SeckillSku createActiveSeckillSku(BigDecimal price, int stockCount, int limitQuantity) {
        SeckillActivity activity = new SeckillActivity();
        activity.setName("test seckill");
        activity.setStartTime(LocalDateTime.now().minusMinutes(5));
        activity.setEndTime(LocalDateTime.now().plusMinutes(30));
        activity.setStatus(1);
        activity.setDeleted(0);
        seckillActivityMapper.insert(activity);

        SeckillSku seckillSku = new SeckillSku();
        seckillSku.setActivityId(activity.getId());
        seckillSku.setSkuId(TestSeedData.SKU_ID);
        seckillSku.setSeckillPrice(price);
        seckillSku.setStockCount(stockCount);
        seckillSku.setLimitQuantity(limitQuantity);
        seckillSku.setSort(0);
        seckillSku.setStatus(1);
        seckillSku.setDeleted(0);
        seckillSkuMapper.insert(seckillSku);
        return seckillSku;
    }

    private void preheat(Long activityId) throws Exception {
        mockMvc.perform(post("/api/admin/seckill/activities/{activityId}/preheat", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String seckillOrderRequest(Long seckillSkuId, String requestId, Integer quantity) {
        return """
                {
                  "seckillSkuId": %d,
                  "requestId": "%s",
                  "addressId": %d,
                  "quantity": %d,
                  "remark": "seckill order test"
                }
                """.formatted(seckillSkuId, requestId, TestSeedData.ADDRESS_ID, quantity);
    }

    private String bearerToken() {
        return "Bearer " + loginSessionService.createSession(TestSeedData.USER_ID);
    }

    private String newRequestId(String prefix) {
        return UUID.randomUUID().toString();
    }

    private String seckillRequestId(Long seckillSkuId, String requestId) {
        return "seckill:" + seckillSkuId + ":" + requestId;
    }

    private Order getOrderByRequestId(String requestId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId));
        assertNotNull(order);
        return order;
    }

    private OrderItem getOrderItem(Long orderId) {
        OrderItem orderItem = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        assertNotNull(orderItem);
        return orderItem;
    }

    private Inventory getInventory() {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, TestSeedData.SKU_ID));
        assertNotNull(inventory);
        return inventory;
    }

    private void resetInventory(Integer availableStock, Integer lockedStock) {
        Inventory inventory = getInventory();
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventoryMapper.updateById(inventory);
    }

    private String readRedisStock(Long seckillSkuId) {
        return redissonClient.<String>getBucket(seckillRedisService.stockKey(seckillSkuId), StringCodec.INSTANCE).get();
    }
}
