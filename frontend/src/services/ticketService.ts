import { apiRequest } from './api';

export enum TicketStatus {
  ACTIVE = 'ACTIVE',
  USED = 'USED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
}

export interface Ticket {
  id: string;
  ticketTypeId: string;
  orderId: string;
  ticketNumber: string;
  qrCode: string;
  holderName: string;
  status: TicketStatus;
  createdAt: string;
  updatedAt: string;
  // Event and ticket type details
  eventName?: string;
  eventDate?: string;
  venueName?: string;
  venueAddress?: string;
  ticketTypeName?: string;
  venueZone?: string;
}

export const ticketService = {
  // Get ticket by ID
  async getTicketById(ticketId: string): Promise<Ticket> {
    return apiRequest<Ticket>(`/tickets/${ticketId}`);
  },

  // Get ticket by ticket number
  async getTicketByNumber(ticketNumber: string): Promise<Ticket> {
    return apiRequest<Ticket>(`/tickets/number/${ticketNumber}`);
  },

  // Get tickets by order ID
  async getTicketsByOrderId(orderId: string): Promise<Ticket[]> {
    return apiRequest<Ticket[]>(`/tickets/order/${orderId}`);
  },

  // Get all tickets for a user
  async getTicketsByUserId(userId: string): Promise<Ticket[]> {
    return apiRequest<Ticket[]>(`/tickets/user/${userId}`);
  },

  // Cancel a ticket
  async cancelTicket(ticketId: string): Promise<string> {
    return apiRequest<string>(`/tickets/${ticketId}/cancel`, {
      method: 'POST',
    });
  },

  // Validate a ticket
  async validateTicket(qrCode: string): Promise<Ticket> {
    return apiRequest<Ticket>('/tickets/validate', {
      method: 'POST',
      body: JSON.stringify(qrCode),
    });
  },
};
