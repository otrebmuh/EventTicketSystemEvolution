package com.eventbooking.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessagePublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);
    
    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    
    public MessagePublisher(SqsClient sqsClient, SnsClient snsClient) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Publish message to SQS queue
     */
    public void publishToQueue(String queueUrl, Object message) {
        try {
            String messageBody = objectMapper.writeValueAsString(message);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(request);
            
            logger.info("Message published to SQS queue: {} with messageId: {}", 
                    queueUrl, response.messageId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for SQS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SQS", e);
        } catch (Exception e) {
            logger.error("Failed to publish message to SQS queue {}: {}", 
                    queueUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SQS", e);
        }
    }
    
    /**
     * Publish message to SQS queue with delay
     */
    public void publishToQueueWithDelay(String queueUrl, Object message, int delaySeconds) {
        try {
            String messageBody = objectMapper.writeValueAsString(message);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delaySeconds)
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(request);
            
            logger.info("Message published to SQS queue: {} with delay: {}s, messageId: {}", 
                    queueUrl, delaySeconds, response.messageId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for SQS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SQS", e);
        } catch (Exception e) {
            logger.error("Failed to publish message to SQS queue {}: {}", 
                    queueUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SQS", e);
        }
    }
    
    /**
     * Publish message to SNS topic
     */
    public void publishToTopic(String topicArn, Object message) {
        publishToTopic(topicArn, message, new HashMap<>());
    }
    
    /**
     * Publish message to SNS topic with attributes
     */
    public void publishToTopic(String topicArn, Object message, Map<String, String> attributes) {
        try {
            String messageBody = objectMapper.writeValueAsString(message);
            
            var requestBuilder = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageBody);
            
            // Add message attributes if provided
            if (attributes != null && !attributes.isEmpty()) {
                requestBuilder.messageAttributes(convertToMessageAttributes(attributes));
            }
            
            PublishResponse response = snsClient.publish(requestBuilder.build());
            
            logger.info("Message published to SNS topic: {} with messageId: {}", 
                    topicArn, response.messageId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for SNS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SNS", e);
        } catch (Exception e) {
            logger.error("Failed to publish message to SNS topic {}: {}", 
                    topicArn, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SNS", e);
        }
    }
    
    private Map<String, software.amazon.awssdk.services.sns.model.MessageAttributeValue> 
            convertToMessageAttributes(Map<String, String> attributes) {
        Map<String, software.amazon.awssdk.services.sns.model.MessageAttributeValue> result = 
                new HashMap<>();
        
        attributes.forEach((key, value) -> 
            result.put(key, software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(value)
                    .build())
        );
        
        return result;
    }
}
