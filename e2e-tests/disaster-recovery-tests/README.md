# Disaster Recovery and Failover Tests

## Overview

This test suite validates the system's resilience and ability to recover from various failure scenarios. These tests ensure the system meets high availability requirements (99.9% uptime) and handles failures gracefully.

## Test Categories

### 1. Database Failures
- **Connection Loss**: Database becomes unreachable
- **Connection Pool Exhaustion**: All connections in use
- **Slow Queries**: Database performance degradation
- **Recovery**: Automatic reconnection after database restart

### 2. Cache Failures
- **Redis Unavailable**: Cache service down
- **Cache Eviction**: Memory pressure causes evictions
- **Fallback Behavior**: System continues without cache
- **Performance Impact**: Degraded but functional

### 3. Service Failures
- **Service Crash**: Individual service goes down
- **Service Restart**: State recovery after restart
- **Cascading Failures**: Multiple services fail
- **Circuit Breaker**: Prevents cascade

### 4. External Service Failures
- **Payment Gateway**: Stripe API unavailable
- **Email Service**: SES/SendGrid unavailable
- **S3 Storage**: Image storage unavailable
- **Graceful Degradation**: Core functionality maintained

### 5. Network Failures
- **Timeout**: Slow network responses
- **Intermittent**: Sporadic connection issues
- **Partition**: Network split between services
- **Retry Logic**: Automatic retry with backoff

### 6. Data Consistency
- **Distributed Transactions**: Saga pattern validation
- **Partial Failures**: Rollback mechanisms
- **Eventual Consistency**: Async updates
- **Idempotency**: Duplicate request handling

## Running Tests

### All Tests
```bash
mvn test
```

### Specific Test Class
```bash
mvn test -Dtest=DisasterRecoveryTest
```

### Single Test Method
```bash
mvn test -Dtest=DisasterRecoveryTest#testDatabaseConnectionFailure
```

### With Detailed Output
```bash
mvn test -X
```

## Test Scenarios

### Scenario 1: Database Connection Failure

**Objective**: Verify system handles database unavailability gracefully

**Steps**:
1. Stop PostgreSQL container
2. Attempt database operations
3. Verify appropriate error responses (503 Service Unavailable)
4. Restart PostgreSQL
5. Verify automatic recovery
6. Confirm operations succeed

**Expected Results**:
- ✅ No application crashes
- ✅ Appropriate error messages
- ✅ Automatic reconnection
- ✅ Full functionality restored

### Scenario 2: Redis Cache Failure

**Objective**: Verify system continues without cache

**Steps**:
1. Stop Redis container
2. Perform cached operations
3. Verify operations complete (slower)
4. Restart Redis
5. Verify cache repopulation

**Expected Results**:
- ✅ Operations succeed without cache
- ✅ Performance degradation acceptable
- ✅ No data loss
- ✅ Cache automatically reconnects

### Scenario 3: Circuit Breaker Activation

**Objective**: Verify circuit breaker prevents cascading failures

**Steps**:
1. Simulate external service failures
2. Make repeated requests (5+ failures)
3. Verify circuit opens
4. Verify fast-fail responses
5. Wait for timeout period
6. Verify circuit closes on success

**Expected Results**:
- ✅ Circuit opens after threshold
- ✅ Fast-fail prevents resource exhaustion
- ✅ Circuit closes on recovery
- ✅ Normal operation resumes

### Scenario 4: Message Queue Failure

**Objective**: Verify message handling during queue unavailability

**Steps**:
1. Stop SQS/message queue
2. Trigger events that publish messages
3. Verify messages queued locally
4. Restart message queue
5. Verify messages delivered

**Expected Results**:
- ✅ No messages lost
- ✅ Local queue prevents blocking
- ✅ Messages delivered on recovery
- ✅ Order preserved

### Scenario 5: Payment Gateway Failure

**Objective**: Verify payment failure handling

**Steps**:
1. Simulate Stripe API failure
2. Attempt ticket purchase
3. Verify inventory not decremented
4. Verify user notified
5. Verify retry capability

**Expected Results**:
- ✅ Inventory remains available
- ✅ No orphaned reservations
- ✅ Clear error message
- ✅ User can retry

### Scenario 6: Concurrent Failures

**Objective**: Verify system handles multiple simultaneous failures

**Steps**:
1. Stop database and Redis simultaneously
2. Attempt various operations
3. Verify graceful degradation
4. Restart services
5. Verify full recovery

**Expected Results**:
- ✅ System remains stable
- ✅ No data corruption
- ✅ Appropriate error responses
- ✅ Complete recovery

## Monitoring During Tests

### Application Logs
```bash
# View service logs
docker-compose logs -f auth-service
docker-compose logs -f payment-service

# Search for errors
docker-compose logs | grep ERROR
```

