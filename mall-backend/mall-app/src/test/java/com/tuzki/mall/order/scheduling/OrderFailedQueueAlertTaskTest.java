package com.tuzki.mall.order.scheduling;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 订单取消失败队列告警任务测试，验证队列积压时会输出结构化错误日志。
 */
@ExtendWith(OutputCaptureExtension.class)
class OrderFailedQueueAlertTaskTest {

    @Test
    void logsErrorWhenFailedQueueHasPendingMessages(CapturedOutput output) {
        AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
        OrderRabbitProperties properties = new OrderRabbitProperties();
        when(amqpAdmin.getQueueInfo("mall.order.failed.queue"))
                .thenReturn(new QueueInformation("mall.order.failed.queue", 3, 0));

        new OrderFailedQueueAlertTask(amqpAdmin, properties).alertWhenFailedMessagesExist();

        assertThat(output)
                .contains("order timeout failed queue has pending messages")
                .contains("queue=mall.order.failed.queue")
                .contains("messageCount=3");
    }

    @Test
    void doesNotLogErrorWhenFailedQueueIsEmpty(CapturedOutput output) {
        AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
        OrderRabbitProperties properties = new OrderRabbitProperties();
        when(amqpAdmin.getQueueInfo("mall.order.failed.queue"))
                .thenReturn(new QueueInformation("mall.order.failed.queue", 0, 0));

        new OrderFailedQueueAlertTask(amqpAdmin, properties).alertWhenFailedMessagesExist();

        assertThat(output).doesNotContain("order timeout failed queue has pending messages");
    }
}
