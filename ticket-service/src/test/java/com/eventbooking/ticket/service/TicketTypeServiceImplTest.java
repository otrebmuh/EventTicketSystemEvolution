package com.eventbooking.ticket.service;

import com.eventbooking.ticket.dto.*;
import com.eventbooking.ticket.entity.TicketReservation;
import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.exception.InsufficientInventoryException;
import com.eventbooking.ticket.exception.InvalidReservationException;
import com.eventbooking.ticket.exception.TicketTypeNotFoundException;
import com.eventbooking.ticket.mapper.TicketTypeMapper;
import com.eventbooking.ticket.repository.TicketReservationRepository;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private TicketReservationRepository reservationRepository;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private TicketTypeServiceImpl ticketTypeService;

    private UUID eventId;
    private UUID ticketTypeId;
    private UUID userId;
    private UUID organizerId;
    private TicketType testTicketType;
    private TicketTypeDto testTicketTypeDto;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizerId = UUID.randomUUID();

        testTicketType = new TicketType();
        testTicketType.setId(ticketTypeId);
        testTicketType.setEventId(eventId);
        testTicketType.setName("General Admission");
        testTicketType.setDescription("Standard entry ticket");
        testTicketType.setPrice(new BigDecimal("50.00"));
        testTicketType.setQuantityAvailable(100);
        testTicketType.setQuantitySold(0);
        testTicketType.setQuantityReserved(0);
        testTicketType.setPerPersonLimit(10);
        testTicketType.setSaleStartDate(LocalDateTime.now().minusDays(1));
        testTicketType.setSaleEndDate(LocalDateTime.now().plusDays(30));

        testTicketTypeDto = new TicketTypeDto();
        testTicketTypeDto.setId(ticketTypeId);
        testTicketTypeDto.setEventId(eventId);
        testTicketTypeDto.setName("General Admission");
        testTicketTypeDto.setPrice(new BigDecimal("50.00"));
        testTicketTypeDto.setQuantityAvailable(100);
    }

    // ========== Ticket Type Creation Tests ==========

    @Test
    void createTicketType_WithValidData_ShouldCreateTicketType() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest();
        request.setEventId(eventId);
        request.setName("VIP Pass");
        request.setDescription("VIP access");
        request.setPrice(new BigDecimal("150.00"));
        request.setQuantityAvailable(50);
        request.setPerPersonLimit(5);

        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
        when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

        TicketTypeDto result = ticketTypeService.createTicketType(request, organizerId);

        assertNotNull(result);
        assertEquals(ticketTypeId, result.getId());
        verify(ticketTypeRepository).save(any(TicketType.class));
        verify(inventoryService).syncInventoryFromDatabase(ticketTypeId);
    }

    @Test
    void createTicketType_WithInvalidSaleDates_ShouldThrowException() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest();
        request.setEventId(eventId);
        request.setName("VIP Pass");
        request.setPrice(new BigDecimal("150.00"));
        request.setQuantityAvailable(50);
        request.setSaleStartDate(LocalDateTime.now().plusDays(10));
        request.setSaleEndDate(LocalDateTime.now().plusDays(5));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.createTicketType(request, organizerId)
        );
    }

    // ========== Ticket Type Retrieval Tests ==========

    @Test
    void getTicketTypeById_WithValidId_ShouldReturnTicketType() {
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

        TicketTypeDto result = ticketTypeService.getTicketTypeById(ticketTypeId);

        assertNotNull(result);
        assertEquals(ticketTypeId, result.getId());
        verify(ticketTypeRepository).findById(ticketTypeId);
    }

    @Test
    void getTicketTypeById_WithInvalidId_ShouldThrowException() {
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.empty());

        assertThrows(TicketTypeNotFoundException.class, () ->
            ticketTypeService.getTicketTypeById(ticketTypeId)
        );
    }

    @Test
    void getTicketTypesByEventId_ShouldReturnList() {
        List<TicketType> ticketTypes = Arrays.asList(testTicketType);
        when(ticketTypeRepository.findByEventId(eventId)).thenReturn(ticketTypes);
        when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

        List<TicketTypeDto> result = ticketTypeService.getTicketTypesByEventId(eventId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketTypeRepository).findByEventId(eventId);
    }

    // ========== Ticket Type Update Tests ==========

    @Test
    void updateTicketType_WithValidData_ShouldUpdateTicketType() {
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest();
        request.setName("Updated Name");
        request.setPrice(new BigDecimal("75.00"));

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
        when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

        TicketTypeDto result = ticketTypeService.updateTicketType(ticketTypeId, request, organizerId);

        assertNotNull(result);
        verify(ticketTypeRepository).save(testTicketType);
        verify(inventoryService).syncInventoryFromDatabase(ticketTypeId);
    }

    @Test
    void updateTicketType_ReducingQuantityBelowSold_ShouldThrowException() {
        testTicketType.setQuantitySold(50);
        
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest();
        request.setQuantityAvailable(30);

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.updateTicketType(ticketTypeId, request, organizerId)
        );
    }

    // ========== Ticket Type Deletion Tests ==========

    @Test
    void deleteTicketType_WithNoSoldTickets_ShouldDelete() {
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        ticketTypeService.deleteTicketType(ticketTypeId, organizerId);

        verify(ticketTypeRepository).delete(testTicketType);
        verify(inventoryService).clearInventoryCache(ticketTypeId);
    }

    @Test
    void deleteTicketType_WithSoldTickets_ShouldThrowException() {
        testTicketType.setQuantitySold(10);
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.deleteTicketType(ticketTypeId, organizerId)
        );
    }

    // ========== Reservation Tests ==========

    @Test
    void reserveTickets_WithValidRequest_ShouldCreateReservation() {
        ReserveTicketsRequest request = new ReserveTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(5);

        TicketReservation reservation = new TicketReservation();
        reservation.setId(UUID.randomUUID());
        reservation.setUserId(userId);
        reservation.setTicketTypeId(ticketTypeId);
        reservation.setQuantity(5);
        reservation.setStatus(TicketReservation.ReservationStatus.ACTIVE);

        when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(inventoryService.reserveTickets(ticketTypeId, 5)).thenReturn(true);
        when(reservationRepository.save(any(TicketReservation.class))).thenReturn(reservation);
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);

        ReservationDto result = ticketTypeService.reserveTickets(request, userId);

        assertNotNull(result);
        assertEquals(5, result.getQuantity());
        verify(inventoryService).reserveTickets(ticketTypeId, 5);
        verify(reservationRepository).save(any(TicketReservation.class));
    }

    @Test
    void reserveTickets_ExceedingPerPersonLimit_ShouldThrowException() {
        ReserveTicketsRequest request = new ReserveTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(15);

        when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.reserveTickets(request, userId)
        );
    }

    @Test
    void reserveTickets_InsufficientInventory_ShouldThrowException() {
        ReserveTicketsRequest request = new ReserveTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(5);

        when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(inventoryService.reserveTickets(ticketTypeId, 5)).thenReturn(false);
        when(inventoryService.getAvailableQuantity(ticketTypeId)).thenReturn(2);

        assertThrows(InsufficientInventoryException.class, () ->
            ticketTypeService.reserveTickets(request, userId)
        );
    }

    @Test
    void reserveTickets_NotOnSale_ShouldThrowException() {
        testTicketType.setSaleStartDate(LocalDateTime.now().plusDays(1));
        
        ReserveTicketsRequest request = new ReserveTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(5);

        when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(testTicketType));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.reserveTickets(request, userId)
        );
    }

    // ========== Reservation Cancellation Tests ==========

    @Test
    void cancelReservation_WithValidReservation_ShouldCancel() {
        UUID reservationId = UUID.randomUUID();
        TicketReservation reservation = new TicketReservation();
        reservation.setId(reservationId);
        reservation.setUserId(userId);
        reservation.setTicketTypeId(ticketTypeId);
        reservation.setQuantity(5);
        reservation.setStatus(TicketReservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(reservationRepository.save(any(TicketReservation.class))).thenReturn(reservation);
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);

        ticketTypeService.cancelReservation(reservationId, userId);

        verify(inventoryService).releaseReservation(ticketTypeId, 5);
        verify(reservationRepository).save(reservation);
        assertEquals(TicketReservation.ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void cancelReservation_UnauthorizedUser_ShouldThrowException() {
        UUID reservationId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        
        TicketReservation reservation = new TicketReservation();
        reservation.setId(reservationId);
        reservation.setUserId(userId);
        reservation.setStatus(TicketReservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationException.class, () ->
            ticketTypeService.cancelReservation(reservationId, differentUserId)
        );
    }

    // ========== Expired Reservation Cleanup Tests ==========

    @Test
    void cleanupExpiredReservations_ShouldProcessExpiredReservations() {
        TicketReservation expiredReservation = new TicketReservation();
        expiredReservation.setId(UUID.randomUUID());
        expiredReservation.setTicketTypeId(ticketTypeId);
        expiredReservation.setQuantity(5);
        expiredReservation.setStatus(TicketReservation.ReservationStatus.ACTIVE);

        List<TicketReservation> expiredReservations = Arrays.asList(expiredReservation);

        when(reservationRepository.findExpiredReservations(
            eq(TicketReservation.ReservationStatus.ACTIVE), any(LocalDateTime.class)))
            .thenReturn(expiredReservations);
        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(reservationRepository.save(any(TicketReservation.class))).thenReturn(expiredReservation);
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);

        ticketTypeService.cleanupExpiredReservations();

        verify(inventoryService).releaseReservation(ticketTypeId, 5);
        verify(reservationRepository).save(expiredReservation);
        assertEquals(TicketReservation.ReservationStatus.EXPIRED, expiredReservation.getStatus());
    }
}
