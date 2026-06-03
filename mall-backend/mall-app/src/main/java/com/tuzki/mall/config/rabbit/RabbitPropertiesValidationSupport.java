package com.tuzki.mall.config.rabbit;

import org.springframework.util.StringUtils;

/**
 * RabbitMQ 配置属性校验工具，统一检查交换机、队列、路由键和超时时间等基础配置。
 */
final class RabbitPropertiesValidationSupport {

    private RabbitPropertiesValidationSupport() {
    }

    static void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
