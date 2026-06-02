package com.tuzki.mall.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration for scanning mapper interfaces.
 */
@Configuration
@MapperScan({
        "com.tuzki.mall.user.mapper",
        "com.tuzki.mall.product.mapper",
        "com.tuzki.mall.inventory.mapper",
        "com.tuzki.mall.order.mapper",
        "com.tuzki.mall.cart.mapper",
        "com.tuzki.mall.payment.mapper"
})
public class MyBatisPlusConfig {
}
