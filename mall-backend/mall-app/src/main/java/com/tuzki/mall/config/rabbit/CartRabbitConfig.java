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
 * 购物车 RabbitMQ 配置，声明逐项变更交换机、队列、死信交换机和失败队列。
 */
@Configuration
@EnableConfigurationProperties(CartRabbitProperties.class)
public class CartRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    /**
     * 创建购物车逐项变更交换机。
     *
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 持久化直连交换机
     */
    @Bean
    public DirectExchange cartChangeExchange(CartRabbitProperties properties) {
        return new DirectExchange(properties.getChangeExchange(), true, false);
    }

    /**
     * 创建购物车逐项变更队列。
     *
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 持久化队列
     */
    @Bean
    public Queue cartChangeQueue(CartRabbitProperties properties) {
        return new Queue(properties.getChangeQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getFailedExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getFailedRoutingKey()
        ));
    }

    /**
     * 绑定购物车逐项变更交换机和队列。
     *
     * @param cartChangeQueue 购物车逐项变更队列
     * @param cartChangeExchange 购物车逐项变更交换机
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 购物车变更绑定关系
     */
    @Bean
    public Binding cartChangeBinding(@Qualifier("cartChangeQueue") Queue cartChangeQueue,
                                     @Qualifier("cartChangeExchange") DirectExchange cartChangeExchange,
                                     CartRabbitProperties properties) {
        return BindingBuilder.bind(cartChangeQueue)
                .to(cartChangeExchange)
                .with(properties.getChangeRoutingKey());
    }

    /**
     * 创建购物车同步失败交换机，接收消费多次失败后转入的死信消息。
     *
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 购物车同步失败交换机
     */
    @Bean
    public DirectExchange cartFailedExchange(CartRabbitProperties properties) {
        return new DirectExchange(properties.getFailedExchange(), true, false);
    }

    /**
     * 创建购物车同步失败队列，用于保留需要人工排查或重新投递的消息。
     *
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 购物车同步失败队列
     */
    @Bean
    public Queue cartFailedQueue(CartRabbitProperties properties) {
        return new Queue(properties.getFailedQueue(), true);
    }

    /**
     * 绑定购物车同步失败交换机和失败队列。
     *
     * @param cartFailedQueue 购物车同步失败队列
     * @param cartFailedExchange 购物车同步失败交换机
     * @param properties 购物车 RabbitMQ 配置属性
     * @return 购物车同步失败绑定关系
     */
    @Bean
    public Binding cartFailedBinding(@Qualifier("cartFailedQueue") Queue cartFailedQueue,
                                     @Qualifier("cartFailedExchange") DirectExchange cartFailedExchange,
                                     CartRabbitProperties properties) {
        return BindingBuilder.bind(cartFailedQueue)
                .to(cartFailedExchange)
                .with(properties.getFailedRoutingKey());
    }
}
