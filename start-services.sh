#!/bin/bash

# Event Ticket Booking System - Startup Script
# This script builds and starts all services with Docker Compose

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${2}${1}${NC}"
}

print_header() {
    echo ""
    print_message "========================================" "$BLUE"
    print_message "$1" "$BLUE"
    print_message "========================================" "$BLUE"
    echo ""
}

# Check if Docker is running
print_header "Checking Prerequisites"
if ! docker info > /dev/null 2>&1; then
    print_message "❌ Docker is not running. Please start Docker and try again." "$RED"
    exit 1
fi
print_message "✓ Docker is running" "$GREEN"

if ! docker-compose --version > /dev/null 2>&1; then
    print_message "❌ Docker Compose is not installed. Please install it and try again." "$RED"
    exit 1
fi
print_message "✓ Docker Compose is installed" "$GREEN"

# Check if Maven is installed
if ! mvn --version > /dev/null 2>&1; then
    print_message "❌ Maven is not installed. Please install Maven and try again." "$RED"
    exit 1
fi
print_message "✓ Maven is installed" "$GREEN"

# Parse command line arguments
BUILD_JARS=true
CLEAN_VOLUMES=false
DETACHED=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build)
            BUILD_JARS=false
            shift
            ;;
        --clean)
            CLEAN_VOLUMES=true
            shift
            ;;
        -d|--detached)
            DETACHED=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./start-services.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-build    Skip Maven build (use existing JARs)"
            echo "  --clean         Remove all volumes and start fresh"
            echo "  -d, --detached  Run containers in detached mode"
            echo "  -h, --help      Show this help message"
            exit 0
            ;;
        *)
            print_message "Unknown option: $1" "$RED"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Clean volumes if requested
if [ "$CLEAN_VOLUMES" = true ]; then
    print_header "Cleaning Up Existing Containers and Volumes"
    print_message "⚠️  This will remove all data!" "$YELLOW"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v
        print_message "✓ Cleaned up containers and volumes" "$GREEN"
    else
        print_message "Skipping cleanup" "$YELLOW"
    fi
fi

# Build JARs if not skipped
if [ "$BUILD_JARS" = true ]; then
    print_header "Building Application JARs"
    print_message "This may take a few minutes..." "$YELLOW"
    
    if mvn clean package -DskipTests; then
        print_message "✓ Successfully built all services" "$GREEN"
    else
        print_message "❌ Maven build failed" "$RED"
        exit 1
    fi
else
    print_message "Skipping Maven build (using existing JARs)" "$YELLOW"
fi

# Check if .env file exists, create template if not
if [ ! -f .env ]; then
    print_header "Creating Environment Configuration"
    cat > .env << 'EOF'
# Stripe Configuration (Optional - for payment processing)
STRIPE_SECRET_KEY=sk_test_your_stripe_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here

# Email Configuration (Optional - for notifications)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# AWS Configuration (Optional - for messaging and S3)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_S3_BUCKET=event-booking-images

# SQS Queues (Optional)
AWS_SQS_PAYMENT_EVENTS_QUEUE=https://sqs.us-east-1.amazonaws.com/123456789/payment-events
AWS_SQS_PAYMENT_EVENTS_DLQ=https://sqs.us-east-1.amazonaws.com/123456789/payment-events-dlq
AWS_SQS_TICKET_EVENTS_QUEUE=https://sqs.us-east-1.amazonaws.com/123456789/ticket-events
AWS_SQS_TICKET_EVENTS_DLQ=https://sqs.us-east-1.amazonaws.com/123456789/ticket-events-dlq

# SNS Topics (Optional)
AWS_SNS_PAYMENT_EVENTS_TOPIC=arn:aws:sns:us-east-1:123456789:payment-events
AWS_SNS_TICKET_EVENTS_TOPIC=arn:aws:sns:us-east-1:123456789:ticket-events
EOF
    print_message "✓ Created .env file template" "$GREEN"
    print_message "⚠️  Please update .env with your actual credentials" "$YELLOW"
fi

# Start services
print_header "Starting Services with Docker Compose"

if [ "$DETACHED" = true ]; then
    docker-compose up --build -d
    print_message "✓ Services started in detached mode" "$GREEN"
    
    print_header "Service Status"
    docker-compose ps
    
    print_header "Waiting for Services to be Healthy"
    print_message "This may take 30-60 seconds..." "$YELLOW"
    sleep 30
    
    # Check service health
    print_header "Service Health Check"
    services=("auth-service:8091" "event-service:8092" "ticket-service:8093" "payment-service:8094" "notification-service:8095")
    
    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
            print_message "✓ $name is healthy" "$GREEN"
        else
            print_message "⚠️  $name is not responding yet" "$YELLOW"
        fi
    done
    
    print_header "Services Started Successfully!"
    echo ""
    print_message "Service URLs:" "$BLUE"
    echo "  Auth Service:         http://localhost:8091"
    echo "  Event Service:        http://localhost:8092"
    echo "  Ticket Service:       http://localhost:8093"
    echo "  Payment Service:      http://localhost:8094"
    echo "  Notification Service: http://localhost:8095"
    echo ""
    print_message "Database Ports:" "$BLUE"
    echo "  Auth DB:         localhost:5432"
    echo "  Event DB:        localhost:5433"
    echo "  Ticket DB:       localhost:5434"
    echo "  Payment DB:      localhost:5435"
    echo "  Notification DB: localhost:5436"
    echo "  Redis:           localhost:6379"
    echo ""
    print_message "Useful Commands:" "$BLUE"
    echo "  View logs:           docker-compose logs -f [service-name]"
    echo "  Stop services:       docker-compose down"
    echo "  Restart service:     docker-compose restart [service-name]"
    echo "  View all services:   docker-compose ps"
    echo ""
else
    print_message "Starting services in foreground mode..." "$YELLOW"
    print_message "Press Ctrl+C to stop all services" "$YELLOW"
    docker-compose up --build
fi
