package com.eventbooking.ticket.service;

import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.exception.TicketTypeNotFoundException;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private UUID ticketTypeId;
    private TicketType testTicketType;
    private String inventoryKey;

    @BeforeEach
    void setUp() {
        ticketTypeId = UUID.randomUUID();
        inventoryKey = "inventory:" + ticketTypeId.toString();

        testTicketType = new TicketType();
        testTicketType.setId(ticketTypeId);
        testTicketType.setEventId(UUID.randomUUID());
        testTicketType.setName("General Admission");
        testTicketType.setPrice(new BigDecimal("50.00"));
        testTicketType.setQuantityAvailable(100);
        testTicketType.setQuantitySold(20);
        testTicketType.setQuantityReserved(10);
    }

    // ========== Get Available Quantity Tests ==========

    @Test
    void getAvailableQuantity_WithCachedValue_ShouldReturnFromCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(inventoryKey)).thenReturn(70);

        Integer result = inventoryService.getAvailableQuantity(ticketTypeId);

        assertEquals(70, result);
        verify(valueOperations).get(inventoryKey);
        verify(ticketTypeRepository, never()).findById(any());
    }

    @Test
    void getAvailableQuantity_WithCacheMiss_ShouldSyncFromDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(inventoryKey)).thenReturn(null, 70);
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        Integer result = inventoryService.getAvailableQuantity(ticketTypeId);

        assertEquals(70, result);
        verify(ticketTypeRepository).findById(ticketTypeId);
        verify(valueOperations).set(eq(inventoryKey), eq(70), eq(24L), eq(TimeUnit.HOURS));
    }

    // ========== Reserve Tickets Tests ==========

    @Test
    void reserveTickets_WithSufficientInventory_ShouldReturnTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(inventoryKey, 5)).thenReturn(65L);

        boolean result = inventoryService.reserveTickets(ticketTypeId, 5);

        assertTrue(result);
        verify(valueOperations).decrement(inventoryKey, 5);
        verify(valueOperations, never()).increment(any(), anyInt());
    }

    @Test
    void reserveTickets_WithInsufficientInventory_ShouldRollbackAndReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(inventoryKey, 10)).thenReturn(-5L);

        boolean result = inventoryService.reserveTickets(ticketTypeId, 10);

        assertFalse(result);
        verify(valueOperations).decrement(inventoryKey, 10);
        verify(valueOperations).increment(inventoryKey, 10);
    }

    @Test
    void reserveTickets_WithNullValue_ShouldReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(inventoryKey, 5)).thenReturn(null);

        boolean result = inventoryService.reserveTickets(ticketTypeId, 5);

        assertFalse(result);
        verify(valueOperations).decrement(inventoryKey, 5);
    }

    // ========== Release Reservation Tests ==========

    @Test
    void releaseReservation_ShouldIncrementInventory() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        inventoryService.releaseReservation(ticketTypeId, 5);

        verify(valueOperations).increment(inventoryKey, 5);
    }

    // ========== Confirm Purchase Tests ==========

    @Test
    void confirmPurchase_ShouldLogPurchase() {
        inventoryService.confirmPurchase(ticketTypeId, 3);

        // This method primarily logs, so we just verify it doesn't throw
        verifyNoInteractions(valueOperations);
    }

    // ========== Sync Inventory Tests ==========

    @Test
    void syncInventoryFromDatabase_WithValidTicketType_ShouldUpdateCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        inventoryService.syncInventoryFromDatabase(ticketTypeId);

        verify(ticketTypeRepository).findById(ticketTypeId);
        verify(valueOperations).set(eq(inventoryKey), eq(70), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void syncInventoryFromDatabase_WithInvalidTicketType_ShouldThrowException() {
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.empty());

        assertThrows(TicketTypeNotFoundException.class, () ->
            inventoryService.syncInventoryFromDatabase(ticketTypeId)
        );
    }

    // ========== Clear Cache Tests ==========

    @Test
    void clearInventoryCache_ShouldDeleteKey() {
        inventoryService.clearInventoryCache(ticketTypeId);

        verify(redisTemplate).delete(inventoryKey);
    }
}
