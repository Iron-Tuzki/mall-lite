USE mall_lite;

DROP TABLE IF EXISTS pms_payment;
DROP TABLE IF EXISTS oms_order_item;
DROP TABLE IF EXISTS oms_order_request;
DROP TABLE IF EXISTS oms_order;
DROP TABLE IF EXISTS ims_inventory;

CREATE TABLE ims_inventory
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    sku_id          BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    available_stock INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '锁定库存',
    version         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '库存表';

CREATE TABLE oms_order
(
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no                VARCHAR(64)     NOT NULL COMMENT '订单号',
    request_id              VARCHAR(64)     NULL COMMENT '下单请求幂等号',
    user_id                 BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    total_amount            DECIMAL(10, 2)  NOT NULL COMMENT '订单总金额',
    pay_amount              DECIMAL(10, 2)  NOT NULL COMMENT '实付金额',
    freight_amount          DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '运费',
    status                  TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '订单状态：10待支付，20已支付，30已取消，40已完成',
    cancel_type             TINYINT UNSIGNED NULL COMMENT '取消类型：10用户主动取消，20超时自动取消',
    cancel_reason           VARCHAR(100)    NULL COMMENT '取消原因',
    receiver_name           VARCHAR(64)     NOT NULL COMMENT '收货人姓名快照',
    receiver_phone          VARCHAR(20)     NOT NULL COMMENT '收货人手机号快照',
    receiver_province       VARCHAR(64)     NOT NULL COMMENT '省快照',
    receiver_city           VARCHAR(64)     NOT NULL COMMENT '市快照',
    receiver_district       VARCHAR(64)     NOT NULL COMMENT '区/县快照',
    receiver_detail_address VARCHAR(255)    NOT NULL COMMENT '详细地址快照',
    remark                  VARCHAR(255)    NULL COMMENT '用户备注',
    pay_time                DATETIME        NULL COMMENT '支付时间',
    cancel_time             DATETIME        NULL COMMENT '取消时间',
    finish_time             DATETIME        NULL COMMENT '完成时间',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_request (user_id, request_id),
    KEY idx_user_status (user_id, status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单主表';

CREATE TABLE oms_order_request
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

CREATE TABLE oms_order_item
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id       BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no       VARCHAR(64)     NOT NULL COMMENT '订单号',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    sku_id         BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    sku_code       VARCHAR(64)     NOT NULL COMMENT 'SKU编码快照',
    product_name   VARCHAR(128)    NOT NULL COMMENT '商品名称快照',
    sku_name       VARCHAR(128)    NOT NULL COMMENT 'SKU名称快照',
    spec_data      JSON            NULL COMMENT '规格快照',
    main_image_url VARCHAR(255)    NULL COMMENT '商品图片快照',
    unit_price     DECIMAL(10, 2)  NOT NULL COMMENT '下单单价',
    quantity       INT UNSIGNED    NOT NULL COMMENT '购买数量',
    total_amount   DECIMAL(10, 2)  NOT NULL COMMENT '明细总金额',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_sku_id (sku_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单明细表';

CREATE TABLE pms_payment
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
    payment_no       VARCHAR(64)     NOT NULL COMMENT '支付流水号',
    order_id         BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no         VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    pay_channel      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '支付渠道：1模拟支付',
    pay_amount       DECIMAL(10, 2)  NOT NULL COMMENT '支付金额',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '支付状态：10待支付，20支付成功，30支付失败，40已关闭',
    pending_order_id BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN status = 10 AND deleted = 0 THEN order_id ELSE NULL END
    ) STORED COMMENT '待支付订单ID生成列，用于限制同一订单同一渠道只有一条待支付流水',
    pay_time         DATETIME        NULL COMMENT '支付成功时间',
    callback_content TEXT            NULL COMMENT '支付回调内容',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_pending_order_channel (pending_order_id, pay_channel),
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '支付流水表';
