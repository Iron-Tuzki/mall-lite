package com.tuzki.mall.config.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品搜索索引 RabbitMQ 配置测试，验证交换机、队列、绑定和死信参数使用预期名称。
 */
class ProductSearchIndexRabbitConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProductSearchIndexRabbitConfig.class)
            .withPropertyValues(
                    "mall.product.search-index-rabbit.event-exchange=mall.product.search-index.event.exchange",
                    "mall.product.search-index-rabbit.event-queue=mall.product.search-index.event.queue",
                    "mall.product.search-index-rabbit.event-routing-key=mall.product.search-index.event.routing-key",
                    "mall.product.search-index-rabbit.failed-exchange=mall.product.search-index.failed.exchange",
                    "mall.product.search-index-rabbit.failed-queue=mall.product.search-index.failed.queue",
                    "mall.product.search-index-rabbit.failed-routing-key=mall.product.search-index.failed.routing-key",
                    "mall.product.search-index-rabbit.confirm-timeout-seconds=3"
            );

    @Test
    void productSearchIndexRabbitBeansUseConfiguredNames() {
        contextRunner.run(context -> {
            ProductSearchIndexRabbitProperties properties = context.getBean(ProductSearchIndexRabbitProperties.class);
            DirectExchange eventExchange = context.getBean("productSearchIndexEventExchange", DirectExchange.class);
            Queue eventQueue = context.getBean("productSearchIndexEventQueue", Queue.class);
            Binding eventBinding = context.getBean("productSearchIndexEventBinding", Binding.class);
            DirectExchange failedExchange = context.getBean("productSearchIndexFailedExchange", DirectExchange.class);
            Queue failedQueue = context.getBean("productSearchIndexFailedQueue", Queue.class);
            Binding failedBinding = context.getBean("productSearchIndexFailedBinding", Binding.class);

            assertThat(properties.getConfirmTimeoutSeconds()).isEqualTo(3);
            assertThat(eventExchange.getName()).isEqualTo("mall.product.search-index.event.exchange");
            assertThat(eventQueue.getName()).isEqualTo("mall.product.search-index.event.queue");
            assertThat(eventQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "mall.product.search-index.failed.exchange")
                    .containsEntry("x-dead-letter-routing-key", "mall.product.search-index.failed.routing-key");
            assertThat(eventBinding.getRoutingKey()).isEqualTo("mall.product.search-index.event.routing-key");
            assertThat(failedExchange.getName()).isEqualTo("mall.product.search-index.failed.exchange");
            assertThat(failedQueue.getName()).isEqualTo("mall.product.search-index.failed.queue");
            assertThat(failedBinding.getRoutingKey()).isEqualTo("mall.product.search-index.failed.routing-key");
        });
    }
}
