package com.tuzki.mall.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.dto.OrderCreateItemRequest;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.entity.OrderRequest;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.mapper.OrderRequestMapper;
import com.tuzki.mall.order.message.OrderTimeoutMessageSender;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.payment.dto.PaymentCallbackRequest;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.enums.MockPaymentResult;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 发起支付并发幂等集成测试，验证同一订单并发发起支付时只会创建一条待支付流水。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class PaymentPayConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderRequestMapper orderRequestMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @MockitoBean
    private OrderTimeoutMessageSender orderTimeoutMessageSender;

    @Test
    void concurrentPayOrderOnlyCreatesOnePendingPayment() throws Exception {
        String requestId = "REQ-pay-concurrent-" + System.nanoTime();
        resetSeedInventory(1000, 0);
        OrderCreateVO orderCreateVO = orderService.createOrder(TestSeedData.USER_ID, buildCreateRequest(requestId));
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            try {
                Future<PaymentPayVO> firstFuture = executorService.submit(payOrderTask(startLatch, orderCreateVO.getOrderId()));
                Future<PaymentPayVO> secondFuture = executorService.submit(payOrderTask(startLatch, orderCreateVO.getOrderId()));

                startLatch.countDown();

                PaymentPayVO firstResult = firstFuture.get();
                PaymentPayVO secondResult = secondFuture.get();

                assertEquals(firstResult.getPaymentNo(), secondResult.getPaymentNo());
                assertEquals(1L, paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderCreateVO.getOrderId())));
            } finally {
                executorService.shutdownNow();
            }
        } finally {
            cleanPaymentData(orderCreateVO.getOrderId());
            cleanOrderData(requestId, orderCreateVO.getOrderId());
            resetSeedInventory(1000, 0);
        }
    }

    @Test
    void concurrentSuccessCallbacksOnlyConfirmOrderAndDeductStockOnce() throws Exception {
        String requestId = "REQ-callback-concurrent-" + System.nanoTime();
        resetSeedInventory(1000, 0);
        OrderCreateVO orderCreateVO = orderService.createOrder(TestSeedData.USER_ID, buildCreateRequest(requestId));
        PaymentPayVO pendingPayment = paymentService.payOrder(orderCreateVO.getOrderId());
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            try {
                Future<PaymentPayVO> firstFuture = executorService.submit(callbackTask(startLatch, pendingPayment.getPaymentNo()));
                Future<PaymentPayVO> secondFuture = executorService.submit(callbackTask(startLatch, pendingPayment.getPaymentNo()));

                startLatch.countDown();

                PaymentPayVO firstResult = firstFuture.get(5, TimeUnit.SECONDS);
                PaymentPayVO secondResult = secondFuture.get(5, TimeUnit.SECONDS);

                assertEquals(20, firstResult.getOrderStatus());
                assertEquals(20, firstResult.getPaymentStatus());
                assertEquals(20, secondResult.getOrderStatus());
                assertEquals(20, secondResult.getPaymentStatus());

                Inventory inventory = getSeedInventory();
                assertEquals(998, inventory.getAvailableStock());
                assertEquals(0, inventory.getLockedStock());
                assertEquals(2, inventory.getVersion());

                Order paidOrder = orderMapper.selectById(orderCreateVO.getOrderId());
                assertEquals(20, paidOrder.getStatus());
            } finally {
                executorService.shutdownNow();
            }
        } finally {
            cleanPaymentData(orderCreateVO.getOrderId());
            cleanOrderData(requestId, orderCreateVO.getOrderId());
            resetSeedInventory(1000, 0);
        }
    }

    private Callable<PaymentPayVO> payOrderTask(CountDownLatch startLatch, Long orderId) {
        return () -> {
            startLatch.await();
            return paymentService.payOrder(orderId);
        };
    }

    private Callable<PaymentPayVO> callbackTask(CountDownLatch startLatch, String paymentNo) {
        return () -> {
            startLatch.await();
            PaymentCallbackRequest request = new PaymentCallbackRequest();
            request.setMockResult(MockPaymentResult.SUCCESS);
            return paymentService.handleCallback(paymentNo, request);
        };
    }

    private OrderCreateRequest buildCreateRequest(String requestId) {
        OrderCreateItemRequest itemRequest = new OrderCreateItemRequest();
        itemRequest.setSkuId(TestSeedData.SKU_ID);
        itemRequest.setQuantity(2);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setRequestId(requestId);
        request.setAddressId(TestSeedData.ADDRESS_ID);
        request.setItems(List.of(itemRequest));
        request.setRemark("concurrent payment test");
        return request;
    }

    private Inventory getSeedInventory() {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, TestSeedData.SKU_ID));
    }

    private void resetSeedInventory(Integer availableStock, Integer lockedStock) {
        Inventory inventory = getSeedInventory();
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventoryMapper.updateById(inventory);
    }

    private void cleanPaymentData(Long orderId) {
        paymentMapper.delete(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
    }

    private void cleanOrderData(String requestId, Long orderId) {
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        orderMapper.deleteById(orderId);
        orderRequestMapper.delete(new LambdaQueryWrapper<OrderRequest>()
                .eq(OrderRequest::getUserId, TestSeedData.USER_ID)
                .eq(OrderRequest::getRequestId, requestId));
    }
}
