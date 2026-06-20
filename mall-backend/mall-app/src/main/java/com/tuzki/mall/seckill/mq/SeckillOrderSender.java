package com.tuzki.mall.seckill.mq;

import com.tuzki.mall.config.rabbit.SeckillRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import com.tuzki.mall.seckill.message.SeckillOrderMessageSender;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息发送器，负责将 Redis 预扣成功的秒杀请求写入 Outbox 并投递到 RabbitMQ。
 */
@Component
public class SeckillOrderSender implements SeckillOrderMessageSender {

    private static final String AGGREGATE_TYPE = "SECKILL_ORDER";

    private final OutboxMessageService outboxMessageService;

    private final SeckillRabbitProperties properties;

    public SeckillOrderSender(OutboxMessageService outboxMessageService, SeckillRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    @Override
    public void send(SeckillOrderMessage message) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                message.getRequestId(),
                properties.getOrderExchange(),
                properties.getOrderRoutingKey(),
                message
        );
    }
}
