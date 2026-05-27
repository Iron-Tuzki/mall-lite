package com.tuzki.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 订单 Mapper 集成测试，用于验证订单主表和订单明细表的基础读写以及订单状态条件更新能力。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Transactional
class OrderMapperIntegrationTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    void orderAndOrderItemMappersCanInsertAndQueryTradeTables() {
        Order order = new Order();
        order.setOrderNo("ORDER-" + System.nanoTime());
        order.setRequestId("REQ-MAPPER-" + System.nanoTime());
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setPayAmount(new BigDecimal("199.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setStatus(10);
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800000000");
        order.setReceiverProvince("广东省");
        order.setReceiverCity("深圳市");
        order.setReceiverDistrict("南山区");
        order.setReceiverDetailAddress("科技园测试地址");
        order.setRemark("mapper integration test");
        order.setDeleted(0);

        orderMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setOrderNo(order.getOrderNo());
        orderItem.setProductId(1001L);
        orderItem.setSkuId(2001L);
        orderItem.setSkuCode("SKU-2001");
        orderItem.setProductName("测试商品");
        orderItem.setSkuName("测试 SKU");
        orderItem.setSpecData("{\"color\":\"black\"}");
        orderItem.setMainImageUrl("https://example.com/product.png");
        orderItem.setUnitPrice(new BigDecimal("199.00"));
        orderItem.setQuantity(1);
        orderItem.setTotalAmount(new BigDecimal("199.00"));
        orderItem.setDeleted(0);

        orderItemMapper.insert(orderItem);

        assertNotNull(order.getId());
        assertNotNull(orderItem.getId());
        assertEquals(1L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, order.getOrderNo())));
        assertEquals(1L, orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())));
    }

    @Test
    void onlyPendingOrderCanBeMarkedAsPaidOnce() {
        Order order = buildPendingOrder("ORDER-CAS-");
        orderMapper.insert(order);

        int firstAffectedRows = orderMapper.markPaidIfPending(order.getId(), LocalDateTime.now());
        int secondAffectedRows = orderMapper.markPaidIfPending(order.getId(), LocalDateTime.now());

        Order currentOrder = orderMapper.selectById(order.getId());
        assertEquals(1, firstAffectedRows);
        assertEquals(0, secondAffectedRows);
        assertEquals(20, currentOrder.getStatus());
        assertNotNull(currentOrder.getPayTime());
    }

    private Order buildPendingOrder(String orderNoPrefix) {
        Order order = new Order();
        order.setOrderNo(orderNoPrefix + System.nanoTime());
        order.setRequestId("REQ-CAS-" + System.nanoTime());
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setPayAmount(new BigDecimal("199.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setStatus(10);
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800000000");
        order.setReceiverProvince("广东省");
        order.setReceiverCity("深圳市");
        order.setReceiverDistrict("南山区");
        order.setReceiverDetailAddress("科技园测试地址");
        order.setRemark("order cas test");
        order.setDeleted(0);
        return order;
    }
}
