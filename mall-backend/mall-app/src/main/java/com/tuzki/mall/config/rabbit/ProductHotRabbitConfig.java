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
 * 商品热点 RabbitMQ 配置，声明热点事件交换机、队列、失败交换机和失败队列。
 */
@Configuration
@EnableConfigurationProperties(ProductHotRabbitProperties.class)
public class ProductHotRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    /**
     * 创建商品热点事件交换机。
     *
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 持久化直连交换机
     */
    @Bean
    public DirectExchange productHotEventExchange(ProductHotRabbitProperties properties) {
        return new DirectExchange(properties.getEventExchange(), true, false);
    }

    /**
     * 创建商品热点事件队列。
     *
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 持久化队列
     */
    @Bean
    public Queue productHotEventQueue(ProductHotRabbitProperties properties) {
        return new Queue(properties.getEventQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getFailedExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getFailedRoutingKey()
        ));
    }

    /**
     * 绑定商品热点事件交换机和队列。
     *
     * @param productHotEventQueue 商品热点事件队列
     * @param productHotEventExchange 商品热点事件交换机
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 商品热点事件绑定关系
     */
    @Bean
    public Binding productHotEventBinding(@Qualifier("productHotEventQueue") Queue productHotEventQueue,
                                          @Qualifier("productHotEventExchange") DirectExchange productHotEventExchange,
                                          ProductHotRabbitProperties properties) {
        return BindingBuilder.bind(productHotEventQueue)
                .to(productHotEventExchange)
                .with(properties.getEventRoutingKey());
    }

    /**
     * 创建商品热点失败交换机。
     *
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 商品热点失败交换机
     */
    @Bean
    public DirectExchange productHotFailedExchange(ProductHotRabbitProperties properties) {
        return new DirectExchange(properties.getFailedExchange(), true, false);
    }

    /**
     * 创建商品热点失败队列，用于保留消费多次失败后的消息。
     *
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 商品热点失败队列
     */
    @Bean
    public Queue productHotFailedQueue(ProductHotRabbitProperties properties) {
        return new Queue(properties.getFailedQueue(), true);
    }

    /**
     * 绑定商品热点失败交换机和失败队列。
     *
     * @param productHotFailedQueue 商品热点失败队列
     * @param productHotFailedExchange 商品热点失败交换机
     * @param properties 商品热点 RabbitMQ 配置属性
     * @return 商品热点失败绑定关系
     */
    @Bean
    public Binding productHotFailedBinding(@Qualifier("productHotFailedQueue") Queue productHotFailedQueue,
                                           @Qualifier("productHotFailedExchange") DirectExchange productHotFailedExchange,
                                           ProductHotRabbitProperties properties) {
        return BindingBuilder.bind(productHotFailedQueue)
                .to(productHotFailedExchange)
                .with(properties.getFailedRoutingKey());
    }
}
