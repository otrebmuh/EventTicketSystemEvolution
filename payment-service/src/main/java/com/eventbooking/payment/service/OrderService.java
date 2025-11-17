package com.eventbooking.payment.service;

import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.RefundResponse;
import com.eventbooking.payment.dto.UpdateOrderStatusRequest;
import com.eventbooking.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    
    /**
     * Create a new order
     */
    OrderDto createOrder(UUID userId, CreateOrderRequest request);
    
    /**
     * Get order by ID
     */
    OrderDto getOrderById(UUID orderId);
    
    /**
     * Get order by order number
     */
    OrderDto getOrderByOrderNumber(String orderNumber);
    
    /**
     * Get all orders for a user
     */
    Page<OrderDto> getUserOrders(UUID userId, Pageable pageable);
    
    /**
     * Get orders by user and status
     */
    Page<OrderDto> getUserOrdersByStatus(UUID userId, PaymentStatus status, Pageable pageable);
    
    /**
     * Update order status
     */
    OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
    
    /**
     * Cancel an order
     */
    OrderDto cancelOrder(UUID orderId, UUID userId);
    
    /**
     * Confirm payment for an order
     */
    OrderDto confirmPayment(UUID orderId, String transactionId);
    
    /**
     * Mark order as payment failed
     */
    OrderDto markPaymentFailed(UUID orderId, String errorMessage);
    
    /**
     * Confirm order after successful payment
     */
    OrderDto confirmOrder(UUID orderId, UUID userId, String paymentIntentId);
    
    /**
     * Cancel order with full refund
     */
    RefundResponse cancelOrderWithRefund(UUID orderId, UUID userId, String cancellationReason);
    
    /**
     * Partially cancel order items with refund
     */
    RefundResponse partialCancelOrder(UUID orderId, UUID userId, java.util.List<UUID> orderItemIds, String cancellationReason);
}
