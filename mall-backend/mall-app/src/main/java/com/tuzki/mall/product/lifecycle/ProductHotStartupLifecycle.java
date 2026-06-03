package com.tuzki.mall.product.lifecycle;

import com.tuzki.mall.product.service.ProductHotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 商品热点启动生命周期处理器，在应用启动完成后初始化首页热门榜单并检查 RabbitMQ 声明状态。
 */
@Component
public class ProductHotStartupLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductHotStartupLifecycle.class);

    private final ProductHotService productHotService;

    private final RabbitAdmin rabbitAdmin;

    private final DirectExchange eventExchange;

    private final Queue eventQueue;

    private final Binding eventBinding;

    private final DirectExchange failedExchange;

    private final Queue failedQueue;

    private final Binding failedBinding;

    public ProductHotStartupLifecycle(ProductHotService productHotService,
                                      RabbitAdmin rabbitAdmin,
                                      @Qualifier("productHotEventExchange") DirectExchange eventExchange,
                                      @Qualifier("productHotEventQueue") Queue eventQueue,
                                      @Qualifier("productHotEventBinding") Binding eventBinding,
                                      @Qualifier("productHotFailedExchange") DirectExchange failedExchange,
                                      @Qualifier("productHotFailedQueue") Queue failedQueue,
                                      @Qualifier("productHotFailedBinding") Binding failedBinding) {
        this.productHotService = productHotService;
        this.rabbitAdmin = rabbitAdmin;
        this.eventExchange = eventExchange;
        this.eventQueue = eventQueue;
        this.eventBinding = eventBinding;
        this.failedExchange = failedExchange;
        this.failedQueue = failedQueue;
        this.failedBinding = failedBinding;
    }

    /**
     * 应用启动完成后执行热点功能初始化。该方法不向外抛异常，避免热点能力影响主应用启动。
     * 注解 @EventListener(ApplicationReadyEvent.class)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAfterApplicationReady() {
        aggregateHomepageHotProducts();
        declareRabbitResources();
    }

    private void aggregateHomepageHotProducts() {
        try {
            productHotService.aggregateHomepageHotProducts();
            LOGGER.info("首页热门商品榜单启动初始化完成");
        } catch (RuntimeException exception) {
            LOGGER.warn("首页热门商品榜单启动初始化失败，将等待定时任务下一轮聚合", exception);
        }
    }

    private void declareRabbitResources() {
        declareExchange(eventExchange);
        declareQueue(eventQueue);
        declareBinding(eventBinding);
        declareExchange(failedExchange);
        declareQueue(failedQueue);
        declareBinding(failedBinding);
    }

    private void declareExchange(DirectExchange exchange) {
        try {
            rabbitAdmin.declareExchange(exchange);
            LOGGER.info("商品热点 RabbitMQ 交换机声明检查完成：{}", exchange.getName());
        } catch (RuntimeException exception) {
            LOGGER.warn("商品热点 RabbitMQ 交换机声明检查失败：{}", exchange.getName(), exception);
        }
    }

    private void declareQueue(Queue queue) {
        try {
            rabbitAdmin.declareQueue(queue);
            LOGGER.info("商品热点 RabbitMQ 队列声明检查完成：{}", queue.getName());
        } catch (RuntimeException exception) {
            LOGGER.warn("商品热点 RabbitMQ 队列声明检查失败：{}", queue.getName(), exception);
        }
    }

    private void declareBinding(Binding binding) {
        try {
            rabbitAdmin.declareBinding(binding);
            LOGGER.info("商品热点 RabbitMQ 绑定声明检查完成：{} -> {}",
                    binding.getExchange(), binding.getDestination());
        } catch (RuntimeException exception) {
            LOGGER.warn("商品热点 RabbitMQ 绑定声明检查失败：{} -> {}",
                    binding.getExchange(), binding.getDestination(), exception);
        }
    }
}
