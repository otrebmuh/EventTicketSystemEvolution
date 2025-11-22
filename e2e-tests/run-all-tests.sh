#!/bin/bash

# End-to-End Test Runner for Event Ticket Booking System
# Tests Requirements: All system requirements (1-12)

set -e

echo "=========================================="
echo "Event Ticket Booking System E2E Tests"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo "ℹ $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

print_success "Docker is running"

# Navigate to project root
cd "$(dirname "$0")/.."

# Check if services are running
print_info "Checking if services are running..."
if ! docker-compose ps | grep -q "Up"; then
    print_warning "Services are not running. Starting services..."
    docker-compose up -d
    
    print_info "Waiting for services to be healthy (this may take 2-3 minutes)..."
    sleep 30
    
    # Wait for services to be healthy
    max_attempts=60
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if docker-compose ps | grep -q "healthy"; then
            print_success "Services are healthy"
            break
        fi
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    echo ""
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Services did not become healthy in time"
        docker-compose ps
        docker-compose logs --tail=50
        exit 1
    fi
else
    print_success "Services are already running"
fi

# Create reports directory
mkdir -p e2e-tests/reports

# Test execution summary
total_tests=0
passed_tests=0
failed_tests=0

echo ""
echo "=========================================="
echo "1. Running User Journey Tests"
echo "=========================================="
echo ""

cd e2e-tests/user-journey-tests

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_info "Installing dependencies..."
    npm install
fi

# Run user journey tests
if npm test; then
    print_success "User Journey Tests PASSED"
    passed_tests=$((passed_tests + 1))
else
    print_error "User Journey Tests FAILED"
    failed_tests=$((failed_tests + 1))
fi
total_tests=$((total_tests + 1))

cd ../..

echo ""
echo "=========================================="
echo "2. Running Load Tests (5 minute duration)"
echo "=========================================="
echo ""

cd e2e-tests/load-tests

# Install Python dependencies if needed
if ! python3 -c "import locust" 2>/dev/null; then
    print_info "Installing Python dependencies..."
    pip3 install -r requirements.txt
fi

# Run load tests
print_info "Starting load test with 100 users, spawn rate 10/sec, duration 5 minutes..."
print_warning "This will take approximately 5 minutes..."

if locust -f locustfile.py \
    --headless \
    --users 100 \
    --spawn-rate 10 \
    --run-time 5m \
    --host http://localhost:8091 \
    --html ../reports/load-test-report.html \
    --csv ../reports/load-test; then
    print_success "Load Tests PASSED"
    passed_tests=$((passed_tests + 1))
else
    print_error "Load Tests FAILED"
    failed_tests=$((failed_tests + 1))
fi
total_tests=$((total_tests + 1))

cd ../..

echo ""
echo "=========================================="
echo "3. Running Disaster Recovery Tests"
echo "=========================================="
echo ""

cd e2e-tests/disaster-recovery-tests

# Run disaster recovery tests
if mvn clean test; then
    print_success "Disaster Recovery Tests PASSED"
    passed_tests=$((passed_tests + 1))
else
    print_error "Disaster Recovery Tests FAILED"
    failed_tests=$((failed_tests + 1))
fi
total_tests=$((total_tests + 1))

cd ../..

# Print summary
echo ""
echo "=========================================="
echo "Test Execution Summary"
echo "=========================================="
echo ""
echo "Total Test Suites: $total_tests"
print_success "Passed: $passed_tests"
if [ $failed_tests -gt 0 ]; then
    print_error "Failed: $failed_tests"
else
    echo "Failed: $failed_tests"
fi
echo ""

# Print report locations
echo "Test Reports:"
echo "  - User Journey: e2e-tests/user-journey-tests/reports/"
echo "  - Load Tests: e2e-tests/reports/load-test-report.html"
echo "  - Disaster Recovery: e2e-tests/disaster-recovery-tests/target/surefire-reports/"
echo ""

# Exit with appropriate code
if [ $failed_tests -gt 0 ]; then
    print_error "Some tests failed. Please check the reports for details."
    exit 1
else
    print_success "All E2E tests passed successfully!"
    echo ""
    echo "=========================================="
    echo "✅ System is ready for production"
    echo "=========================================="
    exit 0
fi
