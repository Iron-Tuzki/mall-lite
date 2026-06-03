package com.tuzki.mall.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品热点 RabbitMQ 配置测试，验证交换机、队列、失败队列和绑定关系使用预期名称。
 */
class ProductHotRabbitConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProductHotRabbitConfig.class)
            .withPropertyValues(
                    "mall.product.hot-rabbit.event-exchange=mall.product.hot.event.exchange",
                    "mall.product.hot-rabbit.event-queue=mall.product.hot.event.queue",
                    "mall.product.hot-rabbit.event-routing-key=mall.product.hot.event.routing-key",
                    "mall.product.hot-rabbit.failed-exchange=mall.product.hot.failed.exchange",
                    "mall.product.hot-rabbit.failed-queue=mall.product.hot.failed.queue",
                    "mall.product.hot-rabbit.failed-routing-key=mall.product.hot.failed.routing-key",
                    "mall.product.hot-rabbit.confirm-timeout-seconds=3"
            );

    @Test
    void productHotRabbitBeansUseConfiguredNames() {
        contextRunner.run(context -> {
            ProductHotRabbitProperties properties = context.getBean(ProductHotRabbitProperties.class);
            DirectExchange eventExchange = context.getBean("productHotEventExchange", DirectExchange.class);
            Queue eventQueue = context.getBean("productHotEventQueue", Queue.class);
            Binding eventBinding = context.getBean("productHotEventBinding", Binding.class);
            DirectExchange failedExchange = context.getBean("productHotFailedExchange", DirectExchange.class);
            Queue failedQueue = context.getBean("productHotFailedQueue", Queue.class);
            Binding failedBinding = context.getBean("productHotFailedBinding", Binding.class);

            assertThat(properties.getConfirmTimeoutSeconds()).isEqualTo(3);
            assertThat(eventExchange.getName()).isEqualTo("mall.product.hot.event.exchange");
            assertThat(eventQueue.getName()).isEqualTo("mall.product.hot.event.queue");
            assertThat(eventQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "mall.product.hot.failed.exchange")
                    .containsEntry("x-dead-letter-routing-key", "mall.product.hot.failed.routing-key");
            assertThat(eventBinding.getRoutingKey()).isEqualTo("mall.product.hot.event.routing-key");
            assertThat(failedExchange.getName()).isEqualTo("mall.product.hot.failed.exchange");
            assertThat(failedQueue.getName()).isEqualTo("mall.product.hot.failed.queue");
            assertThat(failedBinding.getRoutingKey()).isEqualTo("mall.product.hot.failed.routing-key");
        });
    }
}
