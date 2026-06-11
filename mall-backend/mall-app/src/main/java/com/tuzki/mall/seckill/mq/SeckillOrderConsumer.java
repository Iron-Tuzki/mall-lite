package com.tuzki.mall.seckill.mq;

import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import com.tuzki.mall.seckill.service.SeckillService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息消费者，负责异步创建订单并推进秒杀请求流水状态。
 */
@Component
public class SeckillOrderConsumer {

    private final SeckillService seckillService;

    public SeckillOrderConsumer(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @RabbitListener(queues = "${mall.seckill.rabbit.order-queue}")
    public void handle(SeckillOrderMessage message) {
        seckillService.processQueuedSeckillOrder(message);
    }
}
