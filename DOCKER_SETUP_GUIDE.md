# Docker Setup Guide

This guide will help you run the Event Ticket Booking System using Docker Compose.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose installed
- Maven installed (for building JARs)
- At least 4GB of available RAM
- Ports 5432-5436, 6379, and 8081-8085 available

## Quick Start

### Option 1: Using the Startup Script (Recommended)

```bash
# Make the script executable (first time only)
chmod +x start-services.sh

# Start all services
./start-services.sh

# Start in detached mode (background)
./start-services.sh -d

# Clean start (removes all data)
./start-services.sh --clean -d

# Skip Maven build (use existing JARs)
./start-services.sh --skip-build -d
```

### Option 2: Manual Docker Compose

```bash
# Build JARs
mvn clean package -DskipTests

# Start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

## Service URLs

Once started, services are available at:

- **Auth Service**: http://localhost:8081
  - Health: http://localhost:8081/actuator/health
  - API Docs: http://localhost:8081/swagger-ui.html

- **Event Service**: http://localhost:8082
  - Health: http://localhost:8082/actuator/health
  - API Docs: http://localhost:8082/swagger-ui.html

- **Ticket Service**: http://localhost:8083
  - Health: http://localhost:8083/actuator/health
  - API Docs: http://localhost:8083/swagger-ui.html

- **Payment Service**: http://localhost:8084
  - Health: http://localhost:8084/actuator/health
  - API Docs: http://localhost:8084/swagger-ui.html
  - Saga Monitor: http://localhost:8084/api/saga/monitor

- **Notification Service**: http://localhost:8085
  - Health: http://localhost:8085/actuator/health
  - API Docs: http://localhost:8085/swagger-ui.html

## Database Connections

Direct database access (for debugging):

```bash
# Auth Database
psql -h localhost -p 5432 -U auth_user -d auth_service

# Event Database
psql -h localhost -p 5433 -U event_user -d event_service

# Ticket Database
psql -h localhost -p 5434 -U ticket_user -d ticket_service

# Payment Database
psql -h localhost -p 5435 -U payment_user -d payment_service

# Notification Database
psql -h localhost -p 5436 -U notification_user -d notification_service

# Redis
redis-cli -h localhost -p 6379
```

Default passwords match the usernames (e.g., `auth_password` for `auth_user`).

## Environment Configuration

The system uses a `.env` file for sensitive configuration. On first run, a template is created automatically.

### Required Configuration (for full functionality)

Edit `.env` file:

```bash
# Stripe (for payment processing)
STRIPE_SECRET_KEY=sk_test_your_actual_key
STRIPE_WEBHOOK_SECRET=whsec_your_actual_secret

# Email (for notifications)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-specific-password

# AWS (for messaging and image storage)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_S3_BUCKET=your-bucket-name
```

### What Works Without Configuration

The system will start and run without the above configuration, but:
- ✅ All REST APIs work
- ✅ Database operations work
- ✅ Inter-service communication works
- ✅ Saga pattern (distributed transactions) works
- ❌ Payment processing requires Stripe keys
- ❌ Email notifications require SMTP config
- ❌ Asynchronous messaging requires AWS SQS/SNS
- ❌ Image uploads require AWS S3

## Common Commands

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f auth-service
docker-compose logs -f payment-service

# Last 100 lines
docker-compose logs --tail=100 -f
```

### Managing Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Restart a specific service
docker-compose restart payment-service

# Rebuild and restart a service
docker-compose up -d --build payment-service

# View running containers
docker-compose ps

# View resource usage
docker stats
```

### Database Management

```bash
# Backup a database
docker exec auth-db pg_dump -U auth_user auth_service > backup.sql

# Restore a database
docker exec -i auth-db psql -U auth_user auth_service < backup.sql

# Reset a database (removes all data)
docker-compose down -v
docker-compose up -d auth-db
```

## Testing the System

### 1. Check Service Health

```bash
# Check all services
for port in 8081 8082 8083 8084 8085; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

### 2. Register a User

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

### 4. Create an Event

```bash
# Save the token from login response
TOKEN="your-jwt-token-here"

curl -X POST http://localhost:8082/api/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Concert",
    "description": "A test event",
    "eventDate": "2025-12-31T20:00:00",
    "venueName": "Test Venue",
    "venueAddress": "123 Test St",
    "city": "Test City",
    "state": "TS",
    "country": "Test Country",
    "category": "MUSIC"
  }'
```

## Troubleshooting

### Services Won't Start

1. **Check Docker is running**: `docker info`
2. **Check port conflicts**: `lsof -i :8081` (or other ports)
3. **Check logs**: `docker-compose logs [service-name]`
4. **Rebuild**: `docker-compose down && docker-compose up --build`

### Database Connection Errors

1. **Wait for health checks**: Databases take 10-15 seconds to initialize
2. **Check database logs**: `docker-compose logs auth-db`
3. **Verify connection**: `docker exec auth-db pg_isready -U auth_user`

### Out of Memory

1. **Increase Docker memory**: Docker Desktop → Settings → Resources
2. **Stop other containers**: `docker stop $(docker ps -q)`
3. **Clean up**: `docker system prune -a`

### Build Failures

1. **Clean Maven cache**: `mvn clean`
2. **Update dependencies**: `mvn dependency:resolve`
3. **Check Java version**: `java -version` (should be 17+)

## Development Workflow

### Making Code Changes

```bash
# 1. Make your code changes

# 2. Rebuild the specific service
mvn clean package -pl payment-service -DskipTests

# 3. Restart the service
docker-compose up -d --build payment-service

# 4. View logs
docker-compose logs -f payment-service
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
mvn test -pl payment-service

# Run specific test class
mvn test -pl payment-service -Dtest=OrderServiceImplTest
```

## Production Considerations

Before deploying to production:

1. **Change all default passwords** in docker-compose.yml
2. **Use secrets management** for sensitive data
3. **Enable SSL/TLS** for all services
4. **Set up proper monitoring** and logging
5. **Configure backup strategies** for databases
6. **Use production-grade message queues** (AWS SQS/SNS or RabbitMQ)
7. **Set up load balancing** for high availability
8. **Configure proper resource limits** in docker-compose.yml
9. **Enable authentication** between services
10. **Set up CI/CD pipelines** for automated deployments

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Redis Docker Hub](https://hub.docker.com/_/redis)

## Support

For issues or questions:
1. Check the logs: `docker-compose logs -f`
2. Review this guide
3. Check service health endpoints
4. Verify environment configuration in `.env`
