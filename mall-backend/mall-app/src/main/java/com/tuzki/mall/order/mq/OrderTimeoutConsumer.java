package com.tuzki.mall.order.mq;

import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息消费者，负责消费订单超时检查消息并触发待支付订单自动取消。
 */
@Component
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    public OrderTimeoutConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 处理订单超时检查消息。
     *
     * @param message 订单超时检查消息，包含需要检查的订单 ID
     */
    @RabbitListener(queues = "${mall.order.rabbit.cancel-queue}")
    public void handle(OrderTimeoutMessage message) {
        orderService.cancelTimeoutOrder(message.getOrderId());
    }
}
