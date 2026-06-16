package com.tuzki.mall.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.outbox.entity.OutboxMessage;
import com.tuzki.mall.outbox.enums.OutboxMessageStatus;
import com.tuzki.mall.outbox.mapper.OutboxMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 消息服务，负责先记录本地消息，再尝试投递 RabbitMQ，并支持定时补发失败消息。
 */
@Service
public class OutboxMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxMessageService.class);

    private static final int NOT_DELETED = 0;

    private static final int MAX_ERROR_LENGTH = 512;

    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);

    private final OutboxMessageMapper outboxMessageMapper;

    private final OutboxRabbitPublisher outboxRabbitPublisher;

    private final ObjectMapper objectMapper;

    public OutboxMessageService(OutboxMessageMapper outboxMessageMapper,
                                OutboxRabbitPublisher outboxRabbitPublisher,
                                ObjectMapper objectMapper) {
        this.outboxMessageMapper = outboxMessageMapper;
        this.outboxRabbitPublisher = outboxRabbitPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建待发送消息并立即尝试投递一次。投递失败不会删除本地消息，由定时任务继续补发。
     *
     * @param aggregateType 聚合类型，例如 CART
     * @param aggregateId 聚合 ID，例如 userId:skuId:version
     * @param exchangeName RabbitMQ 交换机
     * @param routingKey RabbitMQ 路由键
     * @param payload 消息体对象
     */
    public void createAndPublish(String aggregateType,
                                 String aggregateId,
                                 String exchangeName,
                                 String routingKey,
                                 Object payload) {
        OutboxMessage message = buildMessage(aggregateType, aggregateId, exchangeName, routingKey, payload);
        outboxMessageMapper.insert(message);
        publishAndMark(message);
    }

    /**
     * 批量补发已经到达重试时间的待发送消息。
     *
     * @param batchSize 单次补发最大消息数
     */
    public void relayDueMessages(Integer batchSize) {
        List<OutboxMessage> messages = outboxMessageMapper.listDueMessages(LocalDateTime.now(), batchSize);
        for (OutboxMessage message : messages) {
            publishAndMark(message);
        }
    }

    private OutboxMessage buildMessage(String aggregateType,
                                       String aggregateId,
                                       String exchangeName,
                                       String routingKey,
                                       Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setExchangeName(exchangeName);
        message.setRoutingKey(routingKey);
        message.setPayloadType(payload.getClass().getName());
        message.setPayload(writePayload(payload));
        message.setStatus(OutboxMessageStatus.PENDING.getCode());
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setDeleted(NOT_DELETED);
        return message;
    }

    private void publishAndMark(OutboxMessage message) {
        try {
            Object payload = readPayload(message);
            // 发布消息
            outboxRabbitPublisher.publish(message.getExchangeName(), message.getRoutingKey(), payload);
            // 标记为已发送
            outboxMessageMapper.markSent(message.getMessageId());
        } catch (RuntimeException exception) {
            markFailed(message, exception);
        }
    }

    private void markFailed(OutboxMessage message, RuntimeException exception) {
        LOGGER.warn("outbox message publish failed, messageId={}", message.getMessageId(), exception);
        outboxMessageMapper.markFailed(
                message.getMessageId(),
                LocalDateTime.now().plus(RETRY_DELAY),
                abbreviateError(exception)
        );
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("write outbox payload failed", exception);
        }
    }

    private Object readPayload(OutboxMessage message) {
        try {
            return objectMapper.readValue(message.getPayload(), Class.forName(message.getPayloadType()));
        } catch (JsonProcessingException | ClassNotFoundException exception) {
            throw new IllegalStateException("read outbox payload failed", exception);
        }
    }

    private String abbreviateError(RuntimeException exception) {
        String message = exception.getMessage();
        String error = message == null || message.isBlank()
                ? exception.getClass().getName()
                : message;
        if (error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH);
    }
}
