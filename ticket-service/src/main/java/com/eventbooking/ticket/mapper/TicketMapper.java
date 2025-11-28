package com.eventbooking.ticket.mapper;

import com.eventbooking.ticket.dto.TicketDto;
import com.eventbooking.ticket.entity.Ticket;
import com.eventbooking.ticket.entity.TicketType;
import com.eventbooking.ticket.repository.TicketTypeRepository;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    private final TicketTypeRepository ticketTypeRepository;

    public TicketMapper(TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public TicketDto toDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        TicketDto dto = new TicketDto();
        dto.setId(ticket.getId());
        dto.setTicketTypeId(ticket.getTicketTypeId());
        dto.setOrderId(ticket.getOrderId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setQrCode(ticket.getQrCode());
        dto.setHolderName(ticket.getHolderName());
        dto.setStatus(ticket.getStatus());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());

        // Populate ticket type and event details
        if (ticket.getTicketTypeId() != null) {
            ticketTypeRepository.findById(ticket.getTicketTypeId()).ifPresent(ticketType -> {
                dto.setTicketTypeName(ticketType.getName());
                dto.setVenueZone(ticketType.getVenueZone());
                // Note: eventName, eventDate, venueName would require calling event-service
                // For now, we'll use ticket type name as a fallback for event display
                dto.setEventName(ticketType.getName());
            });
        }

        return dto;
    }

    public Ticket toEntity(TicketDto dto) {
        if (dto == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setId(dto.getId());
        ticket.setTicketTypeId(dto.getTicketTypeId());
        ticket.setOrderId(dto.getOrderId());
        ticket.setTicketNumber(dto.getTicketNumber());
        ticket.setQrCode(dto.getQrCode());
        ticket.setHolderName(dto.getHolderName());
        ticket.setStatus(dto.getStatus());

        return ticket;
    }
}
