package com.tuzki.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mall Lite 应用启动类，负责启动轻量商城后端并加载 {@code com.tuzki.mall} 包下的业务模块。
 */
@SpringBootApplication
@EnableScheduling
public class MallLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallLiteApplication.class, args);
    }
}
