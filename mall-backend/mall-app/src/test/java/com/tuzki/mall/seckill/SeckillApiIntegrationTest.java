package com.tuzki.mall.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.seckill.entity.SeckillActivity;
import com.tuzki.mall.seckill.entity.SeckillRequest;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import com.tuzki.mall.seckill.mapper.SeckillRequestMapper;
import com.tuzki.mall.seckill.mapper.SeckillSkuMapper;
import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import com.tuzki.mall.seckill.message.SeckillOrderMessageSender;
import com.tuzki.mall.seckill.redis.SeckillRedisService;
import com.tuzki.mall.seckill.service.SeckillCompensationService;
import com.tuzki.mall.seckill.service.SeckillService;
import com.tuzki.mall.seckill.sentinel.SeckillSentinelResources;
import com.tuzki.mall.seckill.scheduling.SeckillPreheatTask;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 秒杀接口集成测试，验证秒杀活动查询、库存预热、抢购下单和异常补偿流程。
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "mall.seckill.rate-limit.enabled=true",
        "mall.seckill.rate-limit.window-seconds=5",
        "mall.seckill.rate-limit.user-limit=3",
        "mall.seckill.rate-limit.ip-limit=10"
})
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
    private SeckillRequestMapper seckillRequestMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SeckillRedisService seckillRedisService;

    @Autowired
    private SeckillPreheatTask seckillPreheatTask;

    @Autowired
    private SeckillCompensationService seckillCompensationService;

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SeckillOrderMessageSender seckillOrderMessageSender;

    private SeckillOrderMessage lastSeckillOrderMessage;

    @BeforeTransaction
    void cleanCommittedSeckillRequests() {
        ensureSeckillTables();
        jdbcTemplate.update("DELETE FROM sms_seckill_request");
    }

    @BeforeEach
    void setUp() {
        FlowRuleManager.loadRules(List.of());
        ensureSeckillTables();
        jdbcTemplate.update("DELETE FROM sms_seckill_sku");
        jdbcTemplate.update("DELETE FROM sms_seckill_activity");
        redissonClient.getKeys().deleteByPattern("mall:seckill:*");
        resetInventory(100, 0);
        lastSeckillOrderMessage = null;
        doAnswer(invocation -> {
            lastSeckillOrderMessage = invocation.getArgument(0);
            return null;
        }).when(seckillOrderMessageSender).send(any());
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
    void createSeckillOrderAfterPreheatQueuesMessageAndConsumerCreatesOrder() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("success");

        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.seckillSkuId").value(seckillSku.getId()))
                .andExpect(jsonPath("$.data.requestId").value(requestId));

        verify(seckillOrderMessageSender).send(any(SeckillOrderMessage.class));
        assertNotNull(lastSeckillOrderMessage);
        assertEquals(TestSeedData.USER_ID, lastSeckillOrderMessage.getUserId());
        assertEquals(seckillSku.getId(), lastSeckillOrderMessage.getSeckillSkuId());

        SeckillRequest queuedRequest = getSeckillRequest(seckillSku.getId(), requestId);
        assertEquals(SeckillRequest.STATUS_PRE_DEDUCTED, queuedRequest.getStatus());
        assertEquals(null, queuedRequest.getOrderId());
        assertEquals("1", readRedisStock(seckillSku.getId()));
        assertEquals(2, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());

        seckillService.processQueuedSeckillOrder(lastSeckillOrderMessage);

        Order order = getOrderByRequestId(seckillRequestId(seckillSku.getId(), requestId));
        assertEquals(new BigDecimal("39.90"), order.getTotalAmount());

        OrderItem orderItem = getOrderItem(order.getId());
        assertEquals(new BigDecimal("39.90"), orderItem.getUnitPrice());
        assertEquals(new BigDecimal("39.90"), orderItem.getTotalAmount());

        Inventory inventory = getInventory();
        assertEquals(99, inventory.getAvailableStock());
        assertEquals(1, inventory.getLockedStock());
        assertEquals("1", readRedisStock(seckillSku.getId()));
        assertEquals(1, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());

        SeckillRequest seckillRequest = getSeckillRequest(seckillSku.getId(), requestId);
        assertEquals(30, seckillRequest.getStatus());
        assertEquals(order.getId(), seckillRequest.getOrderId());
        assertEquals(null, seckillRequest.getFailReason());

        mockMvc.perform(get("/api/seckill/orders/result")
                        .header("Authorization", bearerToken())
                        .param("seckillSkuId", String.valueOf(seckillSku.getId()))
                        .param("requestId", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").value(order.getId()));
    }

    @Test
    void cancelTimeoutSeckillOrderRestoresInventoryAndSeckillStockOnce() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("timeout-cancel");
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        seckillService.processQueuedSeckillOrder(lastSeckillOrderMessage);

        Order order = getOrderByRequestId(seckillRequestId(seckillSku.getId(), requestId));
        assertEquals(99, getInventory().getAvailableStock());
        assertEquals(1, getInventory().getLockedStock());
        assertEquals(1, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());
        assertEquals("1", readRedisStock(seckillSku.getId()));

        orderService.cancelTimeoutOrder(order.getId());
        orderService.cancelTimeoutOrder(order.getId());

        Inventory inventory = getInventory();
        assertEquals(100, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());
        assertEquals("2", readRedisStock(seckillSku.getId()));
        assertEquals(
                SeckillRequest.STATUS_CANCEL_COMPENSATED,
                getSeckillRequestInCurrentTransaction(seckillSku.getId(), requestId).getStatus());

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
    void createSeckillOrderReturnsTooFrequentWhenSentinelFlowControlTriggered() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        preheat(seckillSku.getActivityId());
        FlowRule flowRule = new FlowRule(SeckillSentinelResources.SECKILL_CREATE_ORDER);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(0);
        FlowRuleManager.loadRules(List.of(flowRule));

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("sentinel"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("seckill request too frequent"));
    }

    @Test
    void createSeckillOrderReturnsTooFrequentWhenUserRateLimitExceeded() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 10, 10);
        preheat(seckillSku.getActivityId());
        String token = bearerToken();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/seckill/orders")
                            .header("Authorization", token)
                            .contentType("application/json")
                            .content(seckillOrderRequest(seckillSku.getId(), newRequestId("user-rate-" + i), 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("user-rate-blocked"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("seckill request too frequent"));
    }

    @Test
    void createSeckillOrderReturnsTooFrequentWhenIpRateLimitExceeded() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 10, 10);
        preheat(seckillSku.getActivityId());
        String clientIp = "203.0.113.10";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/seckill/orders")
                            .header("Authorization", bearerToken(TestSeedData.USER_ID + i + 1))
                            .header("X-Forwarded-For", clientIp)
                            .contentType("application/json")
                            .content(seckillOrderRequest(seckillSku.getId(), newRequestId("ip-rate-" + i), 1)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken(TestSeedData.USER_ID + 100))
                        .header("X-Forwarded-For", clientIp)
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("ip-rate-blocked"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("seckill request too frequent"));
    }

    @Test
    void createSeckillOrderReturnsProcessingWhenRequestIdRepeatedBeforeConsumerFinished() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("idempotent");
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, seckillRequestId(seckillSku.getId(), requestId))));
        assertEquals("1", readRedisStock(seckillSku.getId()));
        assertEquals(2, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());

    }

    @Test
    void createSeckillOrderRejectsRepeatedProcessingRequest() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("processing");
        insertSeckillRequest(seckillSku, requestId, SeckillRequest.STATUS_INIT, null);

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void createSeckillOrderRejectsRepeatedFailedRequestWithOriginalReason() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("failed");
        insertSeckillRequest(seckillSku, requestId, SeckillRequest.STATUS_FAILED, "manual seckill failure");

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), requestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("manual seckill failure"));
    }

    @Test
    void createSeckillOrderCompensatesRedisWhenRealInventoryIsInsufficient() throws Exception {
        resetInventory(0, 0);
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 1, 2);
        String failedRequestId = newRequestId("compensate-fail");
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), failedRequestId, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        assertNotNull(lastSeckillOrderMessage);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seckillService.processQueuedSeckillOrder(lastSeckillOrderMessage));
        assertEquals("insufficient stock", exception.getMessage());

        assertEquals("1", readRedisStock(seckillSku.getId()));
        assertEquals(1, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());
        SeckillRequest failedRequest = getSeckillRequest(seckillSku.getId(), failedRequestId);
        assertEquals(50, failedRequest.getStatus());
        assertEquals("insufficient stock", failedRequest.getFailReason());

        resetInventory(1, 0);
        seckillService.processQueuedSeckillOrder(lastSeckillOrderMessage);
        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, seckillRequestId(seckillSku.getId(), failedRequestId))));
        assertEquals("1", readRedisStock(seckillSku.getId()));
        SeckillRequest compensatedRequest = getSeckillRequest(seckillSku.getId(), failedRequestId);
        assertEquals(SeckillRequest.STATUS_COMPENSATED, compensatedRequest.getStatus());

        resetInventory(1, 0);
        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("compensate-success"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void compensateTimedOutPreDeductedRequestsRestoresRedisAndMarksCompensated() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String requestId = newRequestId("timeout-compensate");
        preheat(seckillSku.getActivityId());
        insertSeckillRequest(
                seckillSku,
                requestId,
                SeckillRequest.STATUS_PRE_DEDUCTED,
                null,
                LocalDateTime.now().minusMinutes(10));
        redissonClient.getBucket(seckillRedisService.stockKey(seckillSku.getId()), StringCodec.INSTANCE).set("1");
        redissonClient.getBucket(seckillRedisService.userKey(seckillSku.getId(), TestSeedData.USER_ID), StringCodec.INSTANCE).set("1");
        redissonClient.getBucket(seckillRedisService.requestKey(seckillSku.getId(), TestSeedData.USER_ID, requestId), StringCodec.INSTANCE).set("1");

        int compensatedCount = compensateTimedOutPreDeductedRequests(
                LocalDateTime.now().minusMinutes(1),
                10,
                3);

        assertEquals(1, compensatedCount);
        assertEquals("2", readRedisStock(seckillSku.getId()));
        assertEquals(null, redissonClient.getBucket(seckillRedisService.userKey(seckillSku.getId(), TestSeedData.USER_ID), StringCodec.INSTANCE).get());
        assertEquals(null, redissonClient.getBucket(seckillRedisService.requestKey(seckillSku.getId(), TestSeedData.USER_ID, requestId), StringCodec.INSTANCE).get());
        SeckillRequest compensatedRequest = getSeckillRequest(seckillSku.getId(), requestId);
        assertEquals(SeckillRequest.STATUS_COMPENSATED, compensatedRequest.getStatus());
        assertEquals("seckill request compensation succeeded", compensatedRequest.getFailReason());
    }

    @Test
    void compensateTimedOutPreDeductedRequestsSkipsFreshRequestsAndMaxRetries() {
        SeckillSku freshSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        String freshRequestId = newRequestId("fresh-pre-deducted");
        insertSeckillRequest(
                freshSku,
                freshRequestId,
                SeckillRequest.STATUS_PRE_DEDUCTED,
                null,
                LocalDateTime.now());
        SeckillSku retriedSku = createActiveSeckillSku(new BigDecimal("29.90"), 2, 1);
        String retriedRequestId = newRequestId("max-retry");
        insertSeckillRequest(
                retriedSku,
                retriedRequestId,
                SeckillRequest.STATUS_PRE_DEDUCTED,
                null,
                LocalDateTime.now().minusMinutes(10),
                3);

        int compensatedCount = compensateTimedOutPreDeductedRequests(
                LocalDateTime.now().minusMinutes(1),
                10,
                3);

        assertEquals(0, compensatedCount);
        assertEquals(SeckillRequest.STATUS_PRE_DEDUCTED, getSeckillRequest(freshSku.getId(), freshRequestId).getStatus());
        assertEquals(SeckillRequest.STATUS_PRE_DEDUCTED, getSeckillRequest(retriedSku.getId(), retriedRequestId).getStatus());
    }

    @Test
    void preheatKeepsExistingRedisStockAndOnlyRefreshesTtl() throws Exception {
        SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 2, 1);
        preheat(seckillSku.getActivityId());

        mockMvc.perform(post("/api/seckill/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(seckillOrderRequest(seckillSku.getId(), newRequestId("preheat-keep"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals("1", readRedisStock(seckillSku.getId()));
        preheat(seckillSku.getActivityId());
        assertEquals("1", readRedisStock(seckillSku.getId()));
        assertEquals(2, seckillSkuMapper.selectById(seckillSku.getId()).getStockCount());
    }

    @Test
    void scheduledPreheatOnlyPreheatsCurrentAndSoonStartingActivities() {
        LocalDateTime now = LocalDateTime.now();
        SeckillSku currentSku = createSeckillSku(
                "current seckill",
                now.minusMinutes(5),
                now.plusMinutes(30),
                1,
                new BigDecimal("39.90"),
                2,
                1);
        SeckillSku soonSku = createSeckillSku(
                "soon seckill",
                now.plusMinutes(5),
                now.plusMinutes(40),
                1,
                new BigDecimal("29.90"),
                3,
                1);
        SeckillSku laterSku = createSeckillSku(
                "later seckill",
                now.plusMinutes(30),
                now.plusMinutes(60),
                1,
                new BigDecimal("19.90"),
                4,
                1);
        SeckillSku disabledSku = createSeckillSku(
                "disabled seckill",
                now.minusMinutes(5),
                now.plusMinutes(30),
                0,
                new BigDecimal("9.90"),
                5,
                1);
        SeckillSku endedSku = createSeckillSku(
                "ended seckill",
                now.minusMinutes(30),
                now.minusMinutes(5),
                1,
                new BigDecimal("8.90"),
                6,
                1);

        seckillPreheatTask.preheatUpcomingActivities();

        assertEquals("2", readRedisStock(currentSku.getId()));
        assertEquals("3", readRedisStock(soonSku.getId()));
        assertEquals(null, readRedisStock(laterSku.getId()));
        assertEquals(null, readRedisStock(disabledSku.getId()));
        assertEquals(null, readRedisStock(endedSku.getId()));
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
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sms_seckill_request
                (
                    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '秒杀请求流水ID',
                    request_id      VARCHAR(64)      NOT NULL COMMENT '秒杀请求幂等号',
                    user_id         BIGINT UNSIGNED  NOT NULL COMMENT '用户ID',
                    activity_id     BIGINT UNSIGNED  NOT NULL COMMENT '秒杀活动ID',
                    seckill_sku_id  BIGINT UNSIGNED  NOT NULL COMMENT '秒杀活动商品ID',
                    sku_id          BIGINT UNSIGNED  NOT NULL COMMENT 'SKU ID',
                    quantity        INT UNSIGNED     NOT NULL COMMENT '购买数量',
                    status          TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '状态：10初始化，20预扣成功，30订单创建成功，40失败，50已补偿',
                    order_id        BIGINT UNSIGNED  NULL COMMENT '订单ID',
                    fail_reason     VARCHAR(255)     NULL COMMENT '失败原因',
                    retry_count     INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '补偿重试次数',
                    request_ip      VARCHAR(64)      NULL COMMENT '请求IP',
                    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    deleted         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_user_sku_request (user_id, seckill_sku_id, request_id),
                    KEY idx_status_update_time (status, update_time),
                    KEY idx_order_id (order_id)
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                  COLLATE = utf8mb4_0900_ai_ci
                  COMMENT = '秒杀请求流水表'
                """);
    }

    private SeckillSku createActiveSeckillSku(BigDecimal price, int stockCount, int limitQuantity) {
        LocalDateTime now = LocalDateTime.now();
        return createSeckillSku(
                "test seckill",
                now.minusMinutes(5),
                now.plusMinutes(30),
                1,
                price,
                stockCount,
                limitQuantity);
    }

    private SeckillSku createSeckillSku(String name,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime,
                                        int status,
                                        BigDecimal price,
                                        int stockCount,
                                        int limitQuantity) {
        SeckillActivity activity = new SeckillActivity();
        activity.setName(name);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setStatus(status);
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
        return bearerToken(TestSeedData.USER_ID);
    }

    private String bearerToken(Long userId) {
        return "Bearer " + loginSessionService.createSession(userId);
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

    private SeckillRequest getSeckillRequest(Long seckillSkuId, String requestId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        SeckillRequest seckillRequest = transactionTemplate.execute(status -> seckillRequestMapper.selectOne(
                new LambdaQueryWrapper<SeckillRequest>()
                        .eq(SeckillRequest::getUserId, TestSeedData.USER_ID)
                        .eq(SeckillRequest::getSeckillSkuId, seckillSkuId)
                        .eq(SeckillRequest::getRequestId, requestId)));
        assertNotNull(seckillRequest);
        return seckillRequest;
    }

    private SeckillRequest getSeckillRequestInCurrentTransaction(Long seckillSkuId, String requestId) {
        SeckillRequest seckillRequest = seckillRequestMapper.selectOne(
                new LambdaQueryWrapper<SeckillRequest>()
                        .eq(SeckillRequest::getUserId, TestSeedData.USER_ID)
                        .eq(SeckillRequest::getSeckillSkuId, seckillSkuId)
                        .eq(SeckillRequest::getRequestId, requestId));
        assertNotNull(seckillRequest);
        return seckillRequest;
    }

    private int compensateTimedOutPreDeductedRequests(LocalDateTime timeoutBefore, int batchSize, int maxRetryCount) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Integer compensatedCount = transactionTemplate.execute(status ->
                seckillCompensationService.compensateTimedOutPreDeductedRequests(timeoutBefore, batchSize, maxRetryCount));
        assertNotNull(compensatedCount);
        return compensatedCount;
    }

    private void insertSeckillRequest(SeckillSku seckillSku, String requestId, int requestStatus, String failReason) {
        insertSeckillRequest(seckillSku, requestId, requestStatus, failReason, null);
    }

    private void insertSeckillRequest(SeckillSku seckillSku,
                                      String requestId,
                                      int requestStatus,
                                      String failReason,
                                      LocalDateTime updateTime) {
        insertSeckillRequest(seckillSku, requestId, requestStatus, failReason, updateTime, 0);
    }

    private void insertSeckillRequest(SeckillSku seckillSku,
                                      String requestId,
                                      int requestStatus,
                                      String failReason,
                                      LocalDateTime updateTime,
                                      int retryCount) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            SeckillRequest seckillRequest = new SeckillRequest();
            seckillRequest.setRequestId(requestId);
            seckillRequest.setUserId(TestSeedData.USER_ID);
            seckillRequest.setActivityId(seckillSku.getActivityId());
            seckillRequest.setSeckillSkuId(seckillSku.getId());
            seckillRequest.setSkuId(seckillSku.getSkuId());
            seckillRequest.setQuantity(1);
            seckillRequest.setStatus(requestStatus);
            seckillRequest.setFailReason(failReason);
            seckillRequest.setRetryCount(retryCount);
            seckillRequest.setUpdateTime(updateTime);
            seckillRequest.setDeleted(0);
            seckillRequestMapper.insert(seckillRequest);
        });
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
