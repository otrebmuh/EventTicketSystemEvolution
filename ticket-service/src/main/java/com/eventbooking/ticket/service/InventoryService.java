package com.eventbooking.ticket.service;

import java.util.UUID;

public interface InventoryService {
    
    /**
     * Get available quantity for a ticket type from Redis cache
     */
    Integer getAvailableQuantity(UUID ticketTypeId);
    
    /**
     * Reserve tickets in Redis cache
     */
    boolean reserveTickets(UUID ticketTypeId, Integer quantity);
    
    /**
     * Release reserved tickets back to inventory
     */
    void releaseReservation(UUID ticketTypeId, Integer quantity);
    
    /**
     * Confirm ticket purchase and update sold count
     */
    void confirmPurchase(UUID ticketTypeId, Integer quantity);
    
    /**
     * Sync inventory from database to Redis
     */
    void syncInventoryFromDatabase(UUID ticketTypeId);
    
    /**
     * Clear inventory cache for a ticket type
     */
    void clearInventoryCache(UUID ticketTypeId);
}
