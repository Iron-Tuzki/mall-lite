package com.tuzki.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mall Lite application startup class.
 *
 * <p>This class bootstraps the lightweight mall backend and loads all business modules
 * under the {@code com.tuzki.mall} package.</p>
 */
@SpringBootApplication
public class MallLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallLiteApplication.class, args);
    }
}
