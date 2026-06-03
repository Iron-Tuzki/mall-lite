package com.tuzki.mall.product.lifecycle;

import com.tuzki.mall.product.service.ProductHotService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品热点启动生命周期测试，验证应用启动完成后的榜单初始化和 RabbitMQ 声明检查。
 */
class ProductHotStartupLifecycleTest {

    @Test
    void applicationReadyAggregatesHomepageHotProductsAndDeclaresRabbitResources() {
        ProductHotService productHotService = mock(ProductHotService.class);
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        DirectExchange eventExchange = new DirectExchange("mall.product.hot.event.exchange", true, false);
        Queue eventQueue = new Queue("mall.product.hot.event.queue", true);
        Binding eventBinding = mock(Binding.class);
        DirectExchange failedExchange = new DirectExchange("mall.product.hot.failed.exchange", true, false);
        Queue failedQueue = new Queue("mall.product.hot.failed.queue", true);
        Binding failedBinding = mock(Binding.class);
        ProductHotStartupLifecycle lifecycle = new ProductHotStartupLifecycle(
                productHotService,
                rabbitAdmin,
                eventExchange,
                eventQueue,
                eventBinding,
                failedExchange,
                failedQueue,
                failedBinding
        );

        lifecycle.initializeAfterApplicationReady();

        verify(productHotService).aggregateHomepageHotProducts();
        verify(rabbitAdmin).declareExchange(eventExchange);
        verify(rabbitAdmin).declareQueue(eventQueue);
        verify(rabbitAdmin).declareBinding(eventBinding);
        verify(rabbitAdmin).declareExchange(failedExchange);
        verify(rabbitAdmin).declareQueue(failedQueue);
        verify(rabbitAdmin).declareBinding(failedBinding);
    }

    @Test
    void rabbitDeclarationFailureDoesNotBlockOtherStartupChecks() {
        ProductHotService productHotService = mock(ProductHotService.class);
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        DirectExchange eventExchange = new DirectExchange("mall.product.hot.event.exchange", true, false);
        Queue eventQueue = new Queue("mall.product.hot.event.queue", true);
        Binding eventBinding = mock(Binding.class);
        DirectExchange failedExchange = new DirectExchange("mall.product.hot.failed.exchange", true, false);
        Queue failedQueue = new Queue("mall.product.hot.failed.queue", true);
        Binding failedBinding = mock(Binding.class);
        doThrow(new AmqpException("exchange declare failed")).when(rabbitAdmin).declareExchange(eventExchange);
        ProductHotStartupLifecycle lifecycle = new ProductHotStartupLifecycle(
                productHotService,
                rabbitAdmin,
                eventExchange,
                eventQueue,
                eventBinding,
                failedExchange,
                failedQueue,
                failedBinding
        );

        lifecycle.initializeAfterApplicationReady();

        verify(productHotService).aggregateHomepageHotProducts();
        verify(rabbitAdmin).declareQueue(eventQueue);
        verify(rabbitAdmin).declareBinding(eventBinding);
        verify(rabbitAdmin).declareExchange(failedExchange);
        verify(rabbitAdmin).declareQueue(failedQueue);
        verify(rabbitAdmin).declareBinding(failedBinding);
    }

    @Test
    void hotAggregationFailureDoesNotBlockRabbitDeclarationCheck() {
        ProductHotService productHotService = mock(ProductHotService.class);
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        DirectExchange eventExchange = new DirectExchange("mall.product.hot.event.exchange", true, false);
        Queue eventQueue = new Queue("mall.product.hot.event.queue", true);
        Binding eventBinding = mock(Binding.class);
        DirectExchange failedExchange = new DirectExchange("mall.product.hot.failed.exchange", true, false);
        Queue failedQueue = new Queue("mall.product.hot.failed.queue", true);
        Binding failedBinding = mock(Binding.class);
        doThrow(new IllegalStateException("redis failed")).when(productHotService).aggregateHomepageHotProducts();
        ProductHotStartupLifecycle lifecycle = new ProductHotStartupLifecycle(
                productHotService,
                rabbitAdmin,
                eventExchange,
                eventQueue,
                eventBinding,
                failedExchange,
                failedQueue,
                failedBinding
        );

        lifecycle.initializeAfterApplicationReady();

        verify(rabbitAdmin).declareExchange(eventExchange);
        verify(rabbitAdmin).declareQueue(eventQueue);
        verify(rabbitAdmin).declareBinding(eventBinding);
        verify(rabbitAdmin).declareExchange(failedExchange);
        verify(rabbitAdmin).declareQueue(failedQueue);
        verify(rabbitAdmin).declareBinding(failedBinding);
    }
}
