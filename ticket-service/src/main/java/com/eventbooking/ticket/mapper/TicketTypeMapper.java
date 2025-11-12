package com.eventbooking.ticket.mapper;

import com.eventbooking.ticket.dto.TicketTypeDto;
import com.eventbooking.ticket.entity.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketTypeMapper {
    
    public TicketTypeDto toDto(TicketType ticketType) {
        if (ticketType == null) {
            return null;
        }
        
        TicketTypeDto dto = new TicketTypeDto();
        dto.setId(ticketType.getId());
        dto.setEventId(ticketType.getEventId());
        dto.setName(ticketType.getName());
        dto.setDescription(ticketType.getDescription());
        dto.setPrice(ticketType.getPrice());
        dto.setQuantityAvailable(ticketType.getQuantityAvailable());
        dto.setQuantitySold(ticketType.getQuantitySold());
        dto.setAvailableQuantity(ticketType.getAvailableQuantity());
        dto.setSaleStartDate(ticketType.getSaleStartDate());
        dto.setSaleEndDate(ticketType.getSaleEndDate());
        dto.setPerPersonLimit(ticketType.getPerPersonLimit());
        dto.setVenueZone(ticketType.getVenueZone());
        dto.setIsOnSale(ticketType.isOnSale());
        dto.setCreatedAt(ticketType.getCreatedAt());
        dto.setUpdatedAt(ticketType.getUpdatedAt());
        
        return dto;
    }
}
