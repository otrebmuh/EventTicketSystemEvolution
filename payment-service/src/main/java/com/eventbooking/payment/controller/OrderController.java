package com.eventbooking.payment.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.UpdateOrderStatusRequest;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating order for user: {}", userId);
        
        OrderDto order = orderService.createOrder(userId, request);
        ApiResponse<OrderDto> response = ApiResponse.success("Order created successfully", order);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable UUID orderId) {
        log.info("Fetching order: {}", orderId);
        
        OrderDto order = orderService.getOrderById(orderId);
        ApiResponse<OrderDto> response = ApiResponse.success("Order retrieved successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderByOrderNumber(
            @PathVariable String orderNumber) {
        log.info("Fetching order by order number: {}", orderNumber);
        
        OrderDto order = orderService.getOrderByOrderNumber(orderNumber);
        ApiResponse<OrderDto> response = ApiResponse.success("Order retrieved successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getUserOrders(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching orders for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getUserOrders(userId, pageable);
        ApiResponse<Page<OrderDto>> response = ApiResponse.success("Orders retrieved successfully", orders);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getUserOrdersByStatus(
            @PathVariable UUID userId,
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching orders for user: {} with status: {}", userId, status);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getUserOrdersByStatus(userId, status, pageable);
        ApiResponse<Page<OrderDto>> response = ApiResponse.success("Orders retrieved successfully", orders);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order status for order: {}", orderId);
        
        OrderDto order = orderService.updateOrderStatus(orderId, request);
        ApiResponse<OrderDto> response = ApiResponse.success("Order status updated successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Cancelling order: {} for user: {}", orderId, userId);
        
        OrderDto order = orderService.cancelOrder(orderId, userId);
        ApiResponse<OrderDto> response = ApiResponse.success("Order cancelled successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<ApiResponse<OrderDto>> confirmPayment(
            @PathVariable UUID orderId,
            @RequestParam String transactionId) {
        log.info("Confirming payment for order: {}", orderId);
        
        OrderDto order = orderService.confirmPayment(orderId, transactionId);
        ApiResponse<OrderDto> response = ApiResponse.success("Payment confirmed successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderDto>> confirmOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String paymentIntentId) {
        log.info("Confirming order: {} for user: {}", orderId, userId);
        
        OrderDto order = orderService.confirmOrder(orderId, userId, paymentIntentId);
        ApiResponse<OrderDto> response = ApiResponse.success("Order confirmed successfully", order);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/cancel-with-refund")
    public ResponseEntity<ApiResponse<com.eventbooking.payment.dto.RefundResponse>> cancelOrderWithRefund(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String cancellationReason) {
        log.info("Cancelling order with refund: {} for user: {}", orderId, userId);
        
        com.eventbooking.payment.dto.RefundResponse refund = orderService.cancelOrderWithRefund(
                orderId, userId, cancellationReason);
        ApiResponse<com.eventbooking.payment.dto.RefundResponse> response = 
                ApiResponse.success("Order cancelled and refund processed successfully", refund);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/partial-cancel")
    public ResponseEntity<ApiResponse<com.eventbooking.payment.dto.RefundResponse>> partialCancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody com.eventbooking.payment.dto.CancelOrderRequest request) {
        log.info("Partially cancelling order: {} for user: {}", orderId, userId);
        
        if (request.getOrderItemIds() == null || request.getOrderItemIds().isEmpty()) {
            throw new com.eventbooking.payment.exception.InvalidOrderException(
                    "Order item IDs are required for partial cancellation");
        }
        
        com.eventbooking.payment.dto.RefundResponse refund = orderService.partialCancelOrder(
                orderId, userId, request.getOrderItemIds(), request.getCancellationReason());
        ApiResponse<com.eventbooking.payment.dto.RefundResponse> response = 
                ApiResponse.success("Partial cancellation and refund processed successfully", refund);
        
        return ResponseEntity.ok(response);
    }
}
