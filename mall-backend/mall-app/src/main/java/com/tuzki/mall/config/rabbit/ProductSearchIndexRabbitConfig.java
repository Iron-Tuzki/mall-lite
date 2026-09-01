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
 * 商品搜索索引 RabbitMQ 配置，声明索引变更事件交换机、队列、失败交换机和失败队列。
 */
@Configuration
@EnableConfigurationProperties(ProductSearchIndexRabbitProperties.class)
public class ProductSearchIndexRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    /**
     * 创建商品搜索索引事件交换机。
     *
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 持久化直连交换机
     */
    @Bean
    public DirectExchange productSearchIndexEventExchange(ProductSearchIndexRabbitProperties properties) {
        return new DirectExchange(properties.getEventExchange(), true, false);
    }

    /**
     * 创建商品搜索索引事件队列。
     *
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 持久化队列
     */
    @Bean
    public Queue productSearchIndexEventQueue(ProductSearchIndexRabbitProperties properties) {
        return new Queue(properties.getEventQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getFailedExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getFailedRoutingKey()
        ));
    }

    /**
     * 绑定商品搜索索引事件交换机和队列。
     *
     * @param productSearchIndexEventQueue 商品搜索索引事件队列
     * @param productSearchIndexEventExchange 商品搜索索引事件交换机
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 商品搜索索引事件绑定关系
     */
    @Bean
    public Binding productSearchIndexEventBinding(
            @Qualifier("productSearchIndexEventQueue") Queue productSearchIndexEventQueue,
            @Qualifier("productSearchIndexEventExchange") DirectExchange productSearchIndexEventExchange,
            ProductSearchIndexRabbitProperties properties) {
        return BindingBuilder.bind(productSearchIndexEventQueue)
                .to(productSearchIndexEventExchange)
                .with(properties.getEventRoutingKey());
    }

    /**
     * 创建商品搜索索引失败交换机。
     *
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 商品搜索索引失败交换机
     */
    @Bean
    public DirectExchange productSearchIndexFailedExchange(ProductSearchIndexRabbitProperties properties) {
        return new DirectExchange(properties.getFailedExchange(), true, false);
    }

    /**
     * 创建商品搜索索引失败队列，用于保留消费失败后进入死信链路的消息。
     *
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 商品搜索索引失败队列
     */
    @Bean
    public Queue productSearchIndexFailedQueue(ProductSearchIndexRabbitProperties properties) {
        return new Queue(properties.getFailedQueue(), true);
    }

    /**
     * 绑定商品搜索索引失败交换机和失败队列。
     *
     * @param productSearchIndexFailedQueue 商品搜索索引失败队列
     * @param productSearchIndexFailedExchange 商品搜索索引失败交换机
     * @param properties 商品搜索索引 RabbitMQ 配置属性
     * @return 商品搜索索引失败绑定关系
     */
    @Bean
    public Binding productSearchIndexFailedBinding(
            @Qualifier("productSearchIndexFailedQueue") Queue productSearchIndexFailedQueue,
            @Qualifier("productSearchIndexFailedExchange") DirectExchange productSearchIndexFailedExchange,
            ProductSearchIndexRabbitProperties properties) {
        return BindingBuilder.bind(productSearchIndexFailedQueue)
                .to(productSearchIndexFailedExchange)
                .with(properties.getFailedRoutingKey());
    }
}
