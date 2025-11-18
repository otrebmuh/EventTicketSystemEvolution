package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaStep;
import com.eventbooking.payment.dto.PaymentResponse;
import com.eventbooking.payment.dto.ProcessPaymentRequest;
import com.eventbooking.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga step to process payment through payment gateway
 */
@Component
public class ProcessPaymentStep implements SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentStep.class);
    
    private final PaymentService paymentService;
    
    public ProcessPaymentStep(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @Override
    public boolean execute(SagaContext context) {
        logger.info("Processing payment for saga: {}", context.getSagaId());
        
        try {
            UUID orderId = context.get("orderId", UUID.class);
            BigDecimal totalAmount = context.get("totalAmount", BigDecimal.class);
            String paymentMethodId = context.get("paymentMethodId", String.class);
            
            if (orderId == null || totalAmount == null || paymentMethodId == null) {
                logger.error("Missing required parameters for payment processing");
                context.setErrorMessage("Missing required parameters");
                return false;
            }
            
            // Create payment request
            ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
            paymentRequest.setOrderId(orderId);
            paymentRequest.setPaymentMethodId(paymentMethodId);
            
            // Process the payment
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
            
            if ("succeeded".equalsIgnoreCase(paymentResponse.getStatus())) {
                logger.info("Payment processed successfully for order: {} (Saga ID: {})", 
                        orderId, context.getSagaId());
                
                // Store payment details in context
                context.put("paymentIntentId", paymentResponse.getPaymentIntentId());
                context.put("transactionId", paymentResponse.getTransactionId());
                
                return true;
            } else {
                logger.error("Payment processing failed with status: {}", paymentResponse.getStatus());
                context.setErrorMessage("Payment failed: " + paymentResponse.getStatus());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to process payment", e);
            context.setErrorMessage("Payment processing failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void compensate(SagaContext context) {
        logger.info("Compensating payment processing for saga: {}", context.getSagaId());
        
        try {
            UUID orderId = context.get("orderId", UUID.class);
            String paymentIntentId = context.get("paymentIntentId", String.class);
            
            if (orderId != null && paymentIntentId != null) {
                // Refund the payment
                paymentService.refundPayment(orderId, "Saga compensation - transaction failed");
                logger.info("Payment refunded as compensation for order: {} (Saga ID: {})", 
                        orderId, context.getSagaId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to compensate payment processing", e);
            // Log but don't throw - compensation should be best effort
        }
    }
    
    @Override
    public String getStepName() {
        return "ProcessPayment";
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
}
