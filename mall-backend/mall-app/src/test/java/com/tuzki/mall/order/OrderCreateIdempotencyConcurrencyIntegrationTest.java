package com.tuzki.mall.order;

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
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.message.OrderTimeoutMessageSender;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 下单幂等并发集成测试，验证重复 requestId 会先被订单请求表拦截，不会重复进入锁库存流程。
 */
@SpringBootTest
class OrderCreateIdempotencyConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderRequestMapper orderRequestMapper;

    @MockitoBean
    private OrderTimeoutMessageSender orderTimeoutMessageSender;

    @Test
    void concurrentSameRequestIdOnlyLocksStockOnce() throws Exception {
        String requestId = "REQ-concurrent-" + System.nanoTime();
        resetSeedInventory(1000, 0);
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            try {
                Future<OrderCreateVO> firstFuture = executorService.submit(createOrderTask(startLatch, requestId));
                Future<OrderCreateVO> secondFuture = executorService.submit(createOrderTask(startLatch, requestId));

                startLatch.countDown();

                OrderCreateVO firstResult = firstFuture.get();
                OrderCreateVO secondResult = secondFuture.get();

                assertEquals(firstResult.getOrderId(), secondResult.getOrderId());
                assertEquals(1L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, TestSeedData.USER_ID)
                        .eq(Order::getRequestId, requestId)));
                assertEquals(1L, orderRequestMapper.selectCount(new LambdaQueryWrapper<OrderRequest>()
                        .eq(OrderRequest::getUserId, TestSeedData.USER_ID)
                        .eq(OrderRequest::getRequestId, requestId)));

                Inventory inventory = getSeedInventory();
                assertEquals(998, inventory.getAvailableStock());
                assertEquals(2, inventory.getLockedStock());
                assertEquals(1, inventory.getVersion());
                verify(orderTimeoutMessageSender, times(1)).send(any(OrderTimeoutMessage.class));
            } finally {
                executorService.shutdownNow();
            }
        } finally {
            cleanOrderData(requestId);
            resetSeedInventory(1000, 0);
        }
    }

    private Callable<OrderCreateVO> createOrderTask(CountDownLatch startLatch, String requestId) {
        return () -> {
            startLatch.await(); // 阻塞等待主线程唤醒后一起跑，模拟并发
            return orderService.createOrder(TestSeedData.USER_ID, buildCreateRequest(requestId));
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
        request.setRemark("concurrent idempotency test");
        return request;
    }

    private Inventory getSeedInventory() {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, TestSeedData.SKU_ID));
        return inventory;
    }

    private void resetSeedInventory(Integer availableStock, Integer lockedStock) {
        Inventory inventory = getSeedInventory();
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventoryMapper.updateById(inventory);
    }

    private void cleanOrderData(String requestId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, TestSeedData.USER_ID)
                .eq(Order::getRequestId, requestId));
        if (order != null) {
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            orderMapper.deleteById(order.getId());
        }
        orderRequestMapper.delete(new LambdaQueryWrapper<OrderRequest>()
                .eq(OrderRequest::getUserId, TestSeedData.USER_ID)
                .eq(OrderRequest::getRequestId, requestId));
    }
}
