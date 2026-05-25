package com.tuzki.mall.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import com.tuzki.mall.user.service.LoginSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付接口集成测试，基于固定测试种子数据验证发起支付、回调幂等和库存确认扣减。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private LoginSessionService loginSessionService;

    @Test
    void successfulCallbackUpdatesPaymentOrderAndDeductsLockedStock() throws Exception {
        Order order = createOrder(2, "payment order test");

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNo").isString())
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderStatus").value(10))
                .andExpect(jsonPath("$.data.paymentStatus").value(10))
                .andExpect(jsonPath("$.data.payAmount").value(398.00));

        Payment payment = getPayment(order.getId());
        assertEquals(order.getOrderNo(), payment.getOrderNo());
        assertEquals(TestSeedData.USER_ID, payment.getUserId());
        assertEquals(new BigDecimal("398.00"), payment.getPayAmount());
        assertEquals(10, payment.getStatus());

        mockMvc.perform(successCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNo").value(payment.getPaymentNo()))
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderStatus").value(20))
                .andExpect(jsonPath("$.data.paymentStatus").value(20))
                .andExpect(jsonPath("$.data.payAmount").value(398.00));

        Order paidOrder = orderMapper.selectById(order.getId());
        assertEquals(20, paidOrder.getStatus());
        assertNotNull(paidOrder.getPayTime());

        Payment successfulPayment = paymentMapper.selectById(payment.getId());
        assertEquals(20, successfulPayment.getStatus());
        assertNotNull(successfulPayment.getPayTime());

        mockMvc.perform(successCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value(20))
                .andExpect(jsonPath("$.data.paymentStatus").value(20));

        Inventory inventory = getSeedInventory();
        assertEquals(998, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, inventory.getVersion());
    }

    @Test
    void failedCallbackOnlyMarksPaymentFailedAndKeepsOrderPending() throws Exception {
        Order order = createOrder(2, "failed payment order test");

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Payment payment = getPayment(order.getId());

        mockMvc.perform(failedCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value(10))
                .andExpect(jsonPath("$.data.paymentStatus").value(30));

        mockMvc.perform(failedCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value(10))
                .andExpect(jsonPath("$.data.paymentStatus").value(30));

        Order pendingOrder = orderMapper.selectById(order.getId());
        assertEquals(10, pendingOrder.getStatus());

        Payment failedPayment = paymentMapper.selectById(payment.getId());
        assertEquals(30, failedPayment.getStatus());

        Inventory inventory = getSeedInventory();
        assertEquals(998, inventory.getAvailableStock());
        assertEquals(2, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void payOrderRejectsAlreadyPaidOrder() throws Exception {
        Order order = createOrder(1, "double payment order test");

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Payment payment = getPayment(order.getId());

        mockMvc.perform(successCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("paid order cannot be paid"));
    }

    @Test
    void payOrderReturnsExistingPendingPaymentBeforeCallback() throws Exception {
        Order order = createOrder(1, "pending payment idempotent test");

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value(10))
                .andExpect(jsonPath("$.data.paymentStatus").value(10));

        Long paymentCount = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, order.getId()));
        assertEquals(1L, paymentCount);
    }

    @Test
    void terminalPaymentIgnoresConflictingCallbackResult() throws Exception {
        Order order = createOrder(1, "terminal payment callback test");

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Payment payment = getPayment(order.getId());

        mockMvc.perform(successCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(failedCallback(payment.getPaymentNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value(20))
                .andExpect(jsonPath("$.data.paymentStatus").value(20));

        Inventory inventory = getSeedInventory();
        assertEquals(999, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, inventory.getVersion());
    }

    private Order createOrder(Integer quantity, String remark) throws Exception {
        String requestId = "REQ-payment-" + System.nanoTime();
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearerToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestId": "%s",
                                  "addressId": %d,
                                  "items": [
                                    {
                                      "skuId": %d,
                                      "quantity": %d
                                    }
                                  ],
                                  "remark": "%s"
                                }
                                """.formatted(requestId, TestSeedData.ADDRESS_ID,
                                TestSeedData.SKU_ID, quantity, remark)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId));
        assertNotNull(order);
        return order;
    }

    private String bearerToken() {
        return "Bearer " + loginSessionService.createSession(TestSeedData.USER_ID);
    }

    private Payment getPayment(Long orderId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        assertNotNull(payment);
        return payment;
    }

    private Inventory getSeedInventory() {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, TestSeedData.SKU_ID));
        assertNotNull(inventory);
        return inventory;
    }

    private org.springframework.test.web.servlet.RequestBuilder successCallback(String paymentNo) {
        return post("/api/payments/{paymentNo}/callback", paymentNo)
                .contentType("application/json")
                .content("""
                        {
                          "mockResult": "SUCCESS"
                        }
                        """);
    }

    private org.springframework.test.web.servlet.RequestBuilder failedCallback(String paymentNo) {
        return post("/api/payments/{paymentNo}/callback", paymentNo)
                .contentType("application/json")
                .content("""
                        {
                          "mockResult": "FAILED"
                        }
                        """);
    }
}
