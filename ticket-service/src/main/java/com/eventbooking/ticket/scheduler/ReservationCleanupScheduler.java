package com.eventbooking.ticket.scheduler;

import com.eventbooking.ticket.service.TicketTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationCleanupScheduler.class);
    
    private final TicketTypeService ticketTypeService;
    
    @Autowired
    public ReservationCleanupScheduler(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }
    
    /**
     * Clean up expired reservations every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredReservations() {
        logger.info("Starting cleanup of expired reservations");
        try {
            ticketTypeService.cleanupExpiredReservations();
        } catch (Exception e) {
            logger.error("Error during reservation cleanup", e);
        }
    }
}
