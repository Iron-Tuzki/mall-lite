USE mall_lite;

DROP TABLE IF EXISTS sms_seckill_sku;
DROP TABLE IF EXISTS sms_seckill_activity;

CREATE TABLE sms_seckill_activity
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
    name        VARCHAR(128)    NOT NULL COMMENT '活动名称',
    start_time  DATETIME        NOT NULL COMMENT '活动开始时间',
    end_time    DATETIME        NOT NULL COMMENT '活动结束时间',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark      VARCHAR(255)    NULL COMMENT '活动备注',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_time_status (start_time, end_time, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动表';

CREATE TABLE sms_seckill_sku
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动商品ID',
    activity_id    BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    sku_id         BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    seckill_price  DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价',
    stock_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '活动库存',
    limit_quantity INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    sort           INT             NOT NULL DEFAULT 0 COMMENT '排序值',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_sku (activity_id, sku_id),
    KEY idx_activity_status_sort (activity_id, status, sort),
    KEY idx_sku_id (sku_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动商品表';
