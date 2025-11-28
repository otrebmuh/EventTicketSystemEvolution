package com.eventbooking.ticket.service;

import com.eventbooking.common.messaging.MessageConsumer;
import com.eventbooking.common.messaging.PaymentEvent;
import com.eventbooking.ticket.dto.GenerateTicketsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    private final MessageConsumer messageConsumer;
    private final TicketService ticketService;

    @Value("${aws.sqs.payment-events-queue}")
    private String paymentEventsQueue;

    public PaymentEventListener(MessageConsumer messageConsumer, TicketService ticketService) {
        this.messageConsumer = messageConsumer;
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void pollPaymentEvents() {
        logger.debug("Polling payment events from queue: {}", paymentEventsQueue);

        messageConsumer.pollMessages(paymentEventsQueue, PaymentEvent.class, event -> {
            try {
                logger.info("Received payment event: {} for order: {}", event.getEventType(), event.getOrderId());

                if (PaymentEvent.EventType.PAYMENT_COMPLETED.equals(event.getEventType())) {
                    handlePaymentCompleted(event);
                }

            } catch (Exception e) {
                logger.error("Error processing payment event: {}", e.getMessage(), e);
                // Throwing exception will cause the message to not be deleted (if pollMessages
                // handles it that way)
                // But MessageConsumer implementation deletes message on success, logs error on
                // failure.
            }
        });
    }

    private void handlePaymentCompleted(PaymentEvent event) {
        logger.info("Processing PAYMENT_COMPLETED event for order: {}", event.getOrderId());

        if (event.getTicketTypeId() == null || event.getQuantity() == null) {
            logger.warn(
                    "Received PAYMENT_COMPLETED event without ticket details for order: {}. Skipping ticket generation.",
                    event.getOrderId());
            return;
        }

        GenerateTicketsRequest request = new GenerateTicketsRequest();
        request.setOrderId(event.getOrderId());
        request.setUserId(event.getUserId());
        request.setTicketTypeId(event.getTicketTypeId());
        request.setQuantity(event.getQuantity());
        request.setHolderName("Guest"); // Set default holder name

        ticketService.generateTickets(request);

        logger.info("Successfully generated tickets for order: {}", event.getOrderId());
    }
}
