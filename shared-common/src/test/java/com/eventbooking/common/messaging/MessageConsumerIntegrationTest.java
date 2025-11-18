package com.eventbooking.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MessageConsumer testing asynchronous message consumption
 */
@ExtendWith(MockitoExtension.class)
class MessageConsumerIntegrationTest {

    @Mock
    private SqsClient sqsClient;

    private MessageConsumer messageConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        messageConsumer = new MessageConsumer(sqsClient);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void pollMessages_Success() throws Exception {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        PaymentEvent event = createPaymentEvent();
        String messageBody = objectMapper.writeValueAsString(event);
        
        Message message = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-123")
                .body(messageBody)
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(message)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);
        
        AtomicInteger processedCount = new AtomicInteger(0);

        // Act
        messageConsumer.pollMessages(queueUrl, PaymentEvent.class, receivedEvent -> {
            assertEquals(event.getOrderId(), receivedEvent.getOrderId());
            assertEquals(event.getEventType(), receivedEvent.getEventType());
            processedCount.incrementAndGet();
        });

        // Assert
        assertEquals(1, processedCount.get());
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollMessages_MultipleMessages() throws Exception {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        
        PaymentEvent event1 = createPaymentEvent();
        PaymentEvent event2 = createPaymentEvent();
        
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
        
        AtomicInteger processedCount = new AtomicInteger(0);

        // Act
        messageConsumer.pollMessages(queueUrl, PaymentEvent.class, 
            receivedEvent -> processedCount.incrementAndGet());

        // Assert
        assertEquals(2, processedCount.get());
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollMessages_NoMessages() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(Arrays.asList())
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);
        
        AtomicInteger processedCount = new AtomicInteger(0);

        // Act
        messageConsumer.pollMessages(queueUrl, PaymentEvent.class, 
            receivedEvent -> processedCount.incrementAndGet());

        // Assert
        assertEquals(0, processedCount.get());
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollMessages_HandlerException_MessageNotDeleted() throws Exception {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        PaymentEvent event = createPaymentEvent();
        
        Message message = Message.builder()
                .messageId("msg-123")
                .receiptHandle("receipt-123")
                .body(objectMapper.writeValueAsString(event))
                .build();
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(message)
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        messageConsumer.pollMessages(queueUrl, PaymentEvent.class, receivedEvent -> {
            throw new RuntimeException("Processing failed");
        });

        // Assert - message should not be deleted when handler throws exception
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void deleteMessage_Success() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        String receiptHandle = "receipt-123";

        // Act
        messageConsumer.deleteMessage(queueUrl, receiptHandle);

        // Assert
        ArgumentCaptor<DeleteMessageRequest> captor = 
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());
        
        DeleteMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertEquals(receiptHandle, request.receiptHandle());
    }

    @Test
    void moveToDeadLetterQueue_Success() throws Exception {
        // Arrange
        String sourceQueueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/source-queue";
        String dlqUrl = "https://sqs.us-east-1.amazonaws.com/123456789/dlq";
        String receiptHandle = "receipt-123";
        PaymentEvent event = createPaymentEvent();
        String messageBody = objectMapper.writeValueAsString(event);
        
        SendMessageResponse sendResponse = SendMessageResponse.builder()
                .messageId("dlq-msg-123")
                .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendResponse);

        // Act
        messageConsumer.moveToDeadLetterQueue(sourceQueueUrl, dlqUrl, receiptHandle, messageBody);

        // Assert
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollMessages_VerifyLongPolling() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(Arrays.asList())
                .build();
        
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        // Act
        messageConsumer.pollMessages(queueUrl, PaymentEvent.class, event -> {});

        // Assert - verify long polling is configured
        ArgumentCaptor<ReceiveMessageRequest> captor = 
                ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());
        
        ReceiveMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertEquals(20, request.waitTimeSeconds()); // Long polling
        assertEquals(10, request.maxNumberOfMessages());
    }

    private PaymentEvent createPaymentEvent() {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(PaymentEvent.EventType.PAYMENT_COMPLETED);
        event.setOrderId(UUID.randomUUID());
        event.setUserId(UUID.randomUUID());
        event.setAmount(new BigDecimal("100.00"));
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
