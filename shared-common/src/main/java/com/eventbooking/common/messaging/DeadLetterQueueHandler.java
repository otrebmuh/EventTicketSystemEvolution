package com.eventbooking.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.function.Consumer;

@Service
public class DeadLetterQueueHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);
    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20;
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    
    public DeadLetterQueueHandler(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Process messages from dead letter queue
     * This allows for manual inspection and reprocessing of failed messages
     */
    public <T> void processDLQMessages(String dlqUrl, Class<T> messageType, 
                                       Consumer<DLQMessage<T>> messageHandler) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(MAX_MESSAGES)
                    .waitTimeSeconds(WAIT_TIME_SECONDS)
                    .attributeNamesWithStrings("All")
                    .messageAttributeNames("All")
                    .build();
            
            List<Message> messages = sqsClient.receiveMessage(request).messages();
            
            logger.info("Processing {} messages from DLQ: {}", messages.size(), dlqUrl);
            
            for (Message message : messages) {
                try {
                    T parsedMessage = objectMapper.readValue(message.body(), messageType);
                    
                    DLQMessage<T> dlqMessage = new DLQMessage<>(
                            message.messageId(),
                            message.receiptHandle(),
                            parsedMessage,
                            message.attributes().get("ApproximateReceiveCount"),
                            message.attributes().get("SentTimestamp")
                    );
                    
                    messageHandler.accept(dlqMessage);
                    
                } catch (Exception e) {
                    logger.error("Failed to process DLQ message: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to process DLQ messages from {}: {}", 
                    dlqUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Delete message from DLQ after manual processing
     */
    public void deleteDLQMessage(String dlqUrl, String receiptHandle) {
        try {
            sqsClient.deleteMessage(builder -> builder
                    .queueUrl(dlqUrl)
                    .receiptHandle(receiptHandle));
            
            logger.info("Deleted message from DLQ: {}", dlqUrl);
            
        } catch (Exception e) {
            logger.error("Failed to delete message from DLQ {}: {}", 
                    dlqUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Requeue message from DLQ to original queue for retry
     */
    public void requeueMessage(String dlqUrl, String targetQueueUrl, 
                              String receiptHandle, String messageBody) {
        try {
            // Send to target queue
            sqsClient.sendMessage(builder -> builder
                    .queueUrl(targetQueueUrl)
                    .messageBody(messageBody));
            
            // Delete from DLQ
            deleteDLQMessage(dlqUrl, receiptHandle);
            
            logger.info("Requeued message from DLQ {} to {}", dlqUrl, targetQueueUrl);
            
        } catch (Exception e) {
            logger.error("Failed to requeue message from DLQ: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Wrapper class for DLQ messages with metadata
     */
    public static class DLQMessage<T> {
        private final String messageId;
        private final String receiptHandle;
        private final T message;
        private final String receiveCount;
        private final String sentTimestamp;
        
        public DLQMessage(String messageId, String receiptHandle, T message, 
                         String receiveCount, String sentTimestamp) {
            this.messageId = messageId;
            this.receiptHandle = receiptHandle;
            this.message = message;
            this.receiveCount = receiveCount;
            this.sentTimestamp = sentTimestamp;
        }
        
        public String getMessageId() {
            return messageId;
        }
        
        public String getReceiptHandle() {
            return receiptHandle;
        }
        
        public T getMessage() {
            return message;
        }
        
        public String getReceiveCount() {
            return receiveCount;
        }
        
        public String getSentTimestamp() {
            return sentTimestamp;
        }
    }
}
