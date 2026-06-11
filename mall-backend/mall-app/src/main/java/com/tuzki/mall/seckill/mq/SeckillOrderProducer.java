package com.tuzki.mall.seckill.mq;

import com.tuzki.mall.config.rabbit.SeckillRabbitProperties;
import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import com.tuzki.mall.seckill.message.SeckillOrderMessageSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息生产者，负责把 Redis 预扣成功的秒杀请求投递到 RabbitMQ。
 */
@Component
public class SeckillOrderProducer implements SeckillOrderMessageSender {

    private final RabbitTemplate rabbitTemplate;

    private final SeckillRabbitProperties properties;

    public SeckillOrderProducer(RabbitTemplate rabbitTemplate, SeckillRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void send(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getOrderExchange(),
                properties.getOrderRoutingKey(),
                message
        );
    }
}
