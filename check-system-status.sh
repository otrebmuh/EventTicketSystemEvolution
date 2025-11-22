#!/bin/bash

echo "=========================================="
echo "Event Ticket Booking System - Status Check"
echo "=========================================="
echo ""

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running"
    exit 1
fi
echo "✅ Docker is running"
echo ""

# Check containers
echo "Container Status:"
echo "----------------"
docker-compose ps
echo ""

# Check databases
echo "Database Health:"
echo "---------------"
for db in auth-db event-db ticket-db payment-db notification-db; do
    status=$(docker inspect --format='{{.State.Health.Status}}' $db 2>/dev/null || echo "not found")
    if [ "$status" = "healthy" ]; then
        echo "✅ $db: healthy"
    else
        echo "⏳ $db: $status"
    fi
done
echo ""

# Check Redis
redis_status=$(docker inspect --format='{{.State.Health.Status}}' redis-cache 2>/dev/null || echo "not found")
if [ "$redis_status" = "healthy" ]; then
    echo "✅ Redis: healthy"
else
    echo "⏳ Redis: $redis_status"
fi
echo ""

# Check microservices
echo "Microservice Health:"
echo "-------------------"
services=("8091:Auth" "8092:Event" "8093:Ticket" "8094:Payment" "8095:Notification")

for service in "${services[@]}"; do
    IFS=':' read -r port name <<< "$service"
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    if [ "$response" = "200" ]; then
        echo "✅ $name Service (port $port): healthy"
    else
        echo "⏳ $name Service (port $port): starting... (HTTP $response)"
    fi
done
echo ""

# Summary
echo "=========================================="
echo "Quick Test Commands:"
echo "=========================================="
echo ""
echo "# Test Auth Service:"
echo "curl http://localhost:8091/actuator/health"
echo ""
echo "# Register a user:"
echo 'curl -X POST http://localhost:8091/api/auth/register \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"firstName":"Test","lastName":"User","email":"test@example.com","dateOfBirth":"1990-01-01","password":"TestPass123!@#"}'"'"
echo ""
echo "# View logs:"
echo "docker-compose logs -f auth-service"
echo ""
echo "# Stop system:"
echo "docker-compose down"
echo ""
