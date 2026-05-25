package com.tuzki.mall.order.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.order.vo.OrderDetailVO;
import com.tuzki.mall.order.vo.OrderMainVO;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口，提供用户下单等订单交易能力。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    private final PaymentService paymentService;


    public OrderController(OrderService orderService,
                           PaymentService paymentService,
                           OrderMapper orderMapper) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public Result<OrderCreateVO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderById(@PathVariable Long orderId) {
        return Result.success(orderService.getOrderById(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return Result.success();
    }

    @PostMapping("/{orderId}/pay")
    public Result<PaymentPayVO> payOrder(@PathVariable Long orderId) {
        return Result.success(paymentService.payOrder(orderId));
    }


    @GetMapping
    public Result<List<OrderMainVO>> listOrders(@RequestParam Long userId) {
        return Result.success(orderService.listOrders(userId));
    }
}
