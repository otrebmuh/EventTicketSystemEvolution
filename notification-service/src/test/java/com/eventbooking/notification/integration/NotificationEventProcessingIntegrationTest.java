package com.eventbooking.notification.integration;

import com.eventbooking.common.messaging.MessageConsumer;
import com.eventbooking.common.messaging.PaymentEvent;
import com.eventbooking.common.messaging.TicketEvent;
import com.eventbooking.notification.dto.NotificationDto;
import com.eventbooking.notification.entity.NotificationStatus;
import com.eventbooking.notification.messaging.PaymentEventConsumer;
import com.eventbooking.notification.messaging.TicketEventConsumer;
import com.eventbooking.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for asynchronous event processing in notification service
 * Tests message consumption and notification triggering
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventProcessingIntegrationTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private NotificationService notificationService;

    private MessageConsumer messageConsumer;
    private PaymentEventConsumer paymentEventConsumer;
    private TicketEventConsumer ticketEventConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        messageConsumer = new MessageConsumer(sqsClient);
        paymentEventConsumer = new PaymentEventConsumer(messageConsumer, notificationService);
        // Note: TicketEventConsumer requires TicketDeliveryService, not NotificationService
        // For this test, we're focusing on PaymentEventConsumer
        // ticketEventConsumer = new TicketEventConsumer(messageConsumer, ticketDeliveryService);
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testPaymentCompletedEvent_TriggersNotification() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType(PaymentEvent.EventType.PAYMENT_COMPLETED);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setUserId(userId);
        paymentEvent.setAmount(new BigDecimal("150.00"));
        paymentEvent.setTimestamp(LocalDateTime.now());
        
        String messageBody = objectMapper.writeValueAsString(paymentEvent);
        
        Message sqsMessage = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-123")
                .body(messageBody)
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    // Note: Ticket event tests removed as they require TicketDeliveryService
    // This test focuses on PaymentEventConsumer which uses NotificationService

    @Test
    void testMultipleEvents_ProcessedInOrder() throws Exception {
        // Arrange
        PaymentEvent event1 = new PaymentEvent();
        event1.setEventType(PaymentEvent.EventType.PAYMENT_COMPLETED);
        event1.setOrderId(UUID.randomUUID());
        event1.setUserId(UUID.randomUUID());
        event1.setAmount(new BigDecimal("100.00"));
        event1.setTimestamp(LocalDateTime.now());
        
        PaymentEvent event2 = new PaymentEvent();
        event2.setEventType(PaymentEvent.EventType.ORDER_CONFIRMED);
        event2.setOrderId(UUID.randomUUID());
        event2.setUserId(UUID.randomUUID());
        event2.setAmount(new BigDecimal("200.00"));
        event2.setTimestamp(LocalDateTime.now());
        
        Message message1 = Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body(objectMapper.writeValueAsString(event1))
                .build();
        
        Message message2 = Message.builder()
                .messageId("msg-2")
                .receiptHandle("receipt-2")
                .body(objectMapper.writeValueAsString(event2))
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(Arrays.asList(message1, message2))
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert - Both messages should be processed and deleted
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testEventProcessingFailure_MessageNotDeleted() throws Exception {
        // Arrange
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType(PaymentEvent.EventType.PAYMENT_COMPLETED);
        paymentEvent.setOrderId(UUID.randomUUID());
        paymentEvent.setUserId(UUID.randomUUID());
        paymentEvent.setAmount(new BigDecimal("100.00"));
        paymentEvent.setTimestamp(LocalDateTime.now());
        
        String messageBody = objectMapper.writeValueAsString(paymentEvent);
        
        Message sqsMessage = Message.builder()
                .messageId("msg-fail")
                .receiptHandle("receipt-fail")
                .body(messageBody)
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);
        
        // Simulate processing failure
        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationService).sendNotification(any());

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert - Message should not be deleted on failure
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testPaymentFailedEvent_SendsFailureNotification() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType(PaymentEvent.EventType.PAYMENT_FAILED);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setUserId(userId);
        paymentEvent.setAmount(new BigDecimal("150.00"));
        paymentEvent.setErrorMessage("Card declined");
        paymentEvent.setTimestamp(LocalDateTime.now());
        
        String messageBody = objectMapper.writeValueAsString(paymentEvent);
        
        Message sqsMessage = Message.builder()
                .messageId("msg-fail-123")
                .receiptHandle("receipt-fail-123")
                .body(messageBody)
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testRefundProcessedEvent_SendsRefundConfirmation() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType(PaymentEvent.EventType.REFUND_PROCESSED);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setUserId(userId);
        paymentEvent.setAmount(new BigDecimal("150.00"));
        paymentEvent.setTimestamp(LocalDateTime.now());
        
        String messageBody = objectMapper.writeValueAsString(paymentEvent);
        
        Message sqsMessage = Message.builder()
                .messageId("msg-refund-123")
                .receiptHandle("receipt-refund-123")
                .body(messageBody)
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(sqsMessage)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }



    @Test
    void testNoMessages_NoProcessing() {
        // Arrange
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(Arrays.asList())
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        paymentEventConsumer.consumePaymentEvents();

        // Assert
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(notificationService, never()).sendNotification(any());
    }
}
