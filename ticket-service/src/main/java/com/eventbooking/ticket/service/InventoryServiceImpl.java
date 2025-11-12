package com.eventbooking.ticket.service;

import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.exception.TicketTypeNotFoundException;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryServiceImpl implements InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private static final String INVENTORY_KEY_PREFIX = "inventory:";
    private static final long CACHE_TTL_HOURS = 24;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final TicketTypeRepository ticketTypeRepository;
    
    @Autowired
    public InventoryServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            TicketTypeRepository ticketTypeRepository) {
        this.redisTemplate = redisTemplate;
        this.ticketTypeRepository = ticketTypeRepository;
    }
    
    @Override
    public Integer getAvailableQuantity(UUID ticketTypeId) {
        String key = getInventoryKey(ticketTypeId);
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            // Cache miss - sync from database
            syncInventoryFromDatabase(ticketTypeId);
            value = redisTemplate.opsForValue().get(key);
        }
        
        return value != null ? (Integer) value : 0;
    }
    
    @Override
    public boolean reserveTickets(UUID ticketTypeId, Integer quantity) {
        String key = getInventoryKey(ticketTypeId);
        
        // Use Redis decrement operation for atomic inventory update
        Long newValue = redisTemplate.opsForValue().decrement(key, quantity);
        
        if (newValue == null || newValue < 0) {
            // Not enough inventory - rollback
            if (newValue != null) {
                redisTemplate.opsForValue().increment(key, quantity);
            }
            logger.warn("Insufficient inventory for ticket type: {}. Requested: {}, Available: {}", 
                       ticketTypeId, quantity, newValue != null ? newValue + quantity : 0);
            return false;
        }
        
        logger.info("Reserved {} tickets for ticket type: {}. Remaining: {}", 
                   quantity, ticketTypeId, newValue);
        return true;
    }
    
    @Override
    public void releaseReservation(UUID ticketTypeId, Integer quantity) {
        String key = getInventoryKey(ticketTypeId);
        redisTemplate.opsForValue().increment(key, quantity);
        logger.info("Released {} tickets for ticket type: {}", quantity, ticketTypeId);
    }
    
    @Override
    public void confirmPurchase(UUID ticketTypeId, Integer quantity) {
        // Inventory already decremented during reservation
        // This method can be used for additional tracking or logging
        logger.info("Confirmed purchase of {} tickets for ticket type: {}", quantity, ticketTypeId);
    }
    
    @Override
    public void syncInventoryFromDatabase(UUID ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId));
        
        Integer availableQuantity = ticketType.getAvailableQuantity();
        String key = getInventoryKey(ticketTypeId);
        
        redisTemplate.opsForValue().set(key, availableQuantity, CACHE_TTL_HOURS, TimeUnit.HOURS);
        logger.info("Synced inventory for ticket type: {}. Available: {}", ticketTypeId, availableQuantity);
    }
    
    @Override
    public void clearInventoryCache(UUID ticketTypeId) {
        String key = getInventoryKey(ticketTypeId);
        redisTemplate.delete(key);
        logger.info("Cleared inventory cache for ticket type: {}", ticketTypeId);
    }
    
    private String getInventoryKey(UUID ticketTypeId) {
        return INVENTORY_KEY_PREFIX + ticketTypeId.toString();
    }
}
