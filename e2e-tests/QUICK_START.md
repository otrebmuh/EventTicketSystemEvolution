# E2E Tests Quick Start Guide

## Prerequisites

Ensure you have the following installed:
- Docker Desktop (running)
- Node.js 18+ and npm
- Python 3.8+ and pip
- Java 17+ and Maven
- Git

## Quick Start (5 minutes)

### 1. Start All Services

```bash
# From project root
docker-compose up -d

# Wait for services to be healthy (2-3 minutes)
docker-compose ps
```

All services should show "healthy" status.

### 2. Run All E2E Tests

```bash
cd e2e-tests
./run-all-tests.sh
```

This will run:
- ✅ User journey tests (~2 minutes)
- ✅ Load tests (~5 minutes)
- ✅ Disaster recovery tests (~3 minutes)

**Total time: ~10 minutes**

## Run Individual Test Suites

### User Journey Tests Only

```bash
cd e2e-tests/user-journey-tests
npm install
npm test
```

### Load Tests Only

```bash
cd e2e-tests/load-tests
pip install -r requirements.txt

# Run with custom parameters
locust -f locustfile.py \
  --headless \
  --users 50 \
  --spawn-rate 5 \
  --run-time 2m \
  --host http://localhost:8091
```

### Disaster Recovery Tests Only

```bash
cd e2e-tests/disaster-recovery-tests
mvn test
```

## View Test Reports

After running tests, reports are available at:

- **User Journey**: `e2e-tests/user-journey-tests/reports/index.html`
- **Load Tests**: `e2e-tests/reports/load-test-report.html`
- **Disaster Recovery**: `e2e-tests/disaster-recovery-tests/target/surefire-reports/`

## Troubleshooting

### Services won't start

```bash
# Clean up and restart
docker-compose down -v
docker-compose up -d --build
```

### Port conflicts

Ensure these ports are available:
- 5432-5436 (PostgreSQL databases)
- 6379 (Redis)
- 8091-8095 (Microservices)

### Tests failing

1. Check service health:
   ```bash
   docker-compose ps
   docker-compose logs auth-service
   ```

2. Verify services are responding:
   ```bash
   curl http://localhost:8091/actuator/health
   curl http://localhost:8092/actuator/health
   ```

3. Check database connections:
   ```bash
   docker-compose logs auth-db
   ```

## Test Coverage

These E2E tests validate:

### ✅ Complete User Journeys
- Registration → Email verification → Login
- Event browsing → Ticket selection → Purchase
- Order management → Ticket delivery
- Password reset flows
- Event organizer workflows

### ✅ System Performance
- 100+ concurrent users
- Simultaneous ticket purchases
- No overselling under load
- Response times < 3 seconds
- 99.9% availability

### ✅ Disaster Recovery
- Database failure and recovery
- Redis cache failure and fallback
- Service restart and state recovery
- External service failures
- Message queue failures
- Circuit breaker patterns
- Retry mechanisms

## CI/CD Integration

Add to your CI/CD pipeline:

```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start services
        run: docker-compose up -d
      - name: Run E2E tests
        run: cd e2e-tests && ./run-all-tests.sh
      - name: Upload reports
        uses: actions/upload-artifact@v3
        with:
          name: e2e-reports
          path: e2e-tests/reports/
```

## Next Steps

1. Review test reports for any failures
2. Add custom test scenarios for your specific use cases
3. Integrate tests into your CI/CD pipeline
4. Set up monitoring alerts based on test results
5. Schedule regular load tests to validate performance

## Support

For issues or questions:
1. Check service logs: `docker-compose logs [service-name]`
2. Review test output for specific error messages
3. Ensure all prerequisites are installed and up to date
