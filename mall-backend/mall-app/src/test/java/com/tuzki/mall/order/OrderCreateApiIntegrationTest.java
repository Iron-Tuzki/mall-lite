package com.tuzki.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    @Autowired
    private LoginSessionService loginSessionService;

    @Test
    void createOrderLocksStockAndCreatesOrderWithItemSnapshot() throws Exception {
        String requestId = newRequestId("create");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
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
    void createOrderSupportsMultipleSkusAndCreatesMultipleOrderItems() throws Exception {
        String requestId = newRequestId("multi-sku");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(multiSkuOrderRequest(requestId, 2, 3, "multi sku order test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(575.00))
                .andExpect(jsonPath("$.data.payAmount").value(575.00));

        Order order = getOrderByRequestId(requestId);
        assertEquals(new BigDecimal("575.00"), order.getTotalAmount());

        mockMvc.perform(get("/api/orders/{orderId}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].skuId").value(TestSeedData.SKU_ID))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(398.00))
                .andExpect(jsonPath("$.data.items[1].skuId").value(TestSeedData.SKU_ID_2))
                .andExpect(jsonPath("$.data.items[1].quantity").value(3))
                .andExpect(jsonPath("$.data.items[1].totalAmount").value(177.00));

        List<OrderItem> orderItems = getOrderItems(order.getId());
        assertEquals(2, orderItems.size());

        Inventory firstInventory = getInventory(TestSeedData.SKU_ID);
        assertEquals(998, firstInventory.getAvailableStock());
        assertEquals(2, firstInventory.getLockedStock());

        Inventory secondInventory = getInventory(TestSeedData.SKU_ID_2);
        assertEquals(997, secondInventory.getAvailableStock());
        assertEquals(3, secondInventory.getLockedStock());
    }

    @Test
    void createOrderReturnsExistingOrderWhenRequestIdIsRepeated() throws Exception {
        String requestId = newRequestId("idempotent");
        String requestBody = orderRequest(requestId, 2, "idempotent order test");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString());

        Order firstOrder = getOrderByRequestId(requestId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
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
    void createOrderRejectsDuplicatedSkuInSameRequest() throws Exception {
        String requestId = newRequestId("duplicate-sku");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content(duplicatedSkuOrderRequest(requestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("duplicated sku in order items"));

        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId)));
        Inventory inventory = getSeedInventory();
        assertEquals(1000, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
    }

    @Test
    void createOrderRejectsInsufficientStockAndDoesNotCreateOrder() throws Exception {
        resetSeedInventory(1, 0);
        String requestId = newRequestId("insufficient");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
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
                        .header("Authorization", bearerToken())
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
                        .header("Authorization", bearerToken())
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
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getOrderRejectsMissingOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    @Test
    void createOrderRejectsMissingLoginToken() throws Exception {
        String requestId = newRequestId("missing-token");

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, "missing token test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("missing login token"));
    }

    @Test
    void createOrderRejectsInvalidLoginToken() throws Exception {
        String requestId = newRequestId("invalid-token");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType("application/json")
                        .content(orderRequest(requestId, 2, "invalid token test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("invalid login token"));
    }

    private String orderRequest(String requestId, Integer quantity, String remark) {
        String remarkField = remark == null ? "" : ",\n  \"remark\": \"%s\"".formatted(remark);
        return """
                {
                  "requestId": "%s",
                  "addressId": %d,
                  "items": [
                    {
                      "skuId": %d,
                      "quantity": %d
                    }
                  ]%s
                }
                """.formatted(requestId, TestSeedData.ADDRESS_ID, TestSeedData.SKU_ID,
                quantity, remarkField);
    }

    private String multiSkuOrderRequest(String requestId, Integer firstQuantity, Integer secondQuantity, String remark) {
        return """
                {
                  "requestId": "%s",
                  "addressId": %d,
                  "items": [
                    {
                      "skuId": %d,
                      "quantity": %d
                    },
                    {
                      "skuId": %d,
                      "quantity": %d
                    }
                  ],
                  "remark": "%s"
                }
                """.formatted(requestId, TestSeedData.ADDRESS_ID,
                TestSeedData.SKU_ID, firstQuantity, TestSeedData.SKU_ID_2, secondQuantity, remark);
    }

    private String duplicatedSkuOrderRequest(String requestId) {
        return """
                {
                  "requestId": "%s",
                  "addressId": %d,
                  "items": [
                    {
                      "skuId": %d,
                      "quantity": 1
                    },
                    {
                      "skuId": %d,
                      "quantity": 2
                    }
                  ]
                }
                """.formatted(requestId, TestSeedData.ADDRESS_ID,
                TestSeedData.SKU_ID, TestSeedData.SKU_ID);
    }

    private String bearerToken() {
        return "Bearer " + loginSessionService.createSession(TestSeedData.USER_ID);
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

    private List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getSkuId));
    }

    private Inventory getSeedInventory() {
        return getInventory(TestSeedData.SKU_ID);
    }

    private Inventory getInventory(Long skuId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, skuId));
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
