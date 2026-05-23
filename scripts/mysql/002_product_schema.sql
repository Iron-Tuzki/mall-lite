USE mall_lite;

DROP TABLE IF EXISTS pms_sku;
DROP TABLE IF EXISTS pms_product;
DROP TABLE IF EXISTS pms_category;

CREATE TABLE pms_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示一级分类',
    name        VARCHAR(64)     NOT NULL COMMENT '分类名称',
    level       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '分类层级',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    icon_url    VARCHAR(255)    NULL COMMENT '分类图标',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_status_sort (status, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品分类表';

CREATE TABLE pms_product
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    category_id    BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    product_code   VARCHAR(64)     NOT NULL COMMENT '商品编码',
    name           VARCHAR(128)    NOT NULL COMMENT '商品名称',
    subtitle       VARCHAR(255)    NULL COMMENT '商品副标题',
    main_image_url VARCHAR(255)    NULL COMMENT '商品主图',
    description    TEXT            NULL COMMENT '商品描述',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1上架，0下架',
    sort           INT             NOT NULL DEFAULT 0 COMMENT '排序值',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category_status (category_id, status),
    KEY idx_status_sort (status, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品SPU表';

CREATE TABLE pms_sku
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    sku_code       VARCHAR(64)     NOT NULL COMMENT 'SKU编码',
    sku_name       VARCHAR(128)    NOT NULL COMMENT 'SKU名称',
    spec_data      JSON            NULL COMMENT '规格数据，例如颜色、容量',
    price          DECIMAL(10, 2)  NOT NULL COMMENT '销售价',
    original_price DECIMAL(10, 2)  NULL COMMENT '原价',
    main_image_url VARCHAR(255)    NULL COMMENT 'SKU主图',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1可售，0不可售',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_status (product_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品SKU表';
