# Load Testing Guide

## Overview

This directory contains load tests for the Event Ticket Booking System using Locust, a modern load testing framework.

## Test Scenarios

### 1. Normal Load (Baseline)
Simulates typical daily traffic:
```bash
locust -f locustfile.py --headless \
  --users 50 \
  --spawn-rate 5 \
  --run-time 5m \
  --host http://localhost:8091
```

### 2. Peak Load (High Traffic)
Simulates peak hours (e.g., popular event goes on sale):
```bash
locust -f locustfile.py --headless \
  --users 200 \
  --spawn-rate 20 \
  --run-time 10m \
  --host http://localhost:8091
```

### 3. Stress Test (Breaking Point)
Finds system limits:
```bash
locust -f locustfile.py --headless \
  --users 500 \
  --spawn-rate 50 \
  --run-time 15m \
  --host http://localhost:8091
```

### 4. Spike Test (Sudden Traffic)
Simulates sudden traffic spike:
```bash
locust -f locustfile.py --headless \
  --users 300 \
  --spawn-rate 100 \
  --run-time 5m \
  --host http://localhost:8091
```

### 5. Endurance Test (Long Duration)
Tests for memory leaks and degradation:
```bash
locust -f locustfile.py --headless \
  --users 100 \
  --spawn-rate 10 \
  --run-time 2h \
  --host http://localhost:8091
```

## Interactive Mode

Run with web UI for real-time monitoring:

```bash
locust -f locustfile.py --host http://localhost:8091
```

Then open http://localhost:8089 in your browser.

## User Behavior Patterns

The load test simulates two types of users:

### EventTicketUser (80% of traffic)
- Browse events (30% of actions)
- Search events (20% of actions)
- View event details (10% of actions)
- Check ticket availability (10% of actions)
- Reserve tickets (10% of actions)
- View profile (10% of actions)

### EventOrganizerUser (20% of traffic)
- Create events (66% of actions)
- Create ticket types (34% of actions)

## Performance Requirements

Based on system requirements (10.1-10.5):

| Metric | Target | Critical |
|--------|--------|----------|
| Response Time (avg) | < 2s | < 3s |
| Response Time (95th) | < 3s | < 5s |
| Throughput | > 100 req/s | > 50 req/s |
| Error Rate | < 1% | < 5% |
| Concurrent Users | 1000+ | 500+ |

## Monitoring During Tests

### 1. System Metrics
```bash
# CPU and Memory
docker stats

# Service logs
docker-compose logs -f auth-service
docker-compose logs -f ticket-service
```

### 2. Database Performance
```bash
# PostgreSQL connections
docker exec -it auth-db psql -U auth_user -d auth_service -c "SELECT count(*) FROM pg_stat_activity;"

# Slow queries
docker exec -it auth-db psql -U auth_user -d auth_service -c "SELECT query, calls, total_time FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"
```

### 3. Redis Performance
```bash
# Redis stats
docker exec -it redis-cache redis-cli INFO stats

# Monitor commands
docker exec -it redis-cache redis-cli MONITOR
```

## Analyzing Results

### Response Time Analysis
```bash
# View response time distribution
cat reports/load-test_stats.csv | column -t -s,
```

### Failure Analysis
```bash
# View failures
cat reports/load-test_failures.csv | column -t -s,
```

### HTML Report
Open `reports/load-test-report.html` in a browser for:
- Response time charts
- Request distribution
- Failure rates
- Percentile analysis

## Common Issues and Solutions

### High Response Times
- **Cause**: Database query performance
- **Solution**: Check slow query log, add indexes

### Connection Errors
- **Cause**: Connection pool exhausted
- **Solution**: Increase pool size in application.yml

### 503 Service Unavailable
- **Cause**: Service overload
- **Solution**: Scale horizontally, add more instances

### Memory Issues
- **Cause**: Memory leak or insufficient memory
- **Solution**: Increase container memory, check for leaks

## Best Practices

1. **Warm-up Period**: Start with low load to warm up caches
2. **Gradual Ramp-up**: Increase load gradually to identify breaking points
3. **Realistic Data**: Use realistic test data and scenarios
4. **Monitor Everything**: Watch system metrics during tests
5. **Baseline First**: Establish baseline before optimization
6. **Repeat Tests**: Run multiple times for consistency

## Integration with CI/CD

### Nightly Performance Tests
```yaml
# .github/workflows/nightly-load-test.yml
name: Nightly Load Test
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM daily
jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run load test
        run: |
          cd e2e-tests/load-tests
          pip install -r requirements.txt
          locust -f locustfile.py --headless \
            --users 100 --spawn-rate 10 --run-time 10m \
            --host ${{ secrets.STAGING_URL }}
```

## Performance Benchmarks

Record baseline performance for comparison:

| Date | Users | RPS | Avg Response | 95th Percentile | Errors |
|------|-------|-----|--------------|-----------------|--------|
| 2024-01-15 | 100 | 150 | 1.2s | 2.5s | 0.1% |
| 2024-01-20 | 200 | 280 | 1.5s | 3.2s | 0.3% |

## Advanced Scenarios

### Custom User Behavior
Edit `locustfile.py` to add custom scenarios:

```python
@task(1)
def custom_scenario(self):
    # Your custom test logic
    pass
```

### Distributed Load Testing
Run across multiple machines:

```bash
# Master node
locust -f locustfile.py --master --host http://localhost:8091

# Worker nodes (on other machines)
locust -f locustfile.py --worker --master-host=<master-ip>
```

## Troubleshooting

### Locust won't start
```bash
pip install --upgrade locust
```

### Connection refused
```bash
# Check if services are running
docker-compose ps

# Check service health
curl http://localhost:8091/actuator/health
```

### Out of memory
```bash
# Reduce number of users or use distributed mode
locust -f locustfile.py --users 50 --spawn-rate 5
```
