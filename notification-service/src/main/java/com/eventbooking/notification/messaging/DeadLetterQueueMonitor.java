package com.eventbooking.notification.messaging;

import com.eventbooking.common.messaging.DeadLetterQueueHandler;
import com.eventbooking.common.messaging.PaymentEvent;
import com.eventbooking.common.messaging.TicketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterQueueMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueMonitor.class);
    
    private final DeadLetterQueueHandler dlqHandler;
    
    @Value("${aws.sqs.payment-events-dlq}")
    private String paymentEventsDlq;
    
    @Value("${aws.sqs.ticket-events-dlq}")
    private String ticketEventsDlq;
    
    @Value("${aws.sqs.payment-events-queue}")
    private String paymentEventsQueue;
    
    @Value("${aws.sqs.ticket-events-queue}")
    private String ticketEventsQueue;
    
    public DeadLetterQueueMonitor(DeadLetterQueueHandler dlqHandler) {
        this.dlqHandler = dlqHandler;
    }
    
    /**
     * Monitor payment events DLQ every 5 minutes
     * Log failed messages for manual inspection
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    public void monitorPaymentEventsDLQ() {
        try {
            dlqHandler.processDLQMessages(paymentEventsDlq, PaymentEvent.class, dlqMessage -> {
                logger.error("Payment event in DLQ - MessageId: {}, ReceiveCount: {}, Event: {}, Order: {}", 
                        dlqMessage.getMessageId(),
                        dlqMessage.getReceiveCount(),
                        dlqMessage.getMessage().getEventType(),
                        dlqMessage.getMessage().getOrderId());
                
                // Could implement automatic retry logic here
                // For now, just log for manual inspection
                
                // Example: Retry if receive count is low
                int receiveCount = Integer.parseInt(dlqMessage.getReceiveCount());
                if (receiveCount < 5) {
                    logger.info("Attempting to requeue payment event for order: {}", 
                            dlqMessage.getMessage().getOrderId());
                    // Uncomment to enable automatic requeue:
                    // dlqHandler.requeueMessage(paymentEventsDlq, paymentEventsQueue, 
                    //         dlqMessage.getReceiptHandle(), messageBody);
                }
            });
        } catch (Exception e) {
            logger.error("Error monitoring payment events DLQ: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Monitor ticket events DLQ every 5 minutes
     * Log failed messages for manual inspection
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    public void monitorTicketEventsDLQ() {
        try {
            dlqHandler.processDLQMessages(ticketEventsDlq, TicketEvent.class, dlqMessage -> {
                logger.error("Ticket event in DLQ - MessageId: {}, ReceiveCount: {}, Event: {}, Order: {}", 
                        dlqMessage.getMessageId(),
                        dlqMessage.getReceiveCount(),
                        dlqMessage.getMessage().getEventType(),
                        dlqMessage.getMessage().getOrderId());
                
                // Could implement automatic retry logic here
                // For now, just log for manual inspection
                
                // Example: Retry if receive count is low
                int receiveCount = Integer.parseInt(dlqMessage.getReceiveCount());
                if (receiveCount < 5) {
                    logger.info("Attempting to requeue ticket event for order: {}", 
                            dlqMessage.getMessage().getOrderId());
                    // Uncomment to enable automatic requeue:
                    // dlqHandler.requeueMessage(ticketEventsDlq, ticketEventsQueue, 
                    //         dlqMessage.getReceiptHandle(), messageBody);
                }
            });
        } catch (Exception e) {
            logger.error("Error monitoring ticket events DLQ: {}", e.getMessage(), e);
        }
    }
}
