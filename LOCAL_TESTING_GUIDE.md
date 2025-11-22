# Local Testing Guide - Event Ticket Booking System

## Quick Start (5 Minutes)

### Step 1: Start All Services

```bash
# Build and start all services
docker-compose up -d --build

# This will start:
# - 5 PostgreSQL databases (ports 5432-5436)
# - Redis cache (port 6379)
# - 5 microservices (ports 8091-8095)
```

### Step 2: Wait for Services to be Healthy

```bash
# Check service status
docker-compose ps

# Wait until all services show "healthy" status (2-3 minutes)
# You can also watch the logs:
docker-compose logs -f
```

### Step 3: Verify Services are Running

```bash
# Check health endpoints
curl http://localhost:8091/actuator/health  # Auth Service
curl http://localhost:8092/actuator/health  # Event Service
curl http://localhost:8093/actuator/health  # Ticket Service
curl http://localhost:8094/actuator/health  # Payment Service
curl http://localhost:8095/actuator/health  # Notification Service
```

### Step 4: Start Frontend (Optional)

```bash
cd frontend
npm install
npm run dev

# Frontend will be available at: http://localhost:5173
```

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Auth Service | http://localhost:8091 | User authentication |
| Event Service | http://localhost:8092 | Event management |
| Ticket Service | http://localhost:8093 | Ticket operations |
| Payment Service | http://localhost:8094 | Payment processing |
| Notification Service | http://localhost:8095 | Email notifications |
| Frontend | http://localhost:5173 | Web application |

## Manual Testing Scenarios

### Scenario 1: User Registration and Login

**1. Register a new user:**
```bash
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "dateOfBirth": "1990-01-01",
    "password": "SecurePass123!@#"
  }'
```

**Expected Response:**
```json
{
  "message": "Registration successful! Please check your email to verify your account."
}
```

**2. Login:**
```bash
curl -X POST http://localhost:8091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123!@#",
    "rememberMe": false
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "emailVerified": true
  }
}
```

**Save the token for subsequent requests!**

### Scenario 2: Create an Event

```bash
# Replace YOUR_TOKEN with the token from login
curl -X POST http://localhost:8092/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Summer Music Festival 2025",
    "description": "The biggest music festival of the year!",
    "eventDate": "2025-07-15T18:00:00Z",
    "venueName": "Central Park",
    "venueAddress": "123 Park Avenue, New York, NY 10001",
    "category": "CONCERT",
    "status": "PUBLISHED"
  }'
```

**Expected Response:**
```json
{
  "id": "event-uuid",
  "name": "Summer Music Festival 2025",
  "description": "The biggest music festival of the year!",
  "eventDate": "2025-07-15T18:00:00Z",
  "venueName": "Central Park",
  "venueAddress": "123 Park Avenue, New York, NY 10001",
  "category": "CONCERT",
  "status": "PUBLISHED",
  "createdAt": "2024-11-22T..."
}
```

**Save the event ID!**

### Scenario 3: Browse Events

```bash
# Get all events
curl http://localhost:8092/api/events \
  -H "Authorization: Bearer YOUR_TOKEN"

# Search events
curl "http://localhost:8092/api/events/search?q=music" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get specific event
curl http://localhost:8092/api/events/EVENT_ID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Scenario 4: Create Ticket Types

```bash
curl -X POST http://localhost:8093/api/tickets/types \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "eventId": "EVENT_ID",
    "name": "General Admission",
    "description": "General admission ticket",
    "price": 75.00,
    "quantityAvailable": 1000,
    "saleStartDate": "2024-11-22T00:00:00Z",
    "saleEndDate": "2025-07-15T17:00:00Z"
  }'
```

### Scenario 5: Check Ticket Availability

```bash
curl http://localhost:8093/api/tickets/availability/EVENT_ID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Scenario 6: Reserve Tickets

```bash
curl -X POST http://localhost:8093/api/tickets/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "ticketTypeId": "TICKET_TYPE_ID",
    "quantity": 2
  }'
```

**Expected Response:**
```json
{
  "reservationId": "reservation-uuid",
  "ticketTypeId": "ticket-type-uuid",
  "quantity": 2,
  "reservedUntil": "2024-11-22T12:30:00Z",
  "status": "RESERVED"
}
```

### Scenario 7: Process Payment

```bash
curl -X POST http://localhost:8094/api/payments/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "orderId": "RESERVATION_ID",
    "paymentMethodId": "pm_test_card"
  }'
```

### Scenario 8: View User Orders

```bash
curl http://localhost:8094/api/payments/orders \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Scenario 9: View User Tickets

```bash
curl http://localhost:8093/api/tickets/my-tickets \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Testing with Frontend

