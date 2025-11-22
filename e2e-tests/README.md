# End-to-End Testing Suite

This directory contains comprehensive end-to-end tests for the Event Ticket Booking System MVP.

## Test Categories

### 1. User Journey Tests
Complete flows from registration through ticket purchase and usage:
- User registration → Email verification → Login → Event browsing → Ticket purchase → Ticket delivery
- Event organizer flows: Registration → Event creation → Ticket management
- Password reset flows
- Order cancellation flows

### 2. Load and Performance Tests
System behavior under various load conditions:
- Concurrent user registration (100+ users)
- Simultaneous ticket purchases (preventing overselling)
- High-volume event searches
- Payment processing under load
- Database connection pool stress testing

### 3. Disaster Recovery and Failover Tests
System resilience and recovery:
- Database connection failures and recovery
- Redis cache failures and fallback
- External service failures (payment gateway, email service)
- Service restart and state recovery
- Message queue failures and retry mechanisms

## Prerequisites

### Required Software
- Docker and Docker Compose
- Node.js 18+ (for frontend tests)
- Java 17+ (for backend tests)
- Maven 3.8+
- Python 3.8+ (for load testing with Locust)

### Environment Setup
1. Copy `.env.example` to `.env` and configure:
   ```bash
   cp ../.env.example ../.env
   ```

2. Start all services:
   ```bash
   cd ..
   docker-compose up -d
   ```

3. Wait for services to be healthy:
   ```bash
   docker-compose ps
   ```

## Running Tests

### All E2E Tests
```bash
./run-all-tests.sh
```

### User Journey Tests Only
```bash
cd user-journey-tests
npm install
npm test
```

### Load Tests Only
```bash
cd load-tests
pip install -r requirements.txt
locust -f locustfile.py --headless -u 100 -r 10 -t 5m --host=http://localhost:8091
```

### Disaster Recovery Tests Only
```bash
cd disaster-recovery-tests
mvn test
```

## Test Reports

Test results are generated in:
- `user-journey-tests/reports/` - Journey test results
- `load-tests/reports/` - Load test metrics and charts
- `disaster-recovery-tests/target/surefire-reports/` - JUnit test reports

## CI/CD Integration

These tests can be integrated into CI/CD pipelines:
- User journey tests run on every PR
- Load tests run nightly
- Disaster recovery tests run weekly

## Troubleshooting

### Services not starting
```bash
docker-compose down -v
docker-compose up -d --build
```

### Database connection issues
Check database health:
```bash
docker-compose ps
docker-compose logs auth-db
```

### Port conflicts
Ensure ports 5432-5436, 6379, 8091-8095 are available.

## Test Coverage

These E2E tests validate:
- ✅ All system requirements (1-12)
- ✅ Security requirements (9.1-9.5)
- ✅ Performance requirements (10.1-10.5)
- ✅ Inter-service communication
- ✅ Data consistency across services
- ✅ Error handling and recovery
- ✅ Concurrent operations
- ✅ System resilience
