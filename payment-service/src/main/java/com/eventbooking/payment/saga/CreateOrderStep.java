package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaStep;
import com.eventbooking.payment.dto.CreateOrderRequest;
import com.eventbooking.payment.dto.OrderDto;
import com.eventbooking.payment.dto.OrderItemRequest;
import com.eventbooking.payment.entity.PaymentStatus;
import com.eventbooking.payment.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

/**
 * Saga step to create an order
 */
@Component
public class CreateOrderStep implements SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateOrderStep.class);
    
    private final OrderService orderService;
    
    public CreateOrderStep(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @Override
    public boolean execute(SagaContext context) {
        logger.info("Creating order for saga: {}", context.getSagaId());
        
        try {
            UUID userId = context.get("userId", UUID.class);
            UUID eventId = context.get("eventId", UUID.class);
            UUID ticketTypeId = context.get("ticketTypeId", UUID.class);
            Integer quantity = context.get("quantity", Integer.class);
            BigDecimal unitPrice = context.get("unitPrice", BigDecimal.class);
            UUID reservationId = context.get("reservationId", UUID.class);
            
            if (userId == null || eventId == null || ticketTypeId == null || 
                quantity == null || unitPrice == null) {
                logger.error("Missing required parameters for order creation");
                context.setErrorMessage("Missing required parameters");
                return false;
            }
            
            // Create order item request
            OrderItemRequest itemRequest = new OrderItemRequest();
            itemRequest.setTicketTypeId(ticketTypeId);
            itemRequest.setQuantity(quantity);
            itemRequest.setUnitPrice(unitPrice);
            
            // Create order request
            CreateOrderRequest orderRequest = new CreateOrderRequest();
            orderRequest.setEventId(eventId);
            orderRequest.setItems(Collections.singletonList(itemRequest));
            orderRequest.setReservationId(reservationId);
            
            // Create the order
            OrderDto order = orderService.createOrder(userId, orderRequest);
            
            logger.info("Order created successfully: {} (Saga ID: {})", 
                    order.getOrderNumber(), context.getSagaId());
            
            // Store order details in context
            context.put("orderId", order.getId());
            context.put("orderNumber", order.getOrderNumber());
            context.put("totalAmount", order.getTotalAmount());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to create order", e);
            context.setErrorMessage("Order creation failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void compensate(SagaContext context) {
        logger.info("Compensating order creation for saga: {}", context.getSagaId());
        
        try {
            UUID orderId = context.get("orderId", UUID.class);
            UUID userId = context.get("userId", UUID.class);
            
            if (orderId != null && userId != null) {
                // Cancel the order
                orderService.cancelOrder(orderId, userId);
                logger.info("Order cancelled as compensation: {} (Saga ID: {})", 
                        orderId, context.getSagaId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to compensate order creation", e);
            // Log but don't throw - compensation should be best effort
        }
    }
    
    @Override
    public String getStepName() {
        return "CreateOrder";
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
}
