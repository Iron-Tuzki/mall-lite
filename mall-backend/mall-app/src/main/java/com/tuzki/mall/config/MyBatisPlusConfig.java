package com.tuzki.mall.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration for scanning mapper interfaces.
 */
@Configuration
@MapperScan({
        "com.tuzki.mall.user.mapper",
        "com.tuzki.mall.product.mapper"
})
public class MyBatisPlusConfig {
}
