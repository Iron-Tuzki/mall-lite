package com.tuzki.mall.seckill.mq;

import com.tuzki.mall.config.rabbit.SeckillRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 秒杀下单消息发送器测试，验证秒杀预扣成功后的成单消息会通过 Outbox 可靠投递。
 */
class SeckillOrderSenderTest {

    @Test
    void sendCreatesOutboxMessageUsingSeckillOrderExchangeAndRoutingKey() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        SeckillRabbitProperties properties = new SeckillRabbitProperties();
        SeckillOrderMessage message = buildMessage();

        new SeckillOrderSender(outboxMessageService, properties).send(message);

        verify(outboxMessageService).createAndPublish(
                "SECKILL_ORDER",
                "request-1",
                properties.getOrderExchange(),
                properties.getOrderRoutingKey(),
                message
        );
    }

    private SeckillOrderMessage buildMessage() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestLogId(10L);
        message.setUserId(1L);
        message.setSeckillSkuId(2L);
        message.setRequestId("request-1");
        message.setAddressId(3L);
        message.setQuantity(1);
        return message;
    }
}
