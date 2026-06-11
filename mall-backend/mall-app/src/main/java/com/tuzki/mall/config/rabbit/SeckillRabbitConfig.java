package com.tuzki.mall.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 秒杀 RabbitMQ 配置，声明异步下单队列、失败队列及对应交换机和绑定关系。
 */
@Configuration
@EnableConfigurationProperties(SeckillRabbitProperties.class)
public class SeckillRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    @Bean
    public DirectExchange seckillOrderExchange(SeckillRabbitProperties properties) {
        return new DirectExchange(properties.getOrderExchange(), true, false);
    }

    @Bean
    public Queue seckillOrderQueue(SeckillRabbitProperties properties) {
        return new Queue(properties.getOrderQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getFailedExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getFailedRoutingKey()
        ));
    }

    @Bean
    public Binding seckillOrderBinding(@Qualifier("seckillOrderQueue") Queue seckillOrderQueue,
                                       @Qualifier("seckillOrderExchange") DirectExchange seckillOrderExchange,
                                       SeckillRabbitProperties properties) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(properties.getOrderRoutingKey());
    }

    @Bean
    public DirectExchange seckillFailedExchange(SeckillRabbitProperties properties) {
        return new DirectExchange(properties.getFailedExchange(), true, false);
    }

    @Bean
    public Queue seckillFailedQueue(SeckillRabbitProperties properties) {
        return new Queue(properties.getFailedQueue(), true);
    }

    @Bean
    public Binding seckillFailedBinding(@Qualifier("seckillFailedQueue") Queue seckillFailedQueue,
                                        @Qualifier("seckillFailedExchange") DirectExchange seckillFailedExchange,
                                        SeckillRabbitProperties properties) {
        return BindingBuilder.bind(seckillFailedQueue)
                .to(seckillFailedExchange)
                .with(properties.getFailedRoutingKey());
    }
}
