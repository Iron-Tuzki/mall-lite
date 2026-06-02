package com.tuzki.mall.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物车 RabbitMQ 配置测试，验证交换机、队列和绑定关系使用预期名称。
 */
class CartRabbitConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CartRabbitConfig.class)
            .withPropertyValues(
                    "mall.cart.rabbit.change-exchange=mall.cart.change.exchange",
                    "mall.cart.rabbit.change-queue=mall.cart.change.queue",
                    "mall.cart.rabbit.change-routing-key=mall.cart.change.routing-key",
                    "mall.cart.rabbit.confirm-timeout-seconds=3"
            );

    @Test
    void cartRabbitBeansUseConfiguredNames() {
        contextRunner.run(context -> {
            CartRabbitProperties properties = context.getBean(CartRabbitProperties.class);
            DirectExchange exchange = context.getBean("cartChangeExchange", DirectExchange.class);
            Queue queue = context.getBean("cartChangeQueue", Queue.class);
            Binding binding = context.getBean("cartChangeBinding", Binding.class);

            assertThat(properties.getConfirmTimeoutSeconds()).isEqualTo(3);
            assertThat(exchange.getName()).isEqualTo("mall.cart.change.exchange");
            assertThat(queue.getName()).isEqualTo("mall.cart.change.queue");
            assertThat(binding.getRoutingKey()).isEqualTo("mall.cart.change.routing-key");
        });
    }
}
