package com.eventbooking.ticket.service;

import com.eventbooking.ticket.dto.GenerateTicketsRequest;
import com.eventbooking.ticket.dto.TicketDto;
import com.eventbooking.ticket.entity.Ticket;
import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.exception.TicketNotFoundException;
import com.eventbooking.ticket.exception.TicketTypeNotFoundException;
import com.eventbooking.ticket.mapper.TicketMapper;
import com.eventbooking.ticket.repository.TicketRepository;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);
    private static final DateTimeFormatter TICKET_NUMBER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketMapper ticketMapper;
    private final QRCodeService qrCodeService;
    private final TicketEventPublisher eventPublisher;
    
    public TicketServiceImpl(TicketRepository ticketRepository,
                            TicketTypeRepository ticketTypeRepository,
                            TicketMapper ticketMapper,
                            QRCodeService qrCodeService,
                            TicketEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketMapper = ticketMapper;
        this.qrCodeService = qrCodeService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public List<TicketDto> generateTickets(GenerateTicketsRequest request) {
        logger.info("Generating {} tickets for order: {}", request.getQuantity(), request.getOrderId());
        
        // Validate ticket type exists
        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                .orElseThrow(() -> new TicketTypeNotFoundException(
                        "Ticket type not found: " + request.getTicketTypeId()));
        
        List<TicketDto> generatedTickets = new ArrayList<>();
        
        for (int i = 0; i < request.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setTicketTypeId(request.getTicketTypeId());
            ticket.setOrderId(request.getOrderId());
            ticket.setHolderName(request.getHolderName());
            ticket.setStatus(Ticket.TicketStatus.ACTIVE);
            
            // Generate unique ticket number
            String ticketNumber = generateUniqueTicketNumber(ticketType.getEventId());
            ticket.setTicketNumber(ticketNumber);
            
            // Save ticket first to get the ID
            ticket = ticketRepository.save(ticket);
            
            // Generate QR code with ticket ID and number
            String qrCode = qrCodeService.generateQRCode(
                    ticket.getId().toString(), 
                    ticket.getTicketNumber());
            ticket.setQrCode(qrCode);
            
            // Update ticket with QR code
            ticket = ticketRepository.save(ticket);
            
            generatedTickets.add(ticketMapper.toDto(ticket));
            logger.debug("Generated ticket: {}", ticket.getTicketNumber());
        }
        
        logger.info("Successfully generated {} tickets for order: {}", 
                generatedTickets.size(), request.getOrderId());
        
        // Publish tickets generated event
        // Note: userId and holderEmail would need to be passed in the request in a real implementation
        eventPublisher.publishTicketsGenerated(
                request.getOrderId(),
                null, // userId - would need to be added to request
                ticketType.getEventId(),
                generatedTickets,
                request.getHolderName(),
                null // holderEmail - would need to be added to request
        );
        
        return generatedTickets;
    }

    @Override
    public TicketDto getTicketById(UUID ticketId) {
        logger.debug("Retrieving ticket by ID: {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));
        
        return ticketMapper.toDto(ticket);
    }
    
    @Override
    public TicketDto getTicketByNumber(String ticketNumber) {
        logger.debug("Retrieving ticket by number: {}", ticketNumber);
        
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketNumber));
        
        return ticketMapper.toDto(ticket);
    }
    
    @Override
    public List<TicketDto> getTicketsByOrderId(UUID orderId) {
        logger.debug("Retrieving tickets for order: {}", orderId);
        
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        
        return tickets.stream()
                .map(ticketMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TicketDto> getTicketsByUserId(UUID userId) {
        logger.debug("Retrieving tickets for user: {}", userId);
        
        // Note: This would typically involve calling the Payment Service to get user's orders
        // For now, we'll return an empty list as this requires inter-service communication
        // This will be implemented when Payment Service integration is complete
        logger.warn("getTicketsByUserId requires Payment Service integration - returning empty list");
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public void cancelTicket(UUID ticketId) {
        logger.info("Cancelling ticket: {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));
        
        if (ticket.getStatus() == Ticket.TicketStatus.CANCELLED) {
            logger.warn("Ticket already cancelled: {}", ticketId);
            return;
        }
        
        ticket.setStatus(Ticket.TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        
        // Publish ticket cancelled event
        eventPublisher.publishTicketCancelled(ticketId, ticket.getOrderId(), null);
        
        logger.info("Successfully cancelled ticket: {}", ticketId);
    }
    
    @Override
    public TicketDto validateTicket(String qrCode) {
        logger.debug("Validating ticket with QR code");
        
        if (!qrCodeService.validateQRCode(qrCode)) {
            throw new TicketNotFoundException("Invalid QR code format");
        }
        
        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found for QR code"));
        
        if (ticket.getStatus() != Ticket.TicketStatus.ACTIVE) {
            logger.warn("Ticket validation failed - status: {}", ticket.getStatus());
        }
        
        return ticketMapper.toDto(ticket);
    }

    /**
     * Generate a unique ticket number
     * Format: EVT-{eventId-first8}-{timestamp}-{random4}
     */
    private String generateUniqueTicketNumber(UUID eventId) {
        String eventPrefix = eventId.toString().substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now().format(TICKET_NUMBER_FORMATTER);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        
        String ticketNumber;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            ticketNumber = String.format("TKT-%s-%s-%s", eventPrefix, timestamp, random);
            attempts++;
            
            if (attempts >= maxAttempts) {
                // Add additional randomness if we've tried too many times
                random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                ticketNumber = String.format("TKT-%s-%s-%s", eventPrefix, timestamp, random);
            }
            
        } while (ticketRepository.existsByTicketNumber(ticketNumber) && attempts < maxAttempts * 2);
        
        if (ticketRepository.existsByTicketNumber(ticketNumber)) {
            throw new RuntimeException("Failed to generate unique ticket number after multiple attempts");
        }
        
        return ticketNumber;
    }
}
