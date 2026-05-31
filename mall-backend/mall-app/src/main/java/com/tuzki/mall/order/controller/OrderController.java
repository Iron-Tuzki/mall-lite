package com.tuzki.mall.order.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.order.vo.OrderDetailVO;
import com.tuzki.mall.order.vo.OrderMainVO;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import com.tuzki.mall.user.service.LoginSessionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单接口控制器，提供用户下单、订单查询、取消订单和发起支付能力。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final OrderService orderService;

    private final PaymentService paymentService;

    private final LoginSessionService loginSessionService;

    public OrderController(OrderService orderService,
                           PaymentService paymentService,
                           LoginSessionService loginSessionService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.loginSessionService = loginSessionService;
    }

    @PostMapping
    public Result<OrderCreateVO> createOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(resolveCurrentUserId(authorization), request));
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
    public Result<List<OrderMainVO>> listOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(orderService.listOrders(
                resolveCurrentUserId(authorization),
                status,
                startTime,
                endTime));
    }

    private Long resolveCurrentUserId(String authorization) {
        String token = resolveToken(authorization);
        Long userId = loginSessionService.getUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "invalid login token");
        }
        return userId;
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "missing login token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "missing login token");
        }
        return token;
    }
}
