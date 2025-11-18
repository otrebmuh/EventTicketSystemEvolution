package com.eventbooking.payment.service;

import com.eventbooking.payment.dto.PaymentResponse;
import com.eventbooking.payment.dto.ProcessPaymentRequest;
import com.eventbooking.payment.entity.Order;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.entity.PaymentTransaction;
import com.eventbooking.payment.exception.InvalidOrderException;
import com.eventbooking.payment.exception.OrderNotFoundException;
import com.eventbooking.payment.repository.OrderRepository;
import com.eventbooking.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private PaymentTransactionRepository transactionRepository;
    
    @Mock
    private PaymentEventPublisher eventPublisher;
    
    @InjectMocks
    private PaymentServiceImpl paymentService;
    
    private Order testOrder;
    private ProcessPaymentRequest paymentRequest;
    
    @BeforeEach
    void setUp() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        
        testOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .eventId(eventId)
                .orderNumber("ORD-123456")
                .subtotalAmount(new BigDecimal("100.00"))
                .serviceFee(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("8.00"))
                .totalAmount(new BigDecimal("118.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .currency("USD")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();
        
        paymentRequest = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .paymentMethodId("pm_card_visa")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .build();
    }
    
    @Test
    void testProcessPayment_OrderNotFound() {
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        
        assertThrows(OrderNotFoundException.class, () -> {
            paymentService.processPayment(paymentRequest);
        });
        
        verify(orderRepository).findById(paymentRequest.getOrderId());
        verifyNoInteractions(transactionRepository);
    }
    
    @Test
    void testProcessPayment_OrderNotInPendingStatus() {
        testOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.of(testOrder));
        
        assertThrows(InvalidOrderException.class, () -> {
            paymentService.processPayment(paymentRequest);
        });
        
        verify(orderRepository).findById(paymentRequest.getOrderId());
    }
    
    @Test
    void testProcessPayment_OrderExpired() {
        testOrder.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        assertThrows(InvalidOrderException.class, () -> {
            paymentService.processPayment(paymentRequest);
        });
        
        verify(orderRepository).findById(paymentRequest.getOrderId());
        verify(orderRepository).save(testOrder);
        assertEquals(PaymentStatus.CANCELLED, testOrder.getPaymentStatus());
    }
    
    @Test
    void testGetPaymentStatus_Success() {
        UUID transactionId = UUID.randomUUID();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(transactionId)
                .order(testOrder)
                .paymentIntentId("pi_test123")
                .amount(new BigDecimal("118.00"))
                .currency("USD")
                .status("succeeded")
                .paymentMethod("card")
                .build();
        
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        
        PaymentResponse response = paymentService.getPaymentStatus(transactionId);
        
        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
        assertEquals(testOrder.getId(), response.getOrderId());
        assertEquals("succeeded", response.getStatus());
        assertEquals(new BigDecimal("118.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        
        verify(transactionRepository).findById(transactionId);
    }
}
