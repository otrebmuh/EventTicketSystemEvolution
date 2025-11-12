package com.eventbooking.ticket.service;

import com.eventbooking.ticket.dto.*;
import com.eventbooking.ticket.entity.TicketReservation;
import com.eventbooking.ticket.entity.TicketReservation.ReservationStatus;
import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.exception.InsufficientInventoryException;
import com.eventbooking.ticket.exception.InvalidReservationException;
import com.eventbooking.ticket.exception.TicketTypeNotFoundException;
import com.eventbooking.ticket.mapper.TicketTypeMapper;
import com.eventbooking.ticket.repository.TicketReservationRepository;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketTypeServiceImpl implements TicketTypeService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketTypeServiceImpl.class);
    
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketReservationRepository reservationRepository;
    private final TicketTypeMapper ticketTypeMapper;
    private final InventoryService inventoryService;
    
    @Value("${ticket.reservation.timeout-minutes:15}")
    private int reservationTimeoutMinutes;
    
    @Autowired
    public TicketTypeServiceImpl(
            TicketTypeRepository ticketTypeRepository,
            TicketReservationRepository reservationRepository,
            TicketTypeMapper ticketTypeMapper,
            InventoryService inventoryService) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.reservationRepository = reservationRepository;
        this.ticketTypeMapper = ticketTypeMapper;
        this.inventoryService = inventoryService;
    }
    
    @Override
    public TicketTypeDto createTicketType(CreateTicketTypeRequest request, UUID organizerId) {
        // TODO: Validate that the user is the organizer of the event
        // This would require calling the Event Service
        
        // Validate sale dates
        if (request.getSaleStartDate() != null && request.getSaleEndDate() != null) {
            if (request.getSaleEndDate().isBefore(request.getSaleStartDate())) {
                throw new InvalidReservationException("Sale end date must be after sale start date");
            }
        }
        
        TicketType ticketType = new TicketType();
        ticketType.setEventId(request.getEventId());
        ticketType.setName(request.getName());
        ticketType.setDescription(request.getDescription());
        ticketType.setPrice(request.getPrice());
        ticketType.setQuantityAvailable(request.getQuantityAvailable());
        ticketType.setSaleStartDate(request.getSaleStartDate());
        ticketType.setSaleEndDate(request.getSaleEndDate());
        ticketType.setPerPersonLimit(request.getPerPersonLimit());
        ticketType.setVenueZone(request.getVenueZone());
        
        TicketType savedTicketType = ticketTypeRepository.save(ticketType);
        
        // Initialize inventory in Redis
        inventoryService.syncInventoryFromDatabase(savedTicketType.getId());
        
        logger.info("Created ticket type: {} for event: {}", savedTicketType.getId(), request.getEventId());
        
        return ticketTypeMapper.toDto(savedTicketType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TicketTypeDto getTicketTypeById(UUID ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId));
        
        return ticketTypeMapper.toDto(ticketType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeDto> getTicketTypesByEventId(UUID eventId) {
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);
        return ticketTypes.stream()
            .map(ticketTypeMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeDto> getAvailableTicketTypesByEventId(UUID eventId) {
        List<TicketType> ticketTypes = ticketTypeRepository.findAvailableTicketTypesByEventId(eventId);
        return ticketTypes.stream()
            .filter(tt -> tt.getAvailableQuantity() > 0)
            .map(ticketTypeMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public TicketTypeDto updateTicketType(UUID ticketTypeId, UpdateTicketTypeRequest request, UUID organizerId) {
        // TODO: Validate that the user is the organizer of the event
        
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId));
        
        // Update fields if provided
        if (request.getName() != null) {
            ticketType.setName(request.getName());
        }
        if (request.getDescription() != null) {
            ticketType.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            ticketType.setPrice(request.getPrice());
        }
        if (request.getQuantityAvailable() != null) {
            // Validate that new quantity is not less than already sold
            if (request.getQuantityAvailable() < ticketType.getQuantitySold()) {
                throw new InvalidReservationException(
                    "Cannot reduce quantity below already sold tickets: " + ticketType.getQuantitySold());
            }
            ticketType.setQuantityAvailable(request.getQuantityAvailable());
        }
        if (request.getSaleStartDate() != null) {
            ticketType.setSaleStartDate(request.getSaleStartDate());
        }
        if (request.getSaleEndDate() != null) {
            ticketType.setSaleEndDate(request.getSaleEndDate());
        }
        if (request.getPerPersonLimit() != null) {
            ticketType.setPerPersonLimit(request.getPerPersonLimit());
        }
        if (request.getVenueZone() != null) {
            ticketType.setVenueZone(request.getVenueZone());
        }
        
        TicketType savedTicketType = ticketTypeRepository.save(ticketType);
        
        // Sync inventory to Redis
        inventoryService.syncInventoryFromDatabase(savedTicketType.getId());
        
        logger.info("Updated ticket type: {}", ticketTypeId);
        
        return ticketTypeMapper.toDto(savedTicketType);
    }
    
    @Override
    public void deleteTicketType(UUID ticketTypeId, UUID organizerId) {
        // TODO: Validate that the user is the organizer of the event
        
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId));
        
        // Only allow deletion if no tickets have been sold
        if (ticketType.getQuantitySold() > 0) {
            throw new InvalidReservationException("Cannot delete ticket type with sold tickets");
        }
        
        ticketTypeRepository.delete(ticketType);
        inventoryService.clearInventoryCache(ticketTypeId);
        
        logger.info("Deleted ticket type: {}", ticketTypeId);
    }
    
    @Override
    public ReservationDto reserveTickets(ReserveTicketsRequest request, UUID userId) {
        UUID ticketTypeId = request.getTicketTypeId();
        Integer quantity = request.getQuantity();
        
        // Get ticket type with pessimistic lock
        TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId));
        
        // Validate ticket type is on sale
        if (!ticketType.isOnSale()) {
            throw new InvalidReservationException("Tickets are not currently on sale");
        }
        
        // Validate quantity against per-person limit
        if (quantity > ticketType.getPerPersonLimit()) {
            throw new InvalidReservationException(
                "Quantity exceeds per-person limit of " + ticketType.getPerPersonLimit());
        }
        
        // Try to reserve in Redis (atomic operation)
        boolean reserved = inventoryService.reserveTickets(ticketTypeId, quantity);
        
        if (!reserved) {
            Integer available = inventoryService.getAvailableQuantity(ticketTypeId);
            throw new InsufficientInventoryException(quantity, available);
        }
        
        try {
            // Create reservation record
            TicketReservation reservation = new TicketReservation();
            reservation.setUserId(userId);
            reservation.setTicketTypeId(ticketTypeId);
            reservation.setQuantity(quantity);
            reservation.setReservedUntil(LocalDateTime.now().plusMinutes(reservationTimeoutMinutes));
            reservation.setStatus(ReservationStatus.ACTIVE);
            
            TicketReservation savedReservation = reservationRepository.save(reservation);
            
            // Update ticket type reserved count
            ticketType.setQuantityReserved(ticketType.getQuantityReserved() + quantity);
            ticketTypeRepository.save(ticketType);
            
            logger.info("Reserved {} tickets for user: {} on ticket type: {}", 
                       quantity, userId, ticketTypeId);
            
            return toReservationDto(savedReservation);
            
        } catch (Exception e) {
            // Rollback Redis reservation on failure
            inventoryService.releaseReservation(ticketTypeId, quantity);
            throw e;
        }
    }
    
    @Override
    public void cancelReservation(UUID reservationId, UUID userId) {
        TicketReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new InvalidReservationException("Reservation not found"));
        
        // Validate user owns the reservation
        if (!reservation.getUserId().equals(userId)) {
            throw new InvalidReservationException("Not authorized to cancel this reservation");
        }
        
        // Validate reservation is active
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationException("Reservation is not active");
        }
        
        // Release inventory
        inventoryService.releaseReservation(reservation.getTicketTypeId(), reservation.getQuantity());
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        
        // Update ticket type reserved count
        TicketType ticketType = ticketTypeRepository.findById(reservation.getTicketTypeId())
            .orElseThrow(() -> new TicketTypeNotFoundException(reservation.getTicketTypeId()));
        ticketType.setQuantityReserved(ticketType.getQuantityReserved() - reservation.getQuantity());
        ticketTypeRepository.save(ticketType);
        
        logger.info("Cancelled reservation: {} for user: {}", reservationId, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReservationDto> getUserActiveReservations(UUID userId) {
        List<TicketReservation> reservations = reservationRepository
            .findByUserIdAndStatus(userId, ReservationStatus.ACTIVE);
        
        return reservations.stream()
            .map(this::toReservationDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public void cleanupExpiredReservations() {
        List<TicketReservation> expiredReservations = reservationRepository
            .findExpiredReservations(ReservationStatus.ACTIVE, LocalDateTime.now());
        
        for (TicketReservation reservation : expiredReservations) {
            try {
                // Release inventory
                inventoryService.releaseReservation(
                    reservation.getTicketTypeId(), 
                    reservation.getQuantity()
                );
                
                // Update reservation status
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                
                // Update ticket type reserved count
                TicketType ticketType = ticketTypeRepository.findById(reservation.getTicketTypeId())
                    .orElse(null);
                if (ticketType != null) {
                    ticketType.setQuantityReserved(
                        ticketType.getQuantityReserved() - reservation.getQuantity()
                    );
                    ticketTypeRepository.save(ticketType);
                }
                
                logger.info("Cleaned up expired reservation: {}", reservation.getId());
                
            } catch (Exception e) {
                logger.error("Error cleaning up reservation: {}", reservation.getId(), e);
            }
        }
        
        logger.info("Cleaned up {} expired reservations", expiredReservations.size());
    }
    
    private ReservationDto toReservationDto(TicketReservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUserId());
        dto.setTicketTypeId(reservation.getTicketTypeId());
        dto.setQuantity(reservation.getQuantity());
        dto.setReservedUntil(reservation.getReservedUntil());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        return dto;
    }
}
