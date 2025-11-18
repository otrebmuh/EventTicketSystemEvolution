package com.eventbooking.payment.integration;

import com.eventbooking.common.saga.InMemorySagaEventStore;
import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaExecutionSummary;
import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.OrderItemRequest;
import com.eventbooking.payment.dto.PaymentResponse;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.saga.*;
import com.eventbooking.payment.service.OrderService;
import com.eventbooking.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for distributed transaction scenarios using Saga pattern
 * Tests the complete ticket purchase flow across multiple services
 */
@ExtendWith(MockitoExtension.class)
class TicketPurchaseFlowIntegrationTest {

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
    void testCompleteTicketPurchaseFlow_Success() {
        // Arrange - Setup complete purchase scenario
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-" + System.currentTimeMillis();
        BigDecimal unitPrice = new BigDecimal("50.00");
        int quantity = 2;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Mock order creation
        OrderDto createdOrder = new OrderDto();
        createdOrder.setId(orderId);
        createdOrder.setOrderNumber(orderNumber);
        createdOrder.setUserId(userId);
        createdOrder.setEventId(eventId);
        createdOrder.setTotalAmount(totalAmount);
        createdOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenReturn(createdOrder);
        
        // Mock payment processing
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("succeeded");
        paymentResponse.setPaymentIntentId("pi_test_123");
        paymentResponse.setTransactionId(UUID.randomUUID());
        paymentResponse.setAmount(totalAmount);
        
        when(paymentService.processPayment(any()))
                .thenReturn(paymentResponse);
        
        // Mock order confirmation
        OrderDto confirmedOrder = new OrderDto();
        confirmedOrder.setId(orderId);
        confirmedOrder.setOrderNumber(orderNumber);
        confirmedOrder.setPaymentStatus(PaymentStatus.CONFIRMED);
        
        when(orderService.confirmOrder(eq(orderId), eq(userId), eq("pi_test_123")))
                .thenReturn(confirmedOrder);
        
        // Create saga context
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", quantity);
        context.put("unitPrice", unitPrice);
        context.put("paymentMethodId", "pm_test_card");

        // Act - Execute the complete saga
        boolean result = saga.executePurchase(context);

        // Assert - Verify successful completion
        assertTrue(result, "Saga should complete successfully");
        assertEquals(SagaContext.SagaStatus.COMPLETED, context.getStatus());
        assertEquals(orderId, context.get("orderId", UUID.class));
        assertEquals(orderNumber, context.get("orderNumber", String.class));
        assertNull(context.getErrorMessage());
        
        // Verify saga event store
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertEquals(4, summary.getCompletedSteps().size());
        assertFalse(summary.isCompensated());
        assertNull(summary.getFailedStep());
        
        // Verify all steps were executed in order
        assertTrue(summary.getCompletedSteps().contains("ValidateInventory"));
        assertTrue(summary.getCompletedSteps().contains("CreateOrder"));
        assertTrue(summary.getCompletedSteps().contains("ProcessPayment"));
        assertTrue(summary.getCompletedSteps().contains("ConfirmOrder"));
        
        // Verify service interactions
        verify(orderService).createOrder(eq(userId), any(CreateOrderRequest.class));
        verify(paymentService).processPayment(any());
        verify(orderService).confirmOrder(eq(orderId), eq(userId), eq("pi_test_123"));
        verify(orderService, never()).cancelOrder(any(), any());
    }

    @Test
    void testTicketPurchaseFlow_PaymentFailure_TriggersCompensation() {
        // Arrange - Setup payment failure scenario
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderDto createdOrder = new OrderDto();
        createdOrder.setId(orderId);
        createdOrder.setOrderNumber("ORD-FAIL-123");
        createdOrder.setTotalAmount(new BigDecimal("100.00"));
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenReturn(createdOrder);
        
        // Mock payment failure
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("failed");
        paymentResponse.setErrorMessage("Card declined - insufficient funds");
        
        when(paymentService.processPayment(any()))
                .thenReturn(paymentResponse);
        
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", 2);
        context.put("unitPrice", new BigDecimal("50.00"));
        context.put("paymentMethodId", "pm_test_card");

        // Act
        boolean result = saga.executePurchase(context);

        // Assert - Verify compensation was triggered
        assertFalse(result, "Saga should fail due to payment failure");
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        assertNotNull(context.getErrorMessage());
        assertTrue(context.getErrorMessage().contains("Payment failed"));
        
        // Verify saga event store shows compensation
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertNotNull(summary);
        assertEquals("COMPENSATED", summary.getStatus());
        assertTrue(summary.isCompensated());
        assertEquals("ProcessPayment", summary.getFailedStep());
        
        // Verify compensation actions were executed
        verify(orderService).createOrder(eq(userId), any(CreateOrderRequest.class));
        verify(paymentService).processPayment(any());
        verify(orderService).cancelOrder(eq(orderId), eq(userId));
        verify(orderService, never()).confirmOrder(any(), any(), any());
    }

