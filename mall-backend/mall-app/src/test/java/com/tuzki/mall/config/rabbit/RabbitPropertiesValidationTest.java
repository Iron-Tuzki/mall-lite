package com.tuzki.mall.config.rabbit;

import com.tuzki.mall.product.config.ProductHotProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RabbitMQ 和商品热点配置属性校验测试，验证启动期能够发现关键配置错误。
 */
class RabbitPropertiesValidationTest {

    @Test
    void cartRabbitPropertiesRejectBlankExchange() {
        CartRabbitProperties properties = new CartRabbitProperties();
        properties.setChangeExchange(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("购物车变更交换机不能为空");
    }

    @Test
    void cartRabbitPropertiesRejectInvalidConfirmTimeout() {
        CartRabbitProperties properties = new CartRabbitProperties();
        properties.setConfirmTimeoutSeconds(0);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("购物车消息 Confirm 超时时间必须大于 0");
    }

    @Test
    void couponRewardRabbitPropertiesRejectBlankQueue() {
        CouponRewardRabbitProperties properties = new CouponRewardRabbitProperties();
        properties.setCouponRewardQueue("");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("优惠券奖励队列不能为空");
    }

    @Test
    void couponRewardRabbitPropertiesRejectInvalidTimeout() {
        CouponRewardRabbitProperties properties = new CouponRewardRabbitProperties();
        properties.setTimeoutMinutes(-1);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("优惠券奖励超时时间必须大于 0");
    }

    @Test
    void orderRabbitPropertiesRejectBlankRoutingKey() {
        OrderRabbitProperties properties = new OrderRabbitProperties();
        properties.setCancelRoutingKey(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单取消路由键不能为空");
    }

    @Test
    void orderRabbitPropertiesRejectInvalidTimeout() {
        OrderRabbitProperties properties = new OrderRabbitProperties();
        properties.setTimeoutMinutes(null);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单超时时间必须大于 0");
    }

    @Test
    void productHotRabbitPropertiesRejectBlankFailedQueue() {
        ProductHotRabbitProperties properties = new ProductHotRabbitProperties();
        properties.setFailedQueue(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品热点失败队列不能为空");
    }

    @Test
    void productHotRabbitPropertiesRejectInvalidConfirmTimeout() {
        ProductHotRabbitProperties properties = new ProductHotRabbitProperties();
        properties.setConfirmTimeoutSeconds(null);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品热点消息 Confirm 超时时间必须大于 0");
    }

    @Test
    void productHotPropertiesRejectBucketTtlShorterThanWindow() {
        ProductHotProperties properties = new ProductHotProperties();
        properties.setWindowHours(24);
        properties.setBucketTtlHours(23);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("热门商品小时桶过期时间不能小于统计窗口小时数");
    }

    @Test
    void defaultPropertiesPassValidation() {
        assertThatCode(new CartRabbitProperties()::validate).doesNotThrowAnyException();
        assertThatCode(new CouponRewardRabbitProperties()::validate).doesNotThrowAnyException();
        assertThatCode(new OrderRabbitProperties()::validate).doesNotThrowAnyException();
        assertThatCode(new ProductHotRabbitProperties()::validate).doesNotThrowAnyException();
        assertThatCode(new ProductHotProperties()::validate).doesNotThrowAnyException();
    }
}
