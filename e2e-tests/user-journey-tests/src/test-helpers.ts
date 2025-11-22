import { ApiClient } from './api-client';

export function generateUniqueEmail(): string {
  const timestamp = Date.now();
  const random = Math.floor(Math.random() * 10000);
  return `test.user.${timestamp}.${random}@example.com`;
}

export function generateValidPassword(): string {
  return 'TestPassword123!@#';
}

export function generateFutureDate(daysFromNow: number = 30): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString();
}

export function generatePastDate(yearsAgo: number = 25): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - yearsAgo);
  return date.toISOString().split('T')[0];
}

export async function waitForService(
  checkFn: () => Promise<any>,
  maxAttempts: number = 30,
  delayMs: number = 1000
): Promise<boolean> {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      await checkFn();
      return true;
    } catch (error) {
      if (i === maxAttempts - 1) {
        return false;
      }
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }
  return false;
}

export async function waitForAllServices(client: ApiClient): Promise<void> {
  console.log('Waiting for all services to be healthy...');
  
  const services = [
    { name: 'Auth', check: () => client.checkAuthServiceHealth() },
    { name: 'Event', check: () => client.checkEventServiceHealth() },
    { name: 'Ticket', check: () => client.checkTicketServiceHealth() },
    { name: 'Payment', check: () => client.checkPaymentServiceHealth() },
    { name: 'Notification', check: () => client.checkNotificationServiceHealth() },
  ];

  for (const service of services) {
    const isHealthy = await waitForService(service.check, 60, 2000);
    if (!isHealthy) {
      throw new Error(`${service.name} service is not healthy after 2 minutes`);
    }
    console.log(`✓ ${service.name} service is healthy`);
  }
  
  console.log('All services are healthy and ready for testing');
}

export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export interface TestUser {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  token?: string;
  userId?: string;
}

export async function createTestUser(client: ApiClient): Promise<TestUser> {
  const user: TestUser = {
    email: generateUniqueEmail(),
    password: generateValidPassword(),
    firstName: 'Test',
    lastName: 'User',
    dateOfBirth: generatePastDate(25),
  };

  await client.register(user);
  return user;
}

export async function createAndLoginTestUser(client: ApiClient): Promise<TestUser> {
  const user = await createTestUser(client);
  
  // In a real scenario, we'd verify email first
  // For E2E tests, we might need to mock this or have a test endpoint
  const loginResponse = await client.login({
    email: user.email,
    password: user.password,
  });

  user.token = loginResponse.token;
  user.userId = loginResponse.user?.id;
  
  return user;
}

export interface TestEvent {
  id?: string;
  name: string;
  description: string;
  eventDate: string;
  venueName: string;
  venueAddress: string;
  category: string;
  status: string;
}

export async function createTestEvent(client: ApiClient): Promise<TestEvent> {
  const event: TestEvent = {
    name: `Test Event ${Date.now()}`,
    description: 'This is a test event for E2E testing',
    eventDate: generateFutureDate(30),
    venueName: 'Test Venue',
    venueAddress: '123 Test Street, Test City, TC 12345',
    category: 'CONCERT',
    status: 'PUBLISHED',
  };

  const response = await client.createEvent(event);
  event.id = response.id;
  
  return event;
}

export interface TestTicketType {
  id?: string;
  eventId: string;
  name: string;
  description: string;
  price: number;
  quantityAvailable: number;
  saleStartDate: string;
  saleEndDate: string;
}

export async function createTestTicketType(
  client: ApiClient,
  eventId: string,
  quantity: number = 100
): Promise<TestTicketType> {
  const ticketType: TestTicketType = {
    eventId,
    name: 'General Admission',
    description: 'General admission ticket',
    price: 50.00,
    quantityAvailable: quantity,
    saleStartDate: new Date().toISOString(),
    saleEndDate: generateFutureDate(29),
  };

  const response = await client.createTicketType(ticketType);
  ticketType.id = response.id;
  
  return ticketType;
}