### Database Status
```bash
# Check PostgreSQL
docker exec -it auth-db pg_isready

# View connections
docker exec -it auth-db psql -U auth_user -d auth_service \
  -c "SELECT count(*) FROM pg_stat_activity;"
```

### Redis Status
```bash
# Check Redis
docker exec -it redis-cache redis-cli PING

# View stats
docker exec -it redis-cache redis-cli INFO
```

### Service Health
```bash
# Check all services
for port in 8091 8092 8093 8094 8095; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

## Test Results Analysis

### Success Criteria

| Test | Metric | Target | Critical |
|------|--------|--------|----------|
| Database Failure | Recovery Time | < 30s | < 60s |
| Cache Failure | Functionality | 100% | 95% |
| Service Restart | Data Loss | 0% | 0% |
| Circuit Breaker | Activation | 5 failures | 10 failures |
| Message Queue | Message Loss | 0% | 0% |
| Payment Failure | Inventory Accuracy | 100% | 100% |

### Viewing Test Reports

```bash
# HTML report
open target/surefire-reports/index.html

# Text report
cat target/surefire-reports/*.txt

# XML report (for CI/CD)
cat target/surefire-reports/TEST-*.xml
```

## Common Failure Patterns

### Pattern 1: Connection Pool Exhaustion
**Symptoms**: Timeout errors, slow responses
**Cause**: Too many concurrent connections
**Solution**: Increase pool size, add connection timeout

### Pattern 2: Memory Leak
**Symptoms**: Increasing memory usage, eventual crash
**Cause**: Objects not garbage collected
**Solution**: Profile application, fix leaks

### Pattern 3: Deadlock
**Symptoms**: Requests hang indefinitely
**Cause**: Circular resource dependencies
**Solution**: Implement timeout, fix locking order

### Pattern 4: Cascading Failure
**Symptoms**: Multiple services fail together
**Cause**: No circuit breaker, retry storms
**Solution**: Implement circuit breaker, exponential backoff

## Recovery Procedures

### Manual Recovery Steps

1. **Identify Failed Component**
   ```bash
   docker-compose ps
   docker-compose logs [service-name]
   ```

2. **Restart Failed Service**
   ```bash
   docker-compose restart [service-name]
   ```

3. **Verify Health**
   ```bash
   curl http://localhost:809X/actuator/health
   ```

4. **Check Data Consistency**
   ```bash
   # Run consistency checks
   # Verify no orphaned records
   ```

### Automated Recovery

The system implements automatic recovery for:
- ✅ Database connection failures (connection pool retry)
- ✅ Redis cache failures (fallback to database)
- ✅ External service failures (circuit breaker)
- ✅ Message delivery failures (retry with backoff)

## Best Practices

1. **Test Regularly**: Run disaster recovery tests weekly
2. **Document Failures**: Record all failure scenarios
3. **Update Runbooks**: Keep recovery procedures current
4. **Monitor Metrics**: Track MTTR (Mean Time To Recovery)
5. **Practice Drills**: Conduct failure drills with team
6. **Automate Recovery**: Implement self-healing where possible

## Integration with Monitoring

### CloudWatch Alarms
```yaml
# Example alarm configuration
DatabaseConnectionFailure:
  Metric: DatabaseConnectionErrors
  Threshold: 5
  Period: 60
  Action: SNS notification + Auto-restart
```

### Health Check Endpoints
All services expose:
- `/actuator/health` - Overall health
- `/actuator/health/db` - Database health
- `/actuator/health/redis` - Cache health

### Alerting Rules
- Database connection failures > 5 in 1 minute
- Service response time > 5 seconds
- Error rate > 5%
- Circuit breaker open > 2 minutes

## Disaster Recovery Metrics

Track these metrics over time:

| Metric | Definition | Target |
|--------|------------|--------|
| MTBF | Mean Time Between Failures | > 720 hours |
| MTTR | Mean Time To Recovery | < 5 minutes |
| RTO | Recovery Time Objective | < 15 minutes |
| RPO | Recovery Point Objective | < 1 minute |
| Availability | Uptime percentage | > 99.9% |

## Troubleshooting

### Tests Failing
1. Ensure Docker is running
2. Check service health
3. Review test logs
4. Verify network connectivity

### Services Not Recovering
1. Check container logs
2. Verify configuration
3. Restart services manually
4. Check resource limits

### Data Inconsistency
1. Review transaction logs
2. Check saga compensation
3. Verify idempotency keys
4. Run consistency checks

## Future Enhancements

- [ ] Chaos engineering with Chaos Monkey
- [ ] Automated failure injection
- [ ] Multi-region failover tests
- [ ] Backup and restore validation
- [ ] Performance degradation tests
- [ ] Security failure scenarios
