package com.eventbooking.payment.service;

import com.eventbooking.common.messaging.MessagePublisher;
import com.eventbooking.common.messaging.PaymentEvent;
import com.eventbooking.payment.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);
    
    private final MessagePublisher messagePublisher;
    
    @Value("${aws.sqs.payment-events-queue}")
    private String paymentEventsQueue;
    
    @Value("${aws.sns.payment-events-topic}")
    private String paymentEventsTopic;
    
    public PaymentEventPublisher(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }
    
    public void publishPaymentCompleted(Order order, String transactionId) {
        try {
            PaymentEvent event = new PaymentEvent(
                    PaymentEvent.EventType.PAYMENT_COMPLETED,
                    order.getId(),
                    order.getUserId()
            );
            event.setEventId(order.getEventId());
            event.setOrderNumber(order.getOrderNumber());
            event.setAmount(order.getTotalAmount());
            event.setPaymentStatus(order.getPaymentStatus().toString());
            event.setTransactionId(transactionId);
            
            // Publish to both queue and topic for different consumers
            messagePublisher.publishToQueue(paymentEventsQueue, event);
            messagePublisher.publishToTopic(paymentEventsTopic, event);
            
            logger.info("Published PAYMENT_COMPLETED event for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment completed event for order: {}", 
                    order.getId(), e);
            // Don't throw exception - payment already succeeded
        }
    }
    
    public void publishPaymentFailed(Order order, String errorMessage) {
        try {
            PaymentEvent event = new PaymentEvent(
                    PaymentEvent.EventType.PAYMENT_FAILED,
                    order.getId(),
                    order.getUserId()
            );
            event.setEventId(order.getEventId());
            event.setOrderNumber(order.getOrderNumber());
            event.setAmount(order.getTotalAmount());
            event.setPaymentStatus(order.getPaymentStatus().toString());
            event.setErrorMessage(errorMessage);
            
            messagePublisher.publishToQueue(paymentEventsQueue, event);
            messagePublisher.publishToTopic(paymentEventsTopic, event);
            
            logger.info("Published PAYMENT_FAILED event for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment failed event for order: {}", 
                    order.getId(), e);
        }
    }
    
    public void publishOrderConfirmed(Order order) {
        try {
            PaymentEvent event = new PaymentEvent(
                    PaymentEvent.EventType.ORDER_CONFIRMED,
                    order.getId(),
                    order.getUserId()
            );
            event.setEventId(order.getEventId());
            event.setOrderNumber(order.getOrderNumber());
            event.setAmount(order.getTotalAmount());
            event.setPaymentStatus(order.getPaymentStatus().toString());
            
            messagePublisher.publishToQueue(paymentEventsQueue, event);
            messagePublisher.publishToTopic(paymentEventsTopic, event);
            
            logger.info("Published ORDER_CONFIRMED event for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish order confirmed event for order: {}", 
                    order.getId(), e);
        }
    }
    
    public void publishOrderCancelled(Order order) {
        try {
            PaymentEvent event = new PaymentEvent(
                    PaymentEvent.EventType.ORDER_CANCELLED,
                    order.getId(),
                    order.getUserId()
            );
            event.setEventId(order.getEventId());
            event.setOrderNumber(order.getOrderNumber());
            event.setAmount(order.getTotalAmount());
            event.setPaymentStatus(order.getPaymentStatus().toString());
            
            messagePublisher.publishToQueue(paymentEventsQueue, event);
            messagePublisher.publishToTopic(paymentEventsTopic, event);
            
            logger.info("Published ORDER_CANCELLED event for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish order cancelled event for order: {}", 
                    order.getId(), e);
        }
    }
    
    public void publishRefundProcessed(Order order, String transactionId) {
        try {
            PaymentEvent event = new PaymentEvent(
                    PaymentEvent.EventType.REFUND_PROCESSED,
                    order.getId(),
                    order.getUserId()
            );
            event.setEventId(order.getEventId());
            event.setOrderNumber(order.getOrderNumber());
            event.setAmount(order.getTotalAmount());
            event.setPaymentStatus(order.getPaymentStatus().toString());
            event.setTransactionId(transactionId);
            
            messagePublisher.publishToQueue(paymentEventsQueue, event);
            messagePublisher.publishToTopic(paymentEventsTopic, event);
            
            logger.info("Published REFUND_PROCESSED event for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish refund processed event for order: {}", 
                    order.getId(), e);
        }
    }
}
