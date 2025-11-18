package com.eventbooking.payment.service;

import com.eventbooking.payment.dto.*;
import com.eventbooking.payment.entity.Order;
import com.eventbooking.payment.entity.OrderItem;
import com.eventbooking.payment.entity.OrderItemStatus;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.exception.InvalidOrderException;
import com.eventbooking.payment.exception.OrderNotFoundException;
import com.eventbooking.payment.mapper.OrderMapper;
import com.eventbooking.payment.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private PaymentEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderServiceImpl orderService;
    
    private UUID userId;
    private UUID eventId;
    private UUID ticketTypeId;
    private CreateOrderRequest createOrderRequest;
    private Order testOrder;
    private OrderDto testOrderDto;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        
        // Setup create order request
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();
        
        createOrderRequest = CreateOrderRequest.builder()
                .eventId(eventId)
                .reservationId(UUID.randomUUID())
                .items(List.of(itemRequest))
                .build();
        
        // Setup test order
        testOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .eventId(eventId)
                .orderNumber("ORD-123456")
                .subtotalAmount(new BigDecimal("100.00"))
                .serviceFee(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("8.00"))
                .totalAmount(new BigDecimal("118.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .currency("USD")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .orderItems(new ArrayList<>())
                .build();
        
        // Setup test order DTO
        testOrderDto = OrderDto.builder()
                .id(testOrder.getId())
                .userId(userId)
                .eventId(eventId)
                .orderNumber("ORD-123456")
                .subtotalAmount(new BigDecimal("100.00"))
                .serviceFee(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("8.00"))
                .totalAmount(new BigDecimal("118.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .currency("USD")
                .build();
    }
    
    // ========== Order Creation Tests ==========
    
    @Test
    void testCreateOrder_Success() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.createOrder(userId, createOrderRequest);
        
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(eventId, result.getEventId());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(any(Order.class));
    }
    
    @Test
    void testCreateOrder_EmptyItems() {
        CreateOrderRequest emptyRequest = CreateOrderRequest.builder()
                .eventId(eventId)
                .reservationId(UUID.randomUUID())
                .items(new ArrayList<>())
                .build();
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.createOrder(userId, emptyRequest);
        });
        
        verifyNoInteractions(orderRepository);
    }
    
    @Test
    void testCreateOrder_NullItems() {
        CreateOrderRequest nullItemsRequest = CreateOrderRequest.builder()
                .eventId(eventId)
                .reservationId(UUID.randomUUID())
                .items(null)
                .build();
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.createOrder(userId, nullItemsRequest);
        });
        
        verifyNoInteractions(orderRepository);
    }
    
    @Test
    void testCreateOrder_CalculatesCorrectTotals() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            // Verify calculations
            assertEquals(0, new BigDecimal("100.00").compareTo(savedOrder.getSubtotalAmount()));
            assertEquals(0, new BigDecimal("10.00").compareTo(savedOrder.getServiceFee()));
            assertEquals(0, new BigDecimal("8.00").compareTo(savedOrder.getTaxAmount()));
            assertEquals(0, new BigDecimal("118.00").compareTo(savedOrder.getTotalAmount()));
            return savedOrder;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.createOrder(userId, createOrderRequest);
        
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }
    
    // ========== Order Retrieval Tests ==========
    
    @Test
    void testGetOrderById_Success() {
        UUID orderId = testOrder.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        
        OrderDto result = orderService.getOrderById(orderId);
        
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(orderRepository).findById(orderId);
    }
    
    @Test
    void testGetOrderById_NotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(orderId);
        });
        
        verify(orderRepository).findById(orderId);
    }
    
    @Test
    void testGetOrderByOrderNumber_Success() {
        String orderNumber = "ORD-123456";
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        
        OrderDto result = orderService.getOrderByOrderNumber(orderNumber);
        
        assertNotNull(result);
        assertEquals(orderNumber, result.getOrderNumber());
        verify(orderRepository).findByOrderNumber(orderNumber);
    }
    
    @Test
    void testGetUserOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(orderPage);
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        
        Page<OrderDto> result = orderService.getUserOrders(userId, pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    @Test
    void testGetUserOrdersByStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        
        when(orderRepository.findByUserIdAndStatus(userId, PaymentStatus.PENDING, pageable))
                .thenReturn(orderPage);
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        
        Page<OrderDto> result = orderService.getUserOrdersByStatus(userId, PaymentStatus.PENDING, pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findByUserIdAndStatus(userId, PaymentStatus.PENDING, pageable);
    }
    
    // ========== Order Cancellation Tests ==========
    
    @Test
    void testCancelOrder_Success() {
        UUID orderId = testOrder.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.cancelOrder(orderId, userId);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testCancelOrder_WrongUser() {
        UUID orderId = testOrder.getId();
        UUID differentUserId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.cancelOrder(orderId, differentUserId);
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testCancelOrder_InvalidStatus() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.cancelOrder(orderId, userId);
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testCancelOrder_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.cancelOrder(orderId, userId);
        });
    }
    
    // ========== Order Confirmation Tests ==========
    
    @Test
    void testConfirmPayment_Success() {
        UUID orderId = testOrder.getId();
        String transactionId = "txn_123456";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.confirmPayment(orderId, transactionId);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.CONFIRMED, testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testConfirmPayment_InvalidStatus() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.confirmPayment(orderId, "txn_123");
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testConfirmOrder_Success() {
        UUID orderId = testOrder.getId();
        String paymentIntentId = "pi_123456";
        testOrder.setPaymentStatus(PaymentStatus.PROCESSING);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.confirmOrder(orderId, userId, paymentIntentId);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.CONFIRMED, testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testConfirmOrder_WrongUser() {
        UUID orderId = testOrder.getId();
        UUID differentUserId = UUID.randomUUID();
        testOrder.setPaymentStatus(PaymentStatus.PROCESSING);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.confirmOrder(orderId, differentUserId, "pi_123");
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testConfirmOrder_InvalidStatus() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.confirmOrder(orderId, userId, "pi_123");
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testMarkPaymentFailed_Success() {
        UUID orderId = testOrder.getId();
        String errorMessage = "Payment declined";
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.markPaymentFailed(orderId, errorMessage);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }
    
    // ========== Order Status Update Tests ==========
    
    @Test
    void testUpdateOrderStatus_ValidTransition() {
        UUID orderId = testOrder.getId();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .paymentStatus(PaymentStatus.PROCESSING)
                .paymentMethod("card")
                .build();
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        
        OrderDto result = orderService.updateOrderStatus(orderId, request);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.PROCESSING, testOrder.getPaymentStatus());
        assertEquals("card", testOrder.getPaymentMethod());
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testUpdateOrderStatus_InvalidTransition() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CANCELLED);
        
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .paymentStatus(PaymentStatus.CONFIRMED)
                .build();
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.updateOrderStatus(orderId, request);
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    // ========== Order Refund Tests ==========
    
    @Test
    void testCancelOrderWithRefund_Success() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .fees(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("105.00"))
                .status(OrderItemStatus.ACTIVE)
                .build();
        testOrder.addOrderItem(orderItem);
        
        PaymentResponse paymentResponse = PaymentResponse.builder()
                .transactionId(UUID.randomUUID())
                .orderId(orderId)
                .paymentIntentId("pi_refund_123")
                .status("refunded")
                .amount(testOrder.getTotalAmount())
                .currency("USD")
                .build();
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(paymentService.refundPayment(orderId, "Customer request")).thenReturn(paymentResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        RefundResponse result = orderService.cancelOrderWithRefund(orderId, userId, "Customer request");
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(testOrder.getTotalAmount(), result.getRefundAmount());
        assertEquals("COMPLETED", result.getRefundStatus());
        assertEquals(PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
        assertEquals(OrderItemStatus.REFUNDED, orderItem.getStatus());
        
        verify(paymentService).refundPayment(orderId, "Customer request");
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testCancelOrderWithRefund_WrongUser() {
        UUID orderId = testOrder.getId();
        UUID differentUserId = UUID.randomUUID();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.cancelOrderWithRefund(orderId, differentUserId, "Customer request");
        });
        
        verifyNoInteractions(paymentService);
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testCancelOrderWithRefund_InvalidStatus() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.cancelOrderWithRefund(orderId, userId, "Customer request");
        });
        
        verifyNoInteractions(paymentService);
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testPartialCancelOrder_Success() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        OrderItem orderItem1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .fees(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("105.00"))
                .status(OrderItemStatus.ACTIVE)
                .build();
        
        OrderItem orderItem2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(UUID.randomUUID())
                .quantity(1)
                .unitPrice(new BigDecimal("30.00"))
                .subtotal(new BigDecimal("30.00"))
                .fees(new BigDecimal("1.50"))
                .totalPrice(new BigDecimal("31.50"))
                .status(OrderItemStatus.ACTIVE)
                .build();
        
        testOrder.addOrderItem(orderItem1);
        testOrder.addOrderItem(orderItem2);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        RefundResponse result = orderService.partialCancelOrder(
                orderId, userId, List.of(orderItem1.getId()), "Partial cancellation");
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(new BigDecimal("105.00"), result.getRefundAmount());
        assertEquals("COMPLETED", result.getRefundStatus());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, testOrder.getPaymentStatus());
        assertEquals(OrderItemStatus.REFUNDED, orderItem1.getStatus());
        assertEquals(OrderItemStatus.ACTIVE, orderItem2.getStatus());
        
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testPartialCancelOrder_AllItemsCancelled() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .fees(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("105.00"))
                .status(OrderItemStatus.ACTIVE)
                .build();
        testOrder.addOrderItem(orderItem);
        
        PaymentResponse paymentResponse = PaymentResponse.builder()
                .transactionId(UUID.randomUUID())
                .orderId(orderId)
                .paymentIntentId("pi_refund_123")
                .status("refunded")
                .amount(testOrder.getTotalAmount())
                .currency("USD")
                .build();
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(paymentService.refundPayment(orderId, "All items cancelled")).thenReturn(paymentResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        RefundResponse result = orderService.partialCancelOrder(
                orderId, userId, List.of(orderItem.getId()), "All items cancelled");
        
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
        assertEquals(OrderItemStatus.REFUNDED, orderItem.getStatus());
        
        verify(paymentService).refundPayment(orderId, "All items cancelled");
        verify(orderRepository).save(testOrder);
    }
    
    @Test
    void testPartialCancelOrder_InvalidOrderItem() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .fees(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("105.00"))
                .status(OrderItemStatus.ACTIVE)
                .build();
        testOrder.addOrderItem(orderItem);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        UUID invalidItemId = UUID.randomUUID();
        assertThrows(InvalidOrderException.class, () -> {
            orderService.partialCancelOrder(orderId, userId, List.of(invalidItemId), "Cancellation");
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    void testPartialCancelOrder_AlreadyRefundedItem() {
        UUID orderId = testOrder.getId();
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(ticketTypeId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .fees(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("105.00"))
                .status(OrderItemStatus.REFUNDED)
                .build();
        testOrder.addOrderItem(orderItem);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            orderService.partialCancelOrder(orderId, userId, List.of(orderItem.getId()), "Cancellation");
        });
        
        verify(orderRepository, never()).save(any(Order.class));
    }
}
