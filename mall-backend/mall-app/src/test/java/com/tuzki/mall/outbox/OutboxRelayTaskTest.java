package com.tuzki.mall.outbox;

import com.tuzki.mall.outbox.scheduling.OutboxRelayProperties;
import com.tuzki.mall.outbox.scheduling.OutboxRelayTask;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Outbox 投递任务测试，验证任务按配置批量补发到期消息。
 */
class OutboxRelayTaskTest {

    @Test
    void relaysDueMessagesWithConfiguredBatchSize() {
        OutboxMessageService service = mock(OutboxMessageService.class);
        OutboxRelayProperties properties = new OutboxRelayProperties();
        properties.setBatchSize(50);

        new OutboxRelayTask(service, properties).relayDueMessages();

        verify(service).relayDueMessages(50);
    }
}
