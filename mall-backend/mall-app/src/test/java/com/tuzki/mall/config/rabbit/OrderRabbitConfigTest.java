package com.tuzki.mall.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单 RabbitMQ 配置测试，验证订单超时取消相关交换机、队列、绑定关系和延迟队列参数。
 */
class OrderRabbitConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OrderRabbitConfig.class)
            .withPropertyValues(
                    "mall.order.rabbit.delay-exchange=mall.order.delay.exchange",
                    "mall.order.rabbit.delay-queue=mall.order.delay.queue",
                    "mall.order.rabbit.delay-routing-key=mall.order.delay.routing-key",
                    "mall.order.rabbit.cancel-exchange=mall.order.cancel.exchange",
                    "mall.order.rabbit.cancel-queue=mall.order.cancel.queue",
                    "mall.order.rabbit.cancel-routing-key=mall.order.cancel.routing-key",
                    "mall.order.rabbit.timeout-minutes=30"
            );

    @Test
    void orderRabbitBeansUseConfiguredNamesAndDelayQueueArguments() {
        contextRunner.run(context -> {
            OrderRabbitProperties properties = context.getBean(OrderRabbitProperties.class);
            DirectExchange delayExchange = context.getBean("orderDelayExchange", DirectExchange.class);
            Queue delayQueue = context.getBean("orderDelayQueue", Queue.class);
            Binding delayBinding = context.getBean("orderDelayBinding", Binding.class);
            DirectExchange cancelExchange = context.getBean("orderCancelExchange", DirectExchange.class);
            Queue cancelQueue = context.getBean("orderCancelQueue", Queue.class);
            Binding cancelBinding = context.getBean("orderCancelBinding", Binding.class);

            assertThat(properties.getTimeoutMinutes()).isEqualTo(30);
            assertThat(delayExchange.getName()).isEqualTo("mall.order.delay.exchange");
            assertThat(delayQueue.getName()).isEqualTo("mall.order.delay.queue");
            assertThat(delayQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "mall.order.cancel.exchange")
                    .containsEntry("x-dead-letter-routing-key", "mall.order.cancel.routing-key")
                    .containsEntry("x-message-ttl", 1_800_000);
            assertThat(delayBinding.getRoutingKey()).isEqualTo("mall.order.delay.routing-key");

            assertThat(cancelExchange.getName()).isEqualTo("mall.order.cancel.exchange");
            assertThat(cancelQueue.getName()).isEqualTo("mall.order.cancel.queue");
            assertThat(cancelBinding.getRoutingKey()).isEqualTo("mall.order.cancel.routing-key");
        });
    }
}
