package com.tuzki.mall.order.scheduling;

import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.scheduling.lock.RedisDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时补偿任务，定期扫描遗漏的超时待支付订单并复用订单取消能力释放库存。
 */
@Component
public class OrderTimeoutCompensationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTimeoutCompensationTask.class);

    private static final String LOCK_KEY = "mall:order:timeout-compensation";

    private final OrderService orderService;

    private final OrderSchedulingProperties properties;

    public OrderTimeoutCompensationTask(OrderService orderService, OrderSchedulingProperties properties) {
        this.orderService = orderService;
        this.properties = properties;
    }

    /**
     * 扫描并取消超时待支付订单。单个订单处理失败时保留错误日志，由下一轮任务继续补偿。
     */
    @Scheduled(
            initialDelayString = "${mall.order.scheduling.compensation-fixed-delay-ms:60000}",
            fixedDelayString = "${mall.order.scheduling.compensation-fixed-delay-ms:60000}"
    )
    @RedisDistributedLock(LOCK_KEY)
    public void cancelTimeoutOrders() {
        long begin = System.currentTimeMillis();
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(properties.getTimeoutMinutes());
        List<Long> orderIds = orderService.listTimeoutPendingOrderIds(timeoutBefore, properties.getBatchSize());
        for (Long orderId : orderIds) {
            try {
                orderService.cancelTimeoutOrder(orderId);
            } catch (RuntimeException exception) {
                LOGGER.error("compensate timeout order failed, orderId={}", orderId, exception);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.info("订单超时补偿任务，定期扫描遗漏的超时待支付订单，耗时{}ms", (end - begin));
    }
}
