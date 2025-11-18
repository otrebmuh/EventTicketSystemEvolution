package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.InMemorySagaEventStore;
import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaExecutionSummary;
import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.PaymentResponse;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.service.OrderService;
import com.eventbooking.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseSagaTest {
    
    @Mock
    private OrderService orderService;
    
    @Mock
    private PaymentService paymentService;
    
    private InMemorySagaEventStore eventStore;
    private TicketPurchaseSaga saga;
    
    @BeforeEach
    void setUp() {
        eventStore = new InMemorySagaEventStore();
        
        ValidateInventoryStep validateInventoryStep = new ValidateInventoryStep();
        CreateOrderStep createOrderStep = new CreateOrderStep(orderService);
        ProcessPaymentStep processPaymentStep = new ProcessPaymentStep(paymentService);
        ConfirmOrderStep confirmOrderStep = new ConfirmOrderStep(orderService);
        
        saga = new TicketPurchaseSaga(
                validateInventoryStep,
                createOrderStep,
                processPaymentStep,
                confirmOrderStep,
                eventStore
        );
    }
    
    @Test
    void testSuccessfulTicketPurchase() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-123456";
        BigDecimal totalAmount = new BigDecimal("100.00");
        
        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setOrderNumber(orderNumber);
        orderDto.setTotalAmount(totalAmount);
        orderDto.setPaymentStatus(PaymentStatus.PENDING);
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("succeeded");
        paymentResponse.setPaymentIntentId("pi_123");
        paymentResponse.setTransactionId(UUID.randomUUID());
        
        OrderDto confirmedOrder = new OrderDto();
        confirmedOrder.setId(orderId);
        confirmedOrder.setOrderNumber(orderNumber);
        confirmedOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenReturn(orderDto);
        when(paymentService.processPayment(any()))
                .thenReturn(paymentResponse);
        when(orderService.confirmOrder(eq(orderId), eq(userId), eq("pi_123")))
                .thenReturn(confirmedOrder);
        
        // Create saga context
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", 2);
        context.put("unitPrice", new BigDecimal("50.00"));
        context.put("paymentMethodId", "pm_123");
        
        // Act
        boolean result = saga.executePurchase(context);
        
        // Assert
        assertTrue(result);
        assertEquals(SagaContext.SagaStatus.COMPLETED, context.getStatus());
        assertEquals(orderId, context.get("orderId", UUID.class));
        assertEquals(orderNumber, context.get("orderNumber", String.class));
        
        // Verify saga events were recorded
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertEquals(4, summary.getCompletedSteps().size());
        assertFalse(summary.isCompensated());
        
        // Verify service calls
        verify(orderService).createOrder(eq(userId), any(CreateOrderRequest.class));
        verify(paymentService).processPayment(any());
        verify(orderService).confirmOrder(eq(orderId), eq(userId), eq("pi_123"));
    }
    
    @Test
    void testFailedPaymentTriggersCompensation() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setOrderNumber("ORD-123456");
        orderDto.setTotalAmount(new BigDecimal("100.00"));
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("failed");
        paymentResponse.setErrorMessage("Insufficient funds");
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenReturn(orderDto);
        when(paymentService.processPayment(any()))
                .thenReturn(paymentResponse);
        
        // Create saga context
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", 2);
        context.put("unitPrice", new BigDecimal("50.00"));
        context.put("paymentMethodId", "pm_123");
        
        // Act
        boolean result = saga.executePurchase(context);
        
        // Assert
        assertFalse(result);
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        assertNotNull(context.getErrorMessage());
        
        // Verify saga events were recorded
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertNotNull(summary);
        assertEquals("COMPENSATED", summary.getStatus());
        assertTrue(summary.isCompensated());
        assertEquals("ProcessPayment", summary.getFailedStep());
        
        // Verify compensation was called
        verify(orderService).cancelOrder(eq(orderId), eq(userId));
    }
    
    @Test
    void testMissingParametersFailsValidation() {
        // Arrange - context without required parameters
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", UUID.randomUUID());
        // Missing other required parameters
        
        // Act
        boolean result = saga.executePurchase(context);
        
        // Assert
        assertFalse(result);
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        assertNotNull(context.getErrorMessage());
        
        // Verify saga events
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertNotNull(summary);
        assertEquals("COMPENSATED", summary.getStatus());
        assertEquals("ValidateInventory", summary.getFailedStep());
    }
}
