package com.eventbooking.notification.messaging;

import com.eventbooking.common.messaging.MessageConsumer;
import com.eventbooking.common.messaging.PaymentEvent;
import com.eventbooking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final MessageConsumer messageConsumer;
    private final NotificationService notificationService;
    
    @Value("${aws.sqs.payment-events-queue}")
    private String paymentEventsQueue;
    
    @Value("${aws.sqs.payment-events-dlq}")
    private String paymentEventsDlq;
    
    public PaymentEventConsumer(MessageConsumer messageConsumer, 
                               NotificationService notificationService) {
        this.messageConsumer = messageConsumer;
        this.notificationService = notificationService;
    }
    
    /**
     * Poll payment events queue every 10 seconds
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void consumePaymentEvents() {
        try {
            messageConsumer.pollMessages(paymentEventsQueue, PaymentEvent.class, this::handlePaymentEvent);
        } catch (Exception e) {
            logger.error("Error polling payment events queue: {}", e.getMessage(), e);
        }
    }
    
    private void handlePaymentEvent(PaymentEvent event) {
        logger.info("Processing payment event: {} for order: {}", 
                event.getEventType(), event.getOrderId());
        
        try {
            switch (event.getEventType()) {
                case PAYMENT_COMPLETED:
                    handlePaymentCompleted(event);
                    break;
                case PAYMENT_FAILED:
                    handlePaymentFailed(event);
                    break;
                case ORDER_CONFIRMED:
                    handleOrderConfirmed(event);
                    break;
                case ORDER_CANCELLED:
                    handleOrderCancelled(event);
                    break;
                case REFUND_PROCESSED:
                    handleRefundProcessed(event);
                    break;
                default:
                    logger.warn("Unknown payment event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to handle payment event: {} for order: {}", 
                    event.getEventType(), event.getOrderId(), e);
            throw e; // Re-throw to trigger retry or DLQ
        }
    }
    
    private void handlePaymentCompleted(PaymentEvent event) {
        logger.info("Handling payment completed for order: {}", event.getOrderId());
        // Send payment confirmation email
        // This would typically call notificationService.sendPaymentConfirmation()
        // Implementation depends on having user email and order details
    }
    
    private void handlePaymentFailed(PaymentEvent event) {
        logger.info("Handling payment failed for order: {}", event.getOrderId());
        // Send payment failure notification
        // This would typically call notificationService.sendPaymentFailure()
    }
    
    private void handleOrderConfirmed(PaymentEvent event) {
        logger.info("Handling order confirmed for order: {}", event.getOrderId());
        // Send order confirmation email
        // This would typically call notificationService.sendOrderConfirmation()
    }
    
    private void handleOrderCancelled(PaymentEvent event) {
        logger.info("Handling order cancelled for order: {}", event.getOrderId());
        // Send order cancellation notification
        // This would typically call notificationService.sendOrderCancellation()
    }
    
    private void handleRefundProcessed(PaymentEvent event) {
        logger.info("Handling refund processed for order: {}", event.getOrderId());
        // Send refund confirmation email
        // This would typically call notificationService.sendRefundConfirmation()
    }
}
