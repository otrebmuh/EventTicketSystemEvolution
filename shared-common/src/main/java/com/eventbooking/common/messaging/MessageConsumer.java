package com.eventbooking.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.function.Consumer;

@Service
public class MessageConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20; // Long polling
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    
    public MessageConsumer(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Poll messages from SQS queue
     */
    public <T> void pollMessages(String queueUrl, Class<T> messageType, 
                                 Consumer<T> messageHandler) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(MAX_MESSAGES)
                    .waitTimeSeconds(WAIT_TIME_SECONDS)
                    .build();
            
            List<Message> messages = sqsClient.receiveMessage(request).messages();
            
            logger.debug("Received {} messages from queue: {}", messages.size(), queueUrl);
            
            for (Message message : messages) {
                try {
                    T parsedMessage = objectMapper.readValue(message.body(), messageType);
                    messageHandler.accept(parsedMessage);
                    
                    // Delete message after successful processing
                    deleteMessage(queueUrl, message.receiptHandle());
                    
                } catch (Exception e) {
                    logger.error("Failed to process message: {}", e.getMessage(), e);
                    // Message will be retried or moved to DLQ based on queue configuration
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to poll messages from queue {}: {}", 
                    queueUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Delete message from queue after successful processing
     */
    public void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            DeleteMessageRequest request = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();
            
            sqsClient.deleteMessage(request);
            logger.debug("Message deleted from queue: {}", queueUrl);
            
        } catch (Exception e) {
            logger.error("Failed to delete message from queue {}: {}", 
                    queueUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Move message to dead letter queue
     */
    public void moveToDeadLetterQueue(String sourceQueueUrl, String dlqUrl, 
                                      String receiptHandle, String messageBody) {
        try {
            // Send to DLQ
            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(sendRequest);
            
            // Delete from source queue
            deleteMessage(sourceQueueUrl, receiptHandle);
            
            logger.info("Message moved to DLQ: {}", dlqUrl);
            
        } catch (Exception e) {
            logger.error("Failed to move message to DLQ: {}", e.getMessage(), e);
        }
    }
}
