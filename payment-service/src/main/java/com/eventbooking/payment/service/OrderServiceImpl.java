package com.eventbooking.payment.service;

import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.OrderItemRequest;
import com.eventbooking.payment.dto.UpdateOrderStatusRequest;
import com.eventbooking.payment.entity.Order;
import com.eventbooking.payment.entity.OrderItem;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.exception.InvalidOrderException;
import com.eventbooking.payment.exception.OrderNotFoundException;
import com.eventbooking.payment.mapper.OrderMapper;
import com.eventbooking.payment.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private static final BigDecimal SERVICE_FEE_PERCENTAGE = new BigDecimal("0.10"); // 10%
    private static final BigDecimal TAX_PERCENTAGE = new BigDecimal("0.08"); // 8%
    private static final int ORDER_EXPIRY_MINUTES = 15;
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    
    @Override
    @Transactional
    public OrderDto createOrder(UUID userId, CreateOrderRequest request) {
        log.info("Creating order for user {} and event {}", userId, request.getEventId());
        
        // Validate request
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
        
        // Calculate order totals
        BigDecimal subtotal = calculateSubtotal(request.getItems());
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_PERCENTAGE);
        BigDecimal taxAmount = subtotal.multiply(TAX_PERCENTAGE);
        BigDecimal totalAmount = subtotal.add(serviceFee).add(taxAmount);
        
        // Generate unique order number
        String orderNumber = generateOrderNumber();
        
        // Create order
        Order order = Order.builder()
                .userId(userId)
                .eventId(request.getEventId())
                .orderNumber(orderNumber)
                .subtotalAmount(subtotal)
                .serviceFee(serviceFee)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .currency("USD")
                .reservationId(request.getReservationId())
                .expiresAt(Instant.now().plus(ORDER_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .build();
        
        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = createOrderItem(itemRequest);
            order.addOrderItem(orderItem);
        }
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} and order number: {}", 
                savedOrder.getId(), savedOrder.getOrderNumber());
        
        return orderMapper.toDto(savedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID orderId) {
        log.debug("Fetching order by ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toDto(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order by order number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        return orderMapper.toDto(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getUserOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getUserOrdersByStatus(UUID userId, PaymentStatus status, Pageable pageable) {
        log.debug("Fetching orders for user: {} with status: {}", userId, status);
        Page<Order> orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    @Override
    @Transactional
    public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order status for order: {} to {}", orderId, request.getPaymentStatus());
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate status transition
        validateStatusTransition(order.getPaymentStatus(), request.getPaymentStatus());
        
        order.setPaymentStatus(request.getPaymentStatus());
        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully for order: {}", orderId);
        
        return orderMapper.toDto(updatedOrder);
    }
    
    @Override
    @Transactional
    public OrderDto cancelOrder(UUID orderId, UUID userId) {
        log.info("Cancelling order: {} for user: {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new InvalidOrderException("Order does not belong to user");
        }
        
        // Validate order can be cancelled
        if (order.getPaymentStatus() != PaymentStatus.PENDING && 
            order.getPaymentStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidOrderException("Order cannot be cancelled in current status: " + 
                    order.getPaymentStatus());
        }
        
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        
        log.info("Order cancelled successfully: {}", orderId);
        return orderMapper.toDto(cancelledOrder);
    }
    
    @Override
    @Transactional
    public OrderDto confirmPayment(UUID orderId, String transactionId) {
        log.info("Confirming payment for order: {} with transaction: {}", orderId, transactionId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (order.getPaymentStatus() != PaymentStatus.PENDING && 
            order.getPaymentStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidOrderException("Order is not in a payable status: " + 
                    order.getPaymentStatus());
        }
        
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.save(order);
        
        log.info("Payment confirmed for order: {}", orderId);
        return orderMapper.toDto(confirmedOrder);
    }
    
    @Override
    @Transactional
    public OrderDto markPaymentFailed(UUID orderId, String errorMessage) {
        log.warn("Marking payment as failed for order: {} - {}", orderId, errorMessage);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
        Order failedOrder = orderRepository.save(order);
        
        log.info("Order marked as payment failed: {}", orderId);
        return orderMapper.toDto(failedOrder);
    }
    
    private BigDecimal calculateSubtotal(java.util.List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private OrderItem createOrderItem(OrderItemRequest request) {
        BigDecimal subtotal = request.getUnitPrice().multiply(new BigDecimal(request.getQuantity()));
        BigDecimal fees = subtotal.multiply(new BigDecimal("0.05")); // 5% per-item fee
        BigDecimal totalPrice = subtotal.add(fees);
        
        return OrderItem.builder()
                .ticketTypeId(request.getTicketTypeId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .subtotal(subtotal)
                .fees(fees)
                .totalPrice(totalPrice)
                .build();
    }
    
    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + System.currentTimeMillis() + "-" + 
                    (int)(Math.random() * 1000);
        } while (orderRepository.existsByOrderNumber(orderNumber));
        
        return orderNumber;
    }
    
    @Override
    @Transactional
    public OrderDto confirmOrder(UUID orderId, UUID userId, String paymentIntentId) {
        log.info("Confirming order: {} for user: {} with payment intent: {}", orderId, userId, paymentIntentId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new InvalidOrderException("Order does not belong to user");
        }
        
        // Validate order can be confirmed
        if (order.getPaymentStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidOrderException("Order cannot be confirmed in current status: " + 
                    order.getPaymentStatus());
        }
        
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.save(order);
        
        log.info("Order confirmed successfully: {}", orderId);
        return orderMapper.toDto(confirmedOrder);
    }
    
    @Override
    @Transactional
    public com.eventbooking.payment.dto.RefundResponse cancelOrderWithRefund(UUID orderId, UUID userId, String cancellationReason) {
        log.info("Cancelling order with refund: {} for user: {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new InvalidOrderException("Order does not belong to user");
        }
        
        // Validate order can be cancelled
        if (order.getPaymentStatus() != PaymentStatus.CONFIRMED) {
            throw new InvalidOrderException("Only confirmed orders can be refunded. Current status: " + 
                    order.getPaymentStatus());
        }
        
        // Check cancellation timeframe (e.g., 24 hours before event)
        validateCancellationTimeframe(order);
        
        // Process refund through payment gateway
        try {
            com.eventbooking.payment.dto.PaymentResponse refundResponse = 
                    paymentService.refundPayment(orderId, cancellationReason);
            
            // Mark all order items as refunded
            order.getOrderItems().forEach(item -> 
                    item.setStatus(com.eventbooking.payment.entity.OrderItemStatus.REFUNDED));
            
            // Update order status
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            Order refundedOrder = orderRepository.save(order);
            
            log.info("Order cancelled with full refund: {}", orderId);
            
            return com.eventbooking.payment.dto.RefundResponse.builder()
                    .refundId(refundResponse.getTransactionId())
                    .orderId(refundedOrder.getId())
                    .orderNumber(refundedOrder.getOrderNumber())
                    .refundAmount(refundedOrder.getTotalAmount())
                    .refundStatus("COMPLETED")
                    .gatewayRefundId(refundResponse.getPaymentIntentId())
                    .refundedAt(Instant.now())
                    .message("Order cancelled successfully. Full refund processed.")
                    .build();
        } catch (Exception e) {
            log.error("Failed to process refund for order: {}", orderId, e);
            throw new InvalidOrderException("Failed to process refund: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public com.eventbooking.payment.dto.RefundResponse partialCancelOrder(UUID orderId, UUID userId, 
                                                                           java.util.List<UUID> orderItemIds, 
                                                                           String cancellationReason) {
        log.info("Partially cancelling order: {} for user: {} - items: {}", orderId, userId, orderItemIds);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new InvalidOrderException("Order does not belong to user");
        }
        
        // Validate order can be partially cancelled
        if (order.getPaymentStatus() != PaymentStatus.CONFIRMED && 
            order.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidOrderException("Order cannot be partially cancelled in current status: " + 
                    order.getPaymentStatus());
        }
        
        // Check cancellation timeframe
        validateCancellationTimeframe(order);
        
        // Validate order items belong to this order and calculate refund amount
        BigDecimal refundAmount = BigDecimal.ZERO;
        int cancelledCount = 0;
        
        for (UUID itemId : orderItemIds) {
            OrderItem item = order.getOrderItems().stream()
                    .filter(oi -> oi.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOrderException("Order item not found: " + itemId));
            
            if (item.getStatus() != com.eventbooking.payment.entity.OrderItemStatus.ACTIVE) {
                throw new InvalidOrderException("Order item is not active: " + itemId);
            }
            
            item.setStatus(com.eventbooking.payment.entity.OrderItemStatus.REFUNDED);
            refundAmount = refundAmount.add(item.getTotalPrice());
            cancelledCount++;
        }
        
        // Update order status
        boolean allItemsCancelled = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == com.eventbooking.payment.entity.OrderItemStatus.REFUNDED ||
                                 item.getStatus() == com.eventbooking.payment.entity.OrderItemStatus.CANCELLED);
        
        if (allItemsCancelled) {
            // Process full refund through payment gateway
            try {
                com.eventbooking.payment.dto.PaymentResponse refundResponse = 
                        paymentService.refundPayment(orderId, cancellationReason);
                
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                Order updatedOrder = orderRepository.save(order);
                
                log.info("All items cancelled, full refund processed for order: {}", orderId);
                
                return com.eventbooking.payment.dto.RefundResponse.builder()
                        .refundId(refundResponse.getTransactionId())
                        .orderId(updatedOrder.getId())
                        .orderNumber(updatedOrder.getOrderNumber())
                        .refundAmount(updatedOrder.getTotalAmount())
                        .refundStatus("COMPLETED")
                        .gatewayRefundId(refundResponse.getPaymentIntentId())
                        .refundedAt(Instant.now())
                        .message("All items cancelled. Full refund processed.")
                        .build();
            } catch (Exception e) {
                log.error("Failed to process full refund for order: {}", orderId, e);
                throw new InvalidOrderException("Failed to process refund: " + e.getMessage(), e);
            }
        } else {
            // For partial refunds, we'll mark as partially refunded
            // In a real system, you would create a partial refund through Stripe
            order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            Order updatedOrder = orderRepository.save(order);
            
            log.info("Order partially cancelled: {} - {} items cancelled, refund amount: {}", 
                    orderId, cancelledCount, refundAmount);
            
            return com.eventbooking.payment.dto.RefundResponse.builder()
                    .refundId(UUID.randomUUID())
                    .orderId(updatedOrder.getId())
                    .orderNumber(updatedOrder.getOrderNumber())
                    .refundAmount(refundAmount)
                    .refundStatus("COMPLETED")
                    .refundedAt(Instant.now())
                    .message(String.format("Partial cancellation successful. %d item(s) cancelled.", cancelledCount))
                    .build();
        }
    }
    
    private void validateCancellationTimeframe(Order order) {
        // For MVP, we'll allow cancellations up to 24 hours before the event
        // In a real system, this would check the event date from the Event Service
        Instant now = Instant.now();
        Instant orderCreatedAt = order.getCreatedAt();
        
        // For now, allow cancellations within 30 days of order creation
        Instant cancellationDeadline = orderCreatedAt.plus(30, ChronoUnit.DAYS);
        
        if (now.isAfter(cancellationDeadline)) {
            throw new InvalidOrderException("Cancellation period has expired");
        }
    }
    
    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        // Define valid transitions
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == PaymentStatus.PROCESSING || 
                           newStatus == PaymentStatus.CANCELLED ||
                           newStatus == PaymentStatus.PAYMENT_FAILED;
            case PROCESSING -> newStatus == PaymentStatus.CONFIRMED || 
                              newStatus == PaymentStatus.PAYMENT_FAILED ||
                              newStatus == PaymentStatus.CANCELLED;
            case CONFIRMED -> newStatus == PaymentStatus.REFUNDED || 
                             newStatus == PaymentStatus.PARTIALLY_REFUNDED;
            case PAYMENT_FAILED, CANCELLED -> false; // Terminal states
            case REFUNDED, PARTIALLY_REFUNDED -> newStatus == PaymentStatus.REFUNDED;
        };
        
        if (!isValid) {
            throw new InvalidOrderException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }
}
