package com.tuzki.mall.config.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 优惠券 RabbitMQ 队列配置，声明队列、死信交换机和自动取消队列。
 */
@Configuration
@EnableConfigurationProperties(CouponRewardRabbitProperties.class) // 开启CouponRewardRabbitProperties配置类
public class CouponRabbitConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";

    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    private static final String MESSAGE_TTL_ARGUMENT = "x-message-ttl";

    /**
     * 创建发放优惠券的交换机。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供交换机名称
     * @return 订单超时消息延迟交换机
     */
    @Bean
    public DirectExchange couponExchange(CouponRewardRabbitProperties properties) {
        return new DirectExchange(properties.getCouponRewardExchange(), true, false);
    }

    /**
     * 创建发放优惠券的队列。
     */
    @Bean
    public Queue couponQueue(CouponRewardRabbitProperties properties) {
        return new Queue(properties.getCouponRewardQueue(), true, false, false, Map.of(
                DEAD_LETTER_EXCHANGE_ARGUMENT, properties.getFailedExchange(),
                DEAD_LETTER_ROUTING_KEY_ARGUMENT, properties.getFailedRoutingKey(),
                MESSAGE_TTL_ARGUMENT, toMilliseconds(properties.getTimeoutMinutes())
        ));
    }

    /**
     * 绑定交换机和队列。
     *
     */
    @Bean
    public Binding couponBinding(@Qualifier("couponQueue") Queue couponQueue,
                                     @Qualifier("couponExchange") DirectExchange couponExchange,
                                     CouponRewardRabbitProperties properties) {
        return BindingBuilder.bind(couponQueue)
                .to(couponExchange)
                .with(properties.getCouponRewardRoutingKey());
    }

    /**
     * 创建优惠券失败交换机
     */
    @Bean
    public DirectExchange couponFailedExchange(CouponRewardRabbitProperties properties) {
        return new DirectExchange(properties.getFailedExchange(), true, false);
    }

    /**
     * 创建订单取消队列，后续订单超时自动取消消费者会监听该队列。
     *
     * @param properties 订单 RabbitMQ 配置属性，提供取消队列名称
     * @return 订单取消队列
     */
    @Bean
    public Queue couponFailedQueue(CouponRewardRabbitProperties properties) {
        return new Queue(properties.getFailedQueue(), true, false, false);
    }


    @Bean
    public Binding couponFailedBinding(@Qualifier("couponFailedQueue") Queue couponFailedQueue,
                                      @Qualifier("couponFailedExchange") DirectExchange couponFailedExchange,
                                      CouponRewardRabbitProperties properties) {
        return BindingBuilder.bind(couponFailedQueue)
                .to(couponFailedExchange)
                .with(properties.getFailedRoutingKey());
    }


    private Integer toMilliseconds(Integer timeoutMinutes) {
        return timeoutMinutes * 60 * 1000;
    }
}
