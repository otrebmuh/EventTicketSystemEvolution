package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaStep;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.entity.Order;
import com.eventbooking.payment.repository.OrderRepository;
import com.eventbooking.payment.service.OrderService;
import com.eventbooking.payment.service.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga step to confirm the order after successful payment
 */
@Component
public class ConfirmOrderStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmOrderStep.class);

    private final OrderService orderService;
    private final PaymentEventPublisher eventPublisher;
    private final OrderRepository orderRepository;

    public ConfirmOrderStep(OrderService orderService,
            PaymentEventPublisher eventPublisher,
            OrderRepository orderRepository) {
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean execute(SagaContext context) {
        logger.info("Confirming order for saga: {}", context.getSagaId());

        try {
            UUID orderId = context.get("orderId", UUID.class);
            UUID userId = context.get("userId", UUID.class);
            String paymentIntentId = context.get("paymentIntentId", String.class);

            if (orderId == null || userId == null || paymentIntentId == null) {
                logger.error("Missing required parameters for order confirmation");
                context.setErrorMessage("Missing required parameters");
                return false;
            }

            // Confirm the order
            OrderDto confirmedOrder = orderService.confirmOrder(orderId, userId, paymentIntentId);

            logger.info("Order confirmed successfully: {} (Saga ID: {})",
                    confirmedOrder.getOrderNumber(), context.getSagaId());

            // Publish payment completed event to trigger ticket generation
            UUID transactionId = context.get("transactionId", UUID.class);
            UUID ticketTypeId = context.get("ticketTypeId", UUID.class);
            Integer quantity = context.get("quantity", Integer.class);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            eventPublisher.publishPaymentCompleted(order, transactionId != null ? transactionId.toString() : null,
                    ticketTypeId, quantity);
            logger.info("Published payment completed event for order: {}", orderId);

            context.put("orderConfirmed", true);

            return true;

        } catch (Exception e) {
            logger.error("Failed to confirm order", e);
            context.setErrorMessage("Order confirmation failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void compensate(SagaContext context) {
        logger.info("Compensating order confirmation for saga: {}", context.getSagaId());

        try {
            UUID orderId = context.get("orderId", UUID.class);

            if (orderId != null) {
                // Mark order as failed
                orderService.markPaymentFailed(orderId, "Saga compensation - order confirmation failed");
                logger.info("Order marked as failed for compensation: {} (Saga ID: {})",
                        orderId, context.getSagaId());
            }

        } catch (Exception e) {
            logger.error("Failed to compensate order confirmation", e);
            // Log but don't throw - compensation should be best effort
        }
    }

    @Override
    public String getStepName() {
        return "ConfirmOrder";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
