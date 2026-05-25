package com.tuzki.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单创建接口集成测试，基于固定测试种子数据验证下单、幂等、取消和库存流转。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderCreateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    void createOrderLocksStockAndCreatesOrderWithItemSnapshot() throws Exception {
        String requestId = newRequestId("create");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, "please ship soon")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString())
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00))
                .andExpect(jsonPath("$.data.payAmount").value(398.00));

        Order order = getOrderByRequestId(requestId);
        assertEquals(new BigDecimal("398.00"), order.getTotalAmount());
        assertEquals(TestSeedData.RECEIVER_NAME, order.getReceiverName());
        assertEquals("Guangdong", order.getReceiverProvince());

        mockMvc.perform(get("/api/orders/{orderId}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderNo").value(order.getOrderNo()))
                .andExpect(jsonPath("$.data.userId").value(TestSeedData.USER_ID))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00))
                .andExpect(jsonPath("$.data.receiverName").value(TestSeedData.RECEIVER_NAME))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].skuId").value(TestSeedData.SKU_ID))
                .andExpect(jsonPath("$.data.items[0].productName").value(TestSeedData.PRODUCT_NAME))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        OrderItem orderItem = getOrderItem(order.getId());
        assertEquals(TestSeedData.SKU_ID, orderItem.getSkuId());
        assertEquals(TestSeedData.PRODUCT_NAME, orderItem.getProductName());
        assertEquals(TestSeedData.SKU_NAME, orderItem.getSkuName());
        assertEquals(new BigDecimal("199.00"), orderItem.getUnitPrice());
        assertEquals(2, orderItem.getQuantity());
        assertEquals(new BigDecimal("398.00"), orderItem.getTotalAmount());

        Inventory inventory = getSeedInventory();
        assertEquals(998, inventory.getAvailableStock());
        assertEquals(2, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void createOrderReturnsExistingOrderWhenRequestIdIsRepeated() throws Exception {
        String requestId = newRequestId("idempotent");
        String requestBody = orderRequest(requestId, 2, "idempotent order test");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString());

        Order firstOrder = getOrderByRequestId(requestId);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.data.orderNo").value(firstOrder.getOrderNo()))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00));

        assertEquals(1L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId)));
        assertEquals(1L, orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, firstOrder.getId())));

        Inventory inventory = getSeedInventory();
        assertEquals(998, inventory.getAvailableStock());
        assertEquals(2, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void createOrderRejectsInsufficientStockAndDoesNotCreateOrder() throws Exception {
        resetSeedInventory(1, 0);
        String requestId = newRequestId("insufficient");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("insufficient stock"));

        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId)));
        Inventory inventory = getSeedInventory();
        assertEquals(1, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
    }

    @Test
    void cancelPendingOrderReleasesLockedStock() throws Exception {
        String requestId = newRequestId("cancel");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, "cancel order test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = getOrderByRequestId(requestId);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order cancelledOrder = orderMapper.selectById(order.getId());
        assertEquals(30, cancelledOrder.getStatus());
        assertNotNull(cancelledOrder.getCancelTime());

        Inventory inventory = getSeedInventory();
        assertEquals(1000, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, inventory.getVersion());
    }

    @Test
    void cancelOrderRejectsAlreadyCancelledOrder() throws Exception {
        String requestId = newRequestId("double-cancel");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, "double cancel order test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = getOrderByRequestId(requestId);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("cancelled order cannot be cancelled"));
    }

    @Test
    void getOrderRejectsMissingOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    private String orderRequest(String requestId, Integer quantity, String remark) {
        String remarkField = remark == null ? "" : ",\n  \"remark\": \"%s\"".formatted(remark);
        return """
                {
                  "requestId": "%s",
                  "userId": %d,
                  "addressId": %d,
                  "skuId": %d,
                  "quantity": %d%s
                }
                """.formatted(requestId, TestSeedData.USER_ID, TestSeedData.ADDRESS_ID, TestSeedData.SKU_ID,
                quantity, remarkField);
    }

    private String newRequestId(String prefix) {
        return "REQ-" + prefix + "-" + System.nanoTime();
    }

    private Order getOrderByRequestId(String requestId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId));
        assertNotNull(order);
        return order;
    }

    private OrderItem getOrderItem(Long orderId) {
        OrderItem orderItem = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        assertNotNull(orderItem);
        return orderItem;
    }

    private Inventory getSeedInventory() {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, TestSeedData.SKU_ID));
        assertNotNull(inventory);
        return inventory;
    }

    private void resetSeedInventory(Integer availableStock, Integer lockedStock) {
        Inventory inventory = getSeedInventory();
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventoryMapper.updateById(inventory);
    }
}
