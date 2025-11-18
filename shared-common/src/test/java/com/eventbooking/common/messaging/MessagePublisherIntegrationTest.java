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
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MessagePublisher testing asynchronous message publishing
 */
@ExtendWith(MockitoExtension.class)
class MessagePublisherIntegrationTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private SnsClient snsClient;

    private MessagePublisher messagePublisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        messagePublisher = new MessagePublisher(sqsClient, snsClient);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void publishToQueue_Success() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        PaymentEvent event = createPaymentEvent();
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId("msg-123")
                .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(response);

        // Act
        messagePublisher.publishToQueue(queueUrl, event);

        // Assert
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        
        SendMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertNotNull(request.messageBody());
        assertTrue(request.messageBody().contains(event.getOrderId().toString()));
    }

    @Test
    void publishToQueueWithDelay_Success() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        PaymentEvent event = createPaymentEvent();
        int delaySeconds = 30;
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId("msg-123")
                .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(response);

        // Act
        messagePublisher.publishToQueueWithDelay(queueUrl, event, delaySeconds);

        // Assert
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        
        SendMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertEquals(delaySeconds, request.delaySeconds());
        assertNotNull(request.messageBody());
    }

    @Test
    void publishToTopic_Success() {
        // Arrange
        String topicArn = "arn:aws:sns:us-east-1:123456789:test-topic";
        TicketEvent event = createTicketEvent();
        
        PublishResponse response = PublishResponse.builder()
                .messageId("msg-123")
                .build();
        
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        // Act
        messagePublisher.publishToTopic(topicArn, event);

        // Assert
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertEquals(topicArn, request.topicArn());
        assertNotNull(request.message());
        assertTrue(request.message().contains(event.getOrderId().toString()));
    }

    @Test
    void publishToTopicWithAttributes_Success() {
        // Arrange
        String topicArn = "arn:aws:sns:us-east-1:123456789:test-topic";
        TicketEvent event = createTicketEvent();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("eventType", "TICKET_GENERATED");
        attributes.put("priority", "high");
        
        PublishResponse response = PublishResponse.builder()
                .messageId("msg-123")
                .build();
        
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

        // Act
        messagePublisher.publishToTopic(topicArn, event, attributes);

        // Assert
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertEquals(topicArn, request.topicArn());
        assertNotNull(request.messageAttributes());
        assertEquals(2, request.messageAttributes().size());
    }

    @Test
    void publishToQueue_SerializationError() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        Object invalidObject = new Object() {
            // Object that cannot be serialized
            private final Object circular = this;
        };

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> messagePublisher.publishToQueue(queueUrl, invalidObject));
        
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void publishToQueue_SqsClientError() {
        // Arrange
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";
        PaymentEvent event = createPaymentEvent();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS service unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> messagePublisher.publishToQueue(queueUrl, event));
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

    private TicketEvent createTicketEvent() {
        TicketEvent event = new TicketEvent();
        event.setEventType(TicketEvent.EventType.TICKETS_GENERATED);
        event.setOrderId(UUID.randomUUID());
        event.setUserId(UUID.randomUUID());
        event.setEventId(UUID.randomUUID());
        event.setTicketIds(Arrays.asList(UUID.randomUUID()));
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