    @Test
    void testTicketPurchaseFlow_OrderCreationFailure() {
        // Arrange - Setup order creation failure
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", 2);
        context.put("unitPrice", new BigDecimal("50.00"));
        context.put("paymentMethodId", "pm_test_card");

        // Act
        boolean result = saga.executePurchase(context);

        // Assert
        assertFalse(result);
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertEquals("CreateOrder", summary.getFailedStep());
        
        // Verify no payment was attempted
        verify(orderService).createOrder(eq(userId), any(CreateOrderRequest.class));
        verify(paymentService, never()).processPayment(any());
        verify(orderService, never()).confirmOrder(any(), any(), any());
    }

    @Test
    void testTicketPurchaseFlow_ValidationFailure() {
        // Arrange - Missing required parameters
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", UUID.randomUUID());
        // Missing eventId, ticketTypeId, quantity, etc.

        // Act
        boolean result = saga.executePurchase(context);

        // Assert
        assertFalse(result);
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertEquals("ValidateInventory", summary.getFailedStep());
        
        // Verify no services were called
        verify(orderService, never()).createOrder(any(), any());
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    void testTicketPurchaseFlow_ConfirmationFailure_StillCompletes() {
        // Arrange - Confirmation fails but payment succeeded
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderDto createdOrder = new OrderDto();
        createdOrder.setId(orderId);
        createdOrder.setOrderNumber("ORD-123");
        createdOrder.setTotalAmount(new BigDecimal("100.00"));
        
        when(orderService.createOrder(eq(userId), any(CreateOrderRequest.class)))
                .thenReturn(createdOrder);
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("succeeded");
        paymentResponse.setPaymentIntentId("pi_test_123");
        paymentResponse.setTransactionId(UUID.randomUUID());
        
        when(paymentService.processPayment(any()))
                .thenReturn(paymentResponse);
        
        // Confirmation fails
        when(orderService.confirmOrder(any(), any(), any()))
                .thenThrow(new RuntimeException("Confirmation service unavailable"));
        
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", eventId);
        context.put("ticketTypeId", ticketTypeId);
        context.put("quantity", 2);
        context.put("unitPrice", new BigDecimal("50.00"));
        context.put("paymentMethodId", "pm_test_card");

        // Act
        boolean result = saga.executePurchase(context);

        // Assert - Should trigger compensation
        assertFalse(result);
        assertEquals(SagaContext.SagaStatus.COMPENSATED, context.getStatus());
        
        SagaExecutionSummary summary = eventStore.getSagaSummary(context.getSagaId());
        assertEquals("ConfirmOrder", summary.getFailedStep());
        
        // Verify compensation includes refund
        verify(orderService).cancelOrder(eq(orderId), eq(userId));
    }

    @Test
    void testMultipleConcurrentPurchases_IndependentExecution() {
        // Arrange - Setup two independent purchase contexts
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        
        // Mock responses for both purchases
        when(orderService.createOrder(any(), any()))
                .thenAnswer(invocation -> {
                    OrderDto order = new OrderDto();
                    order.setId(UUID.randomUUID());
                    order.setOrderNumber("ORD-" + System.currentTimeMillis());
                    order.setTotalAmount(new BigDecimal("100.00"));
                    return order;
                });
        
        when(paymentService.processPayment(any()))
                .thenAnswer(invocation -> {
                    PaymentResponse response = new PaymentResponse();
                    response.setStatus("succeeded");
                    response.setPaymentIntentId("pi_" + UUID.randomUUID());
                    response.setTransactionId(UUID.randomUUID());
                    return response;
                });
        
        when(orderService.confirmOrder(any(), any(), any()))
                .thenAnswer(invocation -> {
                    OrderDto order = new OrderDto();
                    order.setId(invocation.getArgument(0));
                    order.setPaymentStatus(PaymentStatus.CONFIRMED);
                    return order;
                });
        
        SagaContext context1 = TicketPurchaseSaga.createPurchaseContext();
        context1.put("userId", user1);
        context1.put("eventId", event1);
        context1.put("ticketTypeId", UUID.randomUUID());
        context1.put("quantity", 2);
        context1.put("unitPrice", new BigDecimal("50.00"));
        context1.put("paymentMethodId", "pm_card1");
        
        SagaContext context2 = TicketPurchaseSaga.createPurchaseContext();
        context2.put("userId", user2);
        context2.put("eventId", event2);
        context2.put("ticketTypeId", UUID.randomUUID());
        context2.put("quantity", 3);
        context2.put("unitPrice", new BigDecimal("75.00"));
        context2.put("paymentMethodId", "pm_card2");

        // Act - Execute both sagas
        boolean result1 = saga.executePurchase(context1);
        boolean result2 = saga.executePurchase(context2);

        // Assert - Both should complete independently
        assertTrue(result1);
        assertTrue(result2);
        assertNotEquals(context1.getSagaId(), context2.getSagaId());
        
        // Verify both sagas are tracked separately
        SagaExecutionSummary summary1 = eventStore.getSagaSummary(context1.getSagaId());
        SagaExecutionSummary summary2 = eventStore.getSagaSummary(context2.getSagaId());
        
        assertNotNull(summary1);
        assertNotNull(summary2);
        assertEquals("COMPLETED", summary1.getStatus());
        assertEquals("COMPLETED", summary2.getStatus());
        
        // Verify service was called for both purchases
        verify(orderService, times(2)).createOrder(any(), any());
        verify(paymentService, times(2)).processPayment(any());
        verify(orderService, times(2)).confirmOrder(any(), any(), any());
    }
}
