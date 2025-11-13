package com.eventbooking.ticket.mapper;

import com.eventbooking.ticket.dto.TicketDto;
import com.eventbooking.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    
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
