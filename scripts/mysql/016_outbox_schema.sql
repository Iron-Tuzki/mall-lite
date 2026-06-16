USE mall_lite;

CREATE TABLE IF NOT EXISTS sys_outbox_message
(
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Outbox消息ID',
    message_id      VARCHAR(64)      NOT NULL COMMENT '业务消息唯一ID',
    aggregate_type  VARCHAR(64)      NOT NULL COMMENT '聚合类型，例如CART、ORDER',
    aggregate_id    VARCHAR(128)     NOT NULL COMMENT '聚合ID，例如userId:skuId:version',
    exchange_name   VARCHAR(128)     NOT NULL COMMENT 'RabbitMQ交换机名称',
    routing_key     VARCHAR(128)     NOT NULL COMMENT 'RabbitMQ路由键',
    payload_type    VARCHAR(256)     NOT NULL COMMENT '消息体Java类型',
    payload         JSON             NOT NULL COMMENT '消息体JSON',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待发送，1已发送，2发送失败',
    retry_count     INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    last_error      VARCHAR(512)     NULL COMMENT '最近一次失败原因',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry_time (status, next_retry_time),
    KEY idx_aggregate (aggregate_type, aggregate_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Outbox本地消息表';