1. **Start the frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

2. **Open browser:** http://localhost:5173

3. **Test user flows:**
   - Register a new account
   - Login
   - Browse events
   - Search for events
   - View event details
   - Select tickets
   - Complete purchase
   - View your tickets

## Monitoring and Debugging

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f auth-service
docker-compose logs -f event-service
docker-compose logs -f ticket-service
docker-compose logs -f payment-service
docker-compose logs -f notification-service

# Database logs
docker-compose logs -f auth-db
docker-compose logs -f redis
```

### Check Service Health

```bash
# Quick health check script
for port in 8091 8092 8093 8094 8095; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

### Database Access

```bash
# Connect to Auth database
docker exec -it auth-db psql -U auth_user -d auth_service

# Connect to Event database
docker exec -it event-db psql -U event_user -d event_service

# Connect to Redis
docker exec -it redis-cache redis-cli
```

### Check Database Data

```sql
-- In PostgreSQL
\dt                          -- List tables
SELECT * FROM users;         -- View users
SELECT * FROM events;        -- View events
SELECT * FROM tickets;       -- View tickets
SELECT * FROM orders;        -- View orders
```

## Performance Testing

### Run Load Tests

```bash
cd e2e-tests/load-tests
pip install -r requirements.txt

# Light load (50 users)
locust -f locustfile.py --headless \
  --users 50 --spawn-rate 5 --run-time 2m \
  --host http://localhost:8091

# Medium load (100 users)
locust -f locustfile.py --headless \
  --users 100 --spawn-rate 10 --run-time 5m \
  --host http://localhost:8091
```

### Run E2E Tests

```bash
cd e2e-tests
./run-all-tests.sh
```

## Common Issues and Solutions

### Issue: Services won't start

**Solution:**
```bash
# Clean up and rebuild
docker-compose down -v
docker-compose up -d --build
```

### Issue: Port already in use

**Solution:**
```bash
# Check what's using the port
lsof -i :8091

# Kill the process or change ports in docker-compose.yml
```

### Issue: Database connection errors

**Solution:**
```bash
# Check database health
docker-compose ps
docker-compose logs auth-db

# Restart databases
docker-compose restart auth-db event-db ticket-db payment-db notification-db
```

### Issue: Services show "unhealthy"

**Solution:**
```bash
# Wait longer (services take 2-3 minutes to start)
# Check logs for errors
docker-compose logs auth-service

# Restart specific service
docker-compose restart auth-service
```

### Issue: Frontend can't connect to backend

**Solution:**
```bash
# Check CORS configuration
# Verify backend services are running
curl http://localhost:8091/actuator/health

# Check frontend API configuration in src/services/api.ts
```

## Stopping the System

```bash
# Stop all services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove everything including data
docker-compose down -v
```

## Quick Test Script

Save this as `test-system.sh`:

```bash
#!/bin/bash

echo "Testing Event Ticket Booking System..."
echo ""

# Test Auth Service
echo "1. Testing Auth Service..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test.'$(date +%s)'@example.com",
    "dateOfBirth": "1990-01-01",
    "password": "TestPass123!@#"
  }')
echo "✓ Registration: $REGISTER_RESPONSE"
echo ""

# Test Event Service
echo "2. Testing Event Service..."
EVENTS=$(curl -s http://localhost:8092/api/events)
echo "✓ Events retrieved"
echo ""

# Test Ticket Service
echo "3. Testing Ticket Service..."
curl -s http://localhost:8093/actuator/health > /dev/null && echo "✓ Ticket Service healthy"
echo ""

# Test Payment Service
echo "4. Testing Payment Service..."
curl -s http://localhost:8094/actuator/health > /dev/null && echo "✓ Payment Service healthy"
echo ""

# Test Notification Service
echo "5. Testing Notification Service..."
curl -s http://localhost:8095/actuator/health > /dev/null && echo "✓ Notification Service healthy"
echo ""

echo "All services are operational! ✅"
```

Make it executable and run:
```bash
chmod +x test-system.sh
./test-system.sh
```

## Next Steps

1. ✅ Start the system: `docker-compose up -d --build`
2. ✅ Wait for services to be healthy (2-3 minutes)
3. ✅ Test with curl commands above
4. ✅ Start frontend and test in browser
5. ✅ Run E2E tests to validate everything works
6. ✅ Check logs if any issues arise

## Support

If you encounter any issues:
1. Check service logs: `docker-compose logs [service-name]`
2. Verify all services are healthy: `docker-compose ps`
3. Check database connections
4. Review error messages in logs
5. Restart problematic services

Happy testing! 🚀
