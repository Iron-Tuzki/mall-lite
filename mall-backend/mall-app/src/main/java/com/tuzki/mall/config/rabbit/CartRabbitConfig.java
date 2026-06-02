package com.tuzki.mall.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 购物车 RabbitMQ 配置，声明逐项变更交换机、队列和绑定关系。
 */
@Configuration
@EnableConfigurationProperties(CartRabbitProperties.class)
public class CartRabbitConfig {

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
        return new Queue(properties.getChangeQueue(), true);
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
}
