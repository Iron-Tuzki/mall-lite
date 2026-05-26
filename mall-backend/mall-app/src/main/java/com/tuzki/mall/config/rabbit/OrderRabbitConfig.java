package com.tuzki.mall.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 订单 RabbitMQ 队列配置，声明订单超时延迟队列、死信交换机和自动取消队列。
 */
@Configuration
@EnableConfigurationProperties(OrderRabbitProperties.class)
public class OrderRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    private static final String MESSAGE_TTL_ARGUMENT = "x-message-ttl";

    /**
     * 创建订单超时消息延迟交换机。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供交换机名称
     * @return 订单超时消息延迟交换机
     */
    @Bean
    public DirectExchange orderDelayExchange(OrderRabbitProperties properties) {
        return new DirectExchange(properties.getDelayExchange(), true, false);
    }

    /**
     * 创建订单超时消息延迟队列，消息过期后会进入订单取消交换机。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供队列名称、死信目标和超时时间
     * @return 订单超时消息延迟队列
     */
    @Bean
    public Queue orderDelayQueue(OrderRabbitProperties properties) {
        return new Queue(properties.getDelayQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getCancelExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getCancelRoutingKey(),
                MESSAGE_TTL_ARGUMENT, toMilliseconds(properties.getTimeoutMinutes())
        ));
    }

    /**
     * 绑定订单超时消息延迟交换机和延迟队列。
     *
     * @param orderDelayQueue 订单超时消息延迟队列
     * @param orderDelayExchange 订单超时消息延迟交换机
     * @param properties 订单 RabbitMQ 配置属性，提供延迟路由键
     * @return 订单超时消息延迟绑定关系
     */
    @Bean
    public Binding orderDelayBinding(@Qualifier("orderDelayQueue") Queue orderDelayQueue,
                                     @Qualifier("orderDelayExchange") DirectExchange orderDelayExchange,
                                     OrderRabbitProperties properties) {
        return BindingBuilder.bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with(properties.getDelayRoutingKey());
    }

    /**
     * 创建订单取消交换机，接收延迟队列过期后投递过来的死信消息。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供取消交换机名称
     * @return 订单取消交换机
     */
    @Bean
    public DirectExchange orderCancelExchange(OrderRabbitProperties properties) {
        return new DirectExchange(properties.getCancelExchange(), true, false);
    }

    /**
     * 创建订单取消队列，后续订单超时自动取消消费者会监听该队列。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供取消队列名称
     * @return 订单取消队列
     */
    @Bean
    public Queue orderCancelQueue(OrderRabbitProperties properties) {
        return new Queue(properties.getCancelQueue(), true);
    }

    /**
     * 绑定订单取消交换机和订单取消队列。
     *
     * @param orderCancelQueue 订单取消队列
     * @param orderCancelExchange 订单取消交换机
     * @param properties 订单 RabbitMQ 配置属性，提供取消路由键
     * @return 订单取消绑定关系
     */
    @Bean
    public Binding orderCancelBinding(@Qualifier("orderCancelQueue") Queue orderCancelQueue,
                                      @Qualifier("orderCancelExchange") DirectExchange orderCancelExchange,
                                      OrderRabbitProperties properties) {
        return BindingBuilder.bind(orderCancelQueue)
                .to(orderCancelExchange)
                .with(properties.getCancelRoutingKey());
    }

    private Integer toMilliseconds(Integer timeoutMinutes) {
        return timeoutMinutes * 60 * 1000;
    }
}
