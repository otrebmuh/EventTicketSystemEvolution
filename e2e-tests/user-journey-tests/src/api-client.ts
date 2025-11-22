import axios, { AxiosInstance } from 'axios';

export interface UserRegistration {
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  password: string;
}

export interface UserLogin {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface EventCreation {
  name: string;
  description: string;
  eventDate: string;
  venueName: string;
  venueAddress: string;
  category: string;
  status: string;
}

export interface TicketTypeCreation {
  eventId: string;
  name: string;
  description: string;
  price: number;
  quantityAvailable: number;
  saleStartDate: string;
  saleEndDate: string;
}

export interface TicketReservation {
  ticketTypeId: string;
  quantity: number;
}

export interface PaymentRequest {
  orderId: string;
  paymentMethodId: string;
}

export class ApiClient {
  private authClient: AxiosInstance;
  private eventClient: AxiosInstance;
  private ticketClient: AxiosInstance;
  private paymentClient: AxiosInstance;
  private notificationClient: AxiosInstance;
  
  private authToken: string | null = null;

  constructor(
    private baseUrls = {
      auth: 'http://localhost:8091',
      event: 'http://localhost:8092',
      ticket: 'http://localhost:8093',
      payment: 'http://localhost:8094',
      notification: 'http://localhost:8095',
    }
  ) {
    this.authClient = axios.create({ baseURL: baseUrls.auth, timeout: 10000 });
    this.eventClient = axios.create({ baseURL: baseUrls.event, timeout: 10000 });
    this.ticketClient = axios.create({ baseURL: baseUrls.ticket, timeout: 10000 });
    this.paymentClient = axios.create({ baseURL: baseUrls.payment, timeout: 10000 });
    this.notificationClient = axios.create({ baseURL: baseUrls.notification, timeout: 10000 });
  }

  setAuthToken(token: string) {
    this.authToken = token;
    const headers = { Authorization: `Bearer ${token}` };
    this.authClient.defaults.headers.common = headers;
    this.eventClient.defaults.headers.common = headers;
    this.ticketClient.defaults.headers.common = headers;
    this.paymentClient.defaults.headers.common = headers;
    this.notificationClient.defaults.headers.common = headers;
  }

  // Authentication Service APIs
  async register(data: UserRegistration) {
    const response = await this.authClient.post('/api/auth/register', data);
    return response.data;
  }

  async verifyEmail(token: string) {
    const response = await this.authClient.get(`/api/auth/verify-email?token=${token}`);
    return response.data;
  }

  async login(data: UserLogin) {
    const response = await this.authClient.post('/api/auth/login', data);
    if (response.data.token) {
      this.setAuthToken(response.data.token);
    }
    return response.data;
  }

  async logout() {
    const response = await this.authClient.post('/api/auth/logout');
    this.authToken = null;
    return response.data;
  }

  async forgotPassword(email: string) {
    const response = await this.authClient.post('/api/auth/forgot-password', { email });
    return response.data;
  }

  async resetPassword(token: string, newPassword: string) {
    const response = await this.authClient.post('/api/auth/reset-password', { token, newPassword });
    return response.data;
  }

  async getProfile() {
    const response = await this.authClient.get('/api/auth/profile');
    return response.data;
  }

  // Event Service APIs
  async createEvent(data: EventCreation) {
    const response = await this.eventClient.post('/api/events', data);
    return response.data;
  }

  async getEvent(eventId: string) {
    const response = await this.eventClient.get(`/api/events/${eventId}`);
    return response.data;
  }

  async searchEvents(query: string) {
    const response = await this.eventClient.get(`/api/events/search?q=${query}`);
    return response.data;
  }

  async getAllEvents() {
    const response = await this.eventClient.get('/api/events');
    return response.data;
  }

  async updateEvent(eventId: string, data: Partial<EventCreation>) {
    const response = await this.eventClient.put(`/api/events/${eventId}`, data);
    return response.data;
  }

  async deleteEvent(eventId: string) {
    const response = await this.eventClient.delete(`/api/events/${eventId}`);
    return response.data;
  }

  // Ticket Service APIs
  async createTicketType(data: TicketTypeCreation) {
    const response = await this.ticketClient.post('/api/tickets/types', data);
    return response.data;
  }

  async getTicketAvailability(eventId: string) {
    const response = await this.ticketClient.get(`/api/tickets/availability/${eventId}`);
    return response.data;
  }

  async reserveTickets(data: TicketReservation) {
    const response = await this.ticketClient.post('/api/tickets/reserve', data);
    return response.data;
  }

  async purchaseTickets(reservationId: string) {
    const response = await this.ticketClient.post('/api/tickets/purchase', { reservationId });
    return response.data;
  }

  async getUserTickets() {
    const response = await this.ticketClient.get('/api/tickets/my-tickets');
    return response.data;
  }

  async getTicket(ticketId: string) {
    const response = await this.ticketClient.get(`/api/tickets/${ticketId}`);
    return response.data;
  }

  async resendTicket(ticketId: string) {
    const response = await this.ticketClient.post(`/api/tickets/${ticketId}/resend`);
    return response.data;
  }

  // Payment Service APIs
  async processPayment(data: PaymentRequest) {
    const response = await this.paymentClient.post('/api/payments/process', data);
    return response.data;
  }

  async getOrder(orderId: string) {
    const response = await this.paymentClient.get(`/api/payments/orders/${orderId}`);
    return response.data;
  }

  async getUserOrders() {
    const response = await this.paymentClient.get('/api/payments/orders');
    return response.data;
  }

  async refundOrder(orderId: string) {
    const response = await this.paymentClient.post(`/api/payments/refund`, { orderId });
    return response.data;
  }

  // Health check APIs
  async checkAuthServiceHealth() {
    const response = await this.authClient.get('/actuator/health');
    return response.data;
  }

  async checkEventServiceHealth() {
    const response = await this.eventClient.get('/actuator/health');
    return response.data;
  }

  async checkTicketServiceHealth() {
    const response = await this.ticketClient.get('/actuator/health');
    return response.data;
  }

  async checkPaymentServiceHealth() {
    const response = await this.paymentClient.get('/actuator/health');
    return response.data;
  }

  async checkNotificationServiceHealth() {
    const response = await this.notificationClient.get('/actuator/health');
    return response.data;
  }

  async checkAllServicesHealth() {
    const results = await Promise.allSettled([
      this.checkAuthServiceHealth(),
      this.checkEventServiceHealth(),
      this.checkTicketServiceHealth(),
      this.checkPaymentServiceHealth(),
      this.checkNotificationServiceHealth(),
    ]);
    
    return {
      auth: results[0].status === 'fulfilled' ? results[0].value : null,
      event: results[1].status === 'fulfilled' ? results[1].value : null,
      ticket: results[2].status === 'fulfilled' ? results[2].value : null,
      payment: results[3].status === 'fulfilled' ? results[3].value : null,
      notification: results[4].status === 'fulfilled' ? results[4].value : null,
    };
  }
}
