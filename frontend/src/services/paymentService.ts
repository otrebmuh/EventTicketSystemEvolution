import { apiRequest } from './api';

export interface OrderItem {
  ticketTypeId: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  eventId: string;
  reservationId?: string;
  items: OrderItem[];
}

export interface OrderItemDto {
  id: string;
  ticketTypeId: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  fees: number;
  totalPrice: number;
  status: string;
  createdAt: string;
}

export interface Order {
  id: string;
  userId: string;
  eventId: string;
  orderNumber: string;
  subtotalAmount: number;
  serviceFee: number;
  taxAmount: number;
  totalAmount: number;
  paymentStatus: string;
  paymentMethod?: string;
  currency: string;
  reservationId?: string;
  createdAt: string;
  updatedAt: string;
  expiresAt?: string;
  orderItems: OrderItemDto[];
}

export interface TicketPurchaseRequest {
  eventId: string;
  ticketTypeId: string;
  quantity: number;
  unitPrice: number;
  paymentMethodId: string;
  reservationId?: string;
}

export interface TicketPurchaseResponse {
  sagaId: string;
  orderId: string;
  orderNumber: string;
  transactionId: string;
  status: string;
  message: string;
}

export interface PaymentResponse {
  transactionId: string;
  status: string;
  clientSecret?: string;
  requiresAction?: boolean;
  errorMessage?: string;
}

export const paymentService = {
  // Create an order
  async createOrder(request: CreateOrderRequest): Promise<Order> {
    return apiRequest<Order>('/payments/orders', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  // Get order by ID
  async getOrderById(orderId: string): Promise<Order> {
    return apiRequest<Order>(`/payments/orders/${orderId}`);
  },

  // Get order by order number
  async getOrderByNumber(orderNumber: string): Promise<Order> {
    return apiRequest<Order>(`/payments/orders/number/${orderNumber}`);
  },

  // Get user orders with pagination
  async getUserOrders(userId: string, page: number = 0, size: number = 10): Promise<{
    content: Order[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }> {
    return apiRequest(`/payments/orders/user/${userId}?page=${page}&size=${size}`);
  },

  // Purchase tickets using saga pattern
  async purchaseTickets(request: TicketPurchaseRequest, userId: string): Promise<TicketPurchaseResponse> {
    return apiRequest<TicketPurchaseResponse>('/payments/purchase-tickets', {
      method: 'POST',
      headers: {
        'X-User-Id': userId,
      },
      body: JSON.stringify(request),
    });
  },

  // Cancel order
  async cancelOrder(orderId: string): Promise<Order> {
    return apiRequest<Order>(`/payments/orders/${orderId}/cancel`, {
      method: 'POST',
    });
  },

  // Confirm order
  async confirmOrder(orderId: string, paymentIntentId?: string): Promise<Order> {
    const url = paymentIntentId
      ? `/payments/orders/${orderId}/confirm?paymentIntentId=${paymentIntentId}`
      : `/payments/orders/${orderId}/confirm`;

    return apiRequest<Order>(url, {
      method: 'POST',
    });
  },
};
