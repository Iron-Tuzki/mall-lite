USE mall_lite;

DROP TABLE IF EXISTS sms_seckill_request;
DROP TABLE IF EXISTS sms_seckill_sku;
DROP TABLE IF EXISTS sms_seckill_activity;

CREATE TABLE sms_seckill_activity
(
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
    name        VARCHAR(128)     NOT NULL COMMENT '活动名称',
    start_time  DATETIME         NOT NULL COMMENT '活动开始时间',
    end_time    DATETIME         NOT NULL COMMENT '活动结束时间',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark      VARCHAR(255)     NULL COMMENT '活动备注',
    create_time DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_time_status (start_time, end_time, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动表';

CREATE TABLE sms_seckill_sku
(
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '秒杀活动商品ID',
    activity_id    BIGINT UNSIGNED  NOT NULL COMMENT '秒杀活动ID',
    sku_id         BIGINT UNSIGNED  NOT NULL COMMENT 'SKU ID',
    seckill_price  DECIMAL(10, 2)   NOT NULL COMMENT '秒杀价',
    stock_count    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '活动库存',
    limit_quantity INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    sort           INT              NOT NULL DEFAULT 0 COMMENT '排序值',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_sku (activity_id, sku_id),
    KEY idx_activity_status_sort (activity_id, status, sort),
    KEY idx_sku_id (sku_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动商品表';

CREATE TABLE sms_seckill_request
(
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '秒杀请求流水ID',
    request_id     VARCHAR(64)      NOT NULL COMMENT '秒杀请求幂等号',
    user_id        BIGINT UNSIGNED  NOT NULL COMMENT '用户ID',
    activity_id    BIGINT UNSIGNED  NOT NULL COMMENT '秒杀活动ID',
    seckill_sku_id BIGINT UNSIGNED  NOT NULL COMMENT '秒杀活动商品ID',
    sku_id         BIGINT UNSIGNED  NOT NULL COMMENT 'SKU ID',
    quantity       INT UNSIGNED     NOT NULL COMMENT '购买数量',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '状态：10初始化，20预扣成功，30订单创建成功，40失败，50已补偿',
    order_id       BIGINT UNSIGNED  NULL COMMENT '订单ID',
    fail_reason    VARCHAR(255)     NULL COMMENT '失败原因',
    retry_count    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '补偿重试次数',
    request_ip     VARCHAR(64)      NULL COMMENT '请求IP',
    create_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku_request (user_id, seckill_sku_id, request_id),
    KEY idx_status_update_time (status, update_time),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀请求流水表';

-- 维护秒杀活动测试数据
DELETE FROM sms_seckill_request
WHERE activity_id IN (
    SELECT id
    FROM sms_seckill_activity
    WHERE name IN ('今日限时秒杀', '明日预热秒杀')
);
DELETE FROM sms_seckill_sku
WHERE activity_id IN (
    SELECT id
    FROM sms_seckill_activity
    WHERE name IN ('今日限时秒杀', '明日预热秒杀')
);
DELETE FROM sms_seckill_activity
WHERE name IN ('今日限时秒杀', '明日预热秒杀');

INSERT INTO sms_seckill_activity (name, start_time, end_time, status, remark)
VALUES ('今日限时秒杀', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1,
        '用于前台秒杀页面联调的进行中活动');
SET @current_seckill_activity_id = LAST_INSERT_ID();

INSERT INTO sms_seckill_sku (activity_id, sku_id, seckill_price, stock_count, limit_quantity, sort, status)
SELECT @current_seckill_activity_id,
       s.id,
       GREATEST(1.00, ROUND(s.price * 0.70, 2)),
       2,
       1,
       ROW_NUMBER() OVER (ORDER BY s.id),
       1
FROM pms_sku s
WHERE s.status = 1
  AND s.deleted = 0
ORDER BY s.id
LIMIT 3;

INSERT INTO sms_seckill_activity (name, start_time, end_time, status, remark)
VALUES ('明日预热秒杀', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 1,
        '用于验证定时预热窗口的即将开始活动');
SET @upcoming_seckill_activity_id = LAST_INSERT_ID();

INSERT INTO sms_seckill_sku (activity_id, sku_id, seckill_price, stock_count, limit_quantity, sort, status)
SELECT @upcoming_seckill_activity_id,
       s.id,
       GREATEST(1.00, ROUND(s.price * 0.60, 2)),
       10,
       1,
       ROW_NUMBER() OVER (ORDER BY s.id),
       1
FROM pms_sku s
WHERE s.status = 1
  AND s.deleted = 0
ORDER BY s.id
LIMIT 2;
