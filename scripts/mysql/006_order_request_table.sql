USE mall_lite;

CREATE TABLE IF NOT EXISTS oms_order_request
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单请求ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    request_id  VARCHAR(64)     NOT NULL COMMENT '下单请求幂等号',
    order_id    BIGINT UNSIGNED NULL COMMENT '创建成功后的订单ID',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '请求状态：10处理中，20成功',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_request (user_id, request_id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单请求幂等表';
