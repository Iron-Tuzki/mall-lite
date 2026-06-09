package com.tuzki.mall.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.seckill.entity.SeckillActivity;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import com.tuzki.mall.seckill.mapper.SeckillSkuMapper;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 秒杀手工并发验证测试，依赖外部脚本准备活动、活动商品和 Redis 预热数据。
 */
@Disabled("手工并发验证类：先用脚本插入秒杀数据并手动预热 Redis，再临时启用执行。")
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.data.redis.database=0",
        "mall.test.redis.clean-enabled=false"
})
@AutoConfigureMockMvc
class SeckillManualConcurrentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillSkuMapper seckillSkuMapper;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentSeckillRequestsWithCommittedTransactionsForManualReview() throws Exception {
        Long skuId = 900017L;
        Long[] userIds = {999999L, 1000000L, 900313L};
        Long[] addressIds = {900059L, 900118L, 900119L};
        int expectedSuccessCount = 2;
        int expectedFailCount = 1;

        Long seckillSkuId = resolveActiveSeckillSkuId(skuId);
        int concurrentRequests = userIds.length;
        assertEquals(concurrentRequests, addressIds.length);

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        try {
            CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < concurrentRequests; i++) {
                int requestIndex = i;
                futures.add(executorService.submit(() -> {
                    readyLatch.countDown();
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS)); // 所有请求线程阻塞在这里
                    String token = loginSessionService.createSession(userIds[requestIndex]);
                    String requestId = "mc-" + requestIndex + "-" + UUID.randomUUID().toString().substring(0, 8);
                    return mockMvc.perform(post("/api/seckill/orders")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType("application/json")
                                    .content(seckillOrderRequest(seckillSkuId, requestId, addressIds[requestIndex], 1)))
                            .andExpect(status().isOk())
                            .andReturn();
                }));
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));// 主线程等待所有子线程准备好。CountDownLatch 的特点是：计数一旦归零，就永久打开。
            startLatch.countDown(); // 主线程通知所有请求线程开始执行

            int successCount = 0;
            int failCount = 0;
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get(10, TimeUnit.SECONDS);
                String responseBody = result.getResponse().getContentAsString();
                System.out.println(responseBody);
                JsonNode responseJson = objectMapper.readTree(responseBody);
                if (responseJson.path("success").asBoolean(false)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            assertEquals(expectedSuccessCount, successCount);
            assertEquals(expectedFailCount, failCount);
        } finally {
            executorService.shutdownNow();
            assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private Long resolveActiveSeckillSkuId(Long skuId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillSku> seckillSkus = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getSkuId, skuId)
                .eq(SeckillSku::getStatus, 1)
                .eq(SeckillSku::getDeleted, 0)
                .orderByDesc(SeckillSku::getId));
        for (SeckillSku seckillSku : seckillSkus) {
            SeckillActivity activity = seckillActivityMapper.selectById(seckillSku.getActivityId());
            if (activity != null
                    && Integer.valueOf(1).equals(activity.getStatus())
                    && Integer.valueOf(0).equals(activity.getDeleted())
                    && !now.isBefore(activity.getStartTime())
                    && !now.isAfter(activity.getEndTime())) {
                System.out.printf(
                        "candidate seckillSkuId=%d, skuId=%d, activityId=%d, start=%s, end=%s, now=%s%n",
                        seckillSku.getId(),
                        seckillSku.getSkuId(),
                        activity.getId(),
                        activity.getStartTime(),
                        activity.getEndTime(),
                        now
                );
                return seckillSku.getId();
            }
        }
        throw new IllegalStateException("active seckill sku not found, skuId=" + skuId);
    }

    private String seckillOrderRequest(Long seckillSkuId, String requestId, Long addressId, Integer quantity) {
        return """
                {
                  "seckillSkuId": %d,
                  "requestId": "%s",
                  "addressId": %d,
                  "quantity": %d,
                  "remark": "manual concurrent seckill order test"
                }
                """.formatted(seckillSkuId, requestId, addressId, quantity);
    }
}
