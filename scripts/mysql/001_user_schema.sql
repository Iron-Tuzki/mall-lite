CREATE DATABASE IF NOT EXISTS mall_lite
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE mall_lite;

DROP TABLE IF EXISTS ums_address;
DROP TABLE IF EXISTS ums_user;

CREATE TABLE ums_user
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(64)     NOT NULL COMMENT '用户名',
    password        VARCHAR(255)    NOT NULL COMMENT '密码摘要',
    nickname        VARCHAR(64)     NULL COMMENT '用户昵称',
    phone           VARCHAR(20)     NULL COMMENT '手机号',
    email           VARCHAR(128)    NULL COMMENT '邮箱',
    avatar_url      VARCHAR(255)    NULL COMMENT '头像地址',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户状态：1正常，0禁用',
    last_login_time DATETIME        NULL COMMENT '最近登录时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户基础信息表';

CREATE TABLE ums_address
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    receiver_name  VARCHAR(64)     NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20)     NOT NULL COMMENT '收货人手机号',
    province       VARCHAR(64)     NOT NULL COMMENT '省',
    city           VARCHAR(64)     NOT NULL COMMENT '市',
    district       VARCHAR(64)     NOT NULL COMMENT '区/县',
    detail_address VARCHAR(255)    NOT NULL COMMENT '详细地址',
    postal_code    VARCHAR(20)     NULL COMMENT '邮政编码',
    default_flag   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认地址：1是，0否',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_default (user_id, default_flag)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户收货地址表';
