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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private UUID ticketId;
    private UUID ticketTypeId;
    private UUID orderId;
    private UUID eventId;
    private Ticket testTicket;
    private TicketDto testTicketDto;
    private TicketType testTicketType;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        testTicketType = new TicketType();
        testTicketType.setId(ticketTypeId);
        testTicketType.setEventId(eventId);
        testTicketType.setName("General Admission");
        testTicketType.setPrice(new BigDecimal("50.00"));

        testTicket = new Ticket();
        testTicket.setId(ticketId);
        testTicket.setTicketTypeId(ticketTypeId);
        testTicket.setOrderId(orderId);
        testTicket.setTicketNumber("TKT-12345678-20240115120000-1234");
        testTicket.setQrCode("base64encodedqrcode");
        testTicket.setHolderName("John Doe");
        testTicket.setStatus(Ticket.TicketStatus.ACTIVE);

        testTicketDto = new TicketDto();
        testTicketDto.setId(ticketId);
        testTicketDto.setTicketTypeId(ticketTypeId);
        testTicketDto.setOrderId(orderId);
        testTicketDto.setTicketNumber("TKT-12345678-20240115120000-1234");
        testTicketDto.setStatus(Ticket.TicketStatus.ACTIVE);
    }

    // ========== Ticket Generation Tests ==========

    @Test
    void generateTickets_WithValidRequest_ShouldGenerateTickets() {
        GenerateTicketsRequest request = new GenerateTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setOrderId(orderId);
        request.setQuantity(3);
        request.setHolderName("John Doe");

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(UUID.randomUUID());
            }
            return ticket;
        });
        when(qrCodeService.generateQRCode(anyString(), anyString())).thenReturn("base64qrcode");
        when(ticketMapper.toDto(any(Ticket.class))).thenReturn(testTicketDto);

        List<TicketDto> result = ticketService.generateTickets(request);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(ticketTypeRepository).findById(ticketTypeId);
        verify(ticketRepository, times(6)).save(any(Ticket.class)); // 2 saves per ticket (before and after QR)
        verify(qrCodeService, times(3)).generateQRCode(anyString(), anyString());
    }

    @Test
    void generateTickets_WithInvalidTicketType_ShouldThrowException() {
        GenerateTicketsRequest request = new GenerateTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setOrderId(orderId);
        request.setQuantity(2);

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.empty());

        assertThrows(TicketTypeNotFoundException.class, () ->
            ticketService.generateTickets(request)
        );
    }

    @Test
    void generateTickets_ShouldGenerateUniqueTicketNumbers() {
        GenerateTicketsRequest request = new GenerateTicketsRequest();
        request.setTicketTypeId(ticketTypeId);
        request.setOrderId(orderId);
        request.setQuantity(2);
        request.setHolderName("Jane Smith");

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(testTicketType));
        when(ticketRepository.existsByTicketNumber(anyString())).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(UUID.randomUUID());
            }
            return ticket;
        });
        when(qrCodeService.generateQRCode(anyString(), anyString())).thenReturn("qrcode");
        when(ticketMapper.toDto(any(Ticket.class))).thenReturn(testTicketDto);

        List<TicketDto> result = ticketService.generateTickets(request);

        assertEquals(2, result.size());
        verify(ticketRepository, atLeast(2)).existsByTicketNumber(anyString());
    }

    // ========== Ticket Retrieval Tests ==========

    @Test
    void getTicketById_WithValidId_ShouldReturnTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(ticketMapper.toDto(testTicket)).thenReturn(testTicketDto);

        TicketDto result = ticketService.getTicketById(ticketId);

        assertNotNull(result);
        assertEquals(ticketId, result.getId());
        verify(ticketRepository).findById(ticketId);
    }

    @Test
    void getTicketById_WithInvalidId_ShouldThrowException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
            ticketService.getTicketById(ticketId)
        );
    }

    @Test
    void getTicketByNumber_WithValidNumber_ShouldReturnTicket() {
        String ticketNumber = "TKT-12345678-20240115120000-1234";
        when(ticketRepository.findByTicketNumber(ticketNumber)).thenReturn(Optional.of(testTicket));
        when(ticketMapper.toDto(testTicket)).thenReturn(testTicketDto);

        TicketDto result = ticketService.getTicketByNumber(ticketNumber);

        assertNotNull(result);
        assertEquals(ticketNumber, result.getTicketNumber());
        verify(ticketRepository).findByTicketNumber(ticketNumber);
    }

    @Test
    void getTicketByNumber_WithInvalidNumber_ShouldThrowException() {
        String ticketNumber = "INVALID-NUMBER";
        when(ticketRepository.findByTicketNumber(ticketNumber)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
            ticketService.getTicketByNumber(ticketNumber)
        );
    }

    @Test
    void getTicketsByOrderId_ShouldReturnTicketList() {
        List<Ticket> tickets = Arrays.asList(testTicket);
        when(ticketRepository.findByOrderId(orderId)).thenReturn(tickets);
        when(ticketMapper.toDto(testTicket)).thenReturn(testTicketDto);

        List<TicketDto> result = ticketService.getTicketsByOrderId(orderId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketRepository).findByOrderId(orderId);
    }

    // ========== Ticket Cancellation Tests ==========

    @Test
    void cancelTicket_WithActiveTicket_ShouldCancelTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ticketService.cancelTicket(ticketId);

        assertEquals(Ticket.TicketStatus.CANCELLED, testTicket.getStatus());
        verify(ticketRepository).save(testTicket);
    }

    @Test
    void cancelTicket_WithAlreadyCancelledTicket_ShouldNotThrowException() {
        testTicket.setStatus(Ticket.TicketStatus.CANCELLED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));

        ticketService.cancelTicket(ticketId);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancelTicket_WithInvalidId_ShouldThrowException() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
            ticketService.cancelTicket(ticketId)
        );
    }

    // ========== QR Code Validation Tests ==========

    @Test
    void validateTicket_WithValidQRCode_ShouldReturnTicket() {
        String qrCode = "TICKET:123:TKT-12345678-20240115120000-1234";
        
        when(qrCodeService.validateQRCode(qrCode)).thenReturn(true);
        when(ticketRepository.findByQrCode(qrCode)).thenReturn(Optional.of(testTicket));
        when(ticketMapper.toDto(testTicket)).thenReturn(testTicketDto);

        TicketDto result = ticketService.validateTicket(qrCode);

        assertNotNull(result);
        verify(qrCodeService).validateQRCode(qrCode);
        verify(ticketRepository).findByQrCode(qrCode);
    }

    @Test
    void validateTicket_WithInvalidQRCodeFormat_ShouldThrowException() {
        String qrCode = "INVALID-FORMAT";
        
        when(qrCodeService.validateQRCode(qrCode)).thenReturn(false);

        assertThrows(TicketNotFoundException.class, () ->
            ticketService.validateTicket(qrCode)
        );
    }

    @Test
    void validateTicket_WithNonExistentQRCode_ShouldThrowException() {
        String qrCode = "TICKET:999:TKT-NONEXISTENT";
        
        when(qrCodeService.validateQRCode(qrCode)).thenReturn(true);
        when(ticketRepository.findByQrCode(qrCode)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
            ticketService.validateTicket(qrCode)
        );
    }
}
