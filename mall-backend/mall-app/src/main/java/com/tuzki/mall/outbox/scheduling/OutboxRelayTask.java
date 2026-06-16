package com.tuzki.mall.outbox.scheduling;

import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.scheduling.lock.RedisDistributedLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 消息投递任务，定期扫描本地消息表并补发待发送或失败消息。
 */
@Component
public class OutboxRelayTask {

    private static final String LOCK_KEY = "mall:outbox:relay";

    private final OutboxMessageService outboxMessageService;

    private final OutboxRelayProperties properties;

    public OutboxRelayTask(OutboxMessageService outboxMessageService, OutboxRelayProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    /**
     * 补发到达重试时间的 Outbox 消息。关闭开关后直接跳过，便于本地调试或压测隔离。
     */
    @Scheduled(
            initialDelayString = "${mall.outbox.relay.fixed-delay-ms:60000}",
            fixedDelayString = "${mall.outbox.relay.fixed-delay-ms:60000}"
    )
    @RedisDistributedLock(LOCK_KEY)
    public void relayDueMessages() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        outboxMessageService.relayDueMessages(properties.getBatchSize());
    }
}
