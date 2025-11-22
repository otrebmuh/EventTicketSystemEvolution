# Performance Optimization Guide

## Overview

This document describes the performance optimizations implemented in the Event Ticket Booking System to meet the requirement of supporting 1000+ concurrent users with sub-3-second response times.

## Redis Caching Strategy

### Cache Configuration

All services use Redis for caching with Spring Cache abstraction. Cache configurations are defined in `RedisConfig.java` for each service.

#### Event Service Caches

| Cache Name | TTL | Purpose |
|------------|-----|---------|
| `events` | 1 hour | Individual event details |
| `searchResults` | 15 minutes | Search query results |
| `categories` | 24 hours | Event categories (rarely change) |
| `venues` | 6 hours | Venue information |
| `availability` | 5 minutes | Ticket availability (frequently updated) |

#### Auth Service Caches

| Cache Name | TTL | Purpose |
|------------|-----|---------|
| `userSessions` | 24 hours | Active user sessions |
| `userProfiles` | 1 hour | User profile data |
| `tokenValidation` | 15 minutes | JWT token validation results |

### Cache Annotations

The application uses Spring Cache annotations for declarative caching:

```java
// Cache the result
@Cacheable(value = "events", key = "#eventId", unless = "#result == null")
public EventDto getEventById(UUID eventId) { ... }

// Update cache entry
@CachePut(value = "events", key = "#eventId")
public EventDto updateEvent(UUID eventId, UpdateEventRequest request) { ... }

// Evict cache entry
@CacheEvict(value = {"events", "searchResults"}, key = "#eventId", allEntries = true)
public void deleteEvent(UUID eventId) { ... }
```

### Cache Invalidation Strategy

1. **Event Updates**: Invalidate specific event cache and all search results
2. **Event Deletion**: Invalidate event and search caches
3. **Category Changes**: Invalidate category cache and search results
4. **Ticket Updates**: Invalidate availability cache

### Redis Connection Pool

Configured in `application.yml`:

```yaml
spring:
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: ${SPRING_REDIS_PORT:6379}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## Database Query Optimization

### Indexing Strategy

Comprehensive indexes have been added to optimize common query patterns. See `scripts/optimize-databases.sql` for full details.

#### Key Indexes

**Auth Service:**
- Composite index on `(email, email_verified)` for login queries
- Partial index on active sessions for session management
- Index on token expiration for cleanup queries

**Event Service:**
- Partial index on published events for public listings
- Composite index on `(status, event_date)` for filtered queries
- Full-text search indexes using `pg_trgm` for fuzzy matching
- Covering indexes to avoid table lookups

**Ticket Service:**
- Composite index on `(event_id, quantity_available)` for inventory checks
- Partial index on expired reservations for cleanup
- Index on QR codes for fast ticket validation

**Payment Service:**
- Composite index on `(user_id, payment_status, created_at)` for order history
- Partial index on pending orders for timeout processing
- Index on gateway transaction IDs for reconciliation

### Query Optimization Techniques

1. **Covering Indexes**: Include frequently selected columns in indexes
2. **Partial Indexes**: Index only relevant rows (e.g., published events)
3. **Composite Indexes**: Optimize multi-column WHERE clauses
4. **Full-Text Search**: Use PostgreSQL's `pg_trgm` for fuzzy text search

### Connection Pooling

HikariCP is used for database connection pooling (Spring Boot default):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Query Performance Monitoring

Enable `pg_stat_statements` extension to monitor slow queries:

```sql
-- View slowest queries
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

## API Response Compression

### Server-Side Compression

All services have compression enabled in `application.yml`:

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 1024  # Only compress responses > 1KB
```

### Compression Algorithms

- **Gzip**: Default compression algorithm
- **Brotli**: Can be enabled for better compression ratios

### Benefits

- Reduces bandwidth usage by 60-80% for JSON responses
- Improves response times for clients with slower connections
- Minimal CPU overhead on modern servers

## CDN Configuration

### CloudFront Distribution

See `docs/cdn-configuration.md` for detailed CDN setup.

#### Key Features

1. **Static Asset Caching**: 7-day TTL for CSS, JS, images
2. **Event Image Caching**: 1-day TTL with S3 origin
3. **API Passthrough**: No caching for API requests
4. **Compression**: Automatic gzip/brotli compression
5. **Security Headers**: Added via Lambda@Edge

#### Cache Behaviors

| Path Pattern | Origin | TTL | Compression |
|--------------|--------|-----|-------------|
| `/static/*` | ALB | 7 days | Yes |
| `/images/*` | S3 | 1 day | Yes |
| `/api/*` | ALB | No cache | Yes |
| `/*` | ALB | 24 hours | Yes |

### Cache-Control Headers

Set appropriate headers in application code:

```java
// Static assets (immutable)
response.setHeader("Cache-Control", "public, max-age=604800, immutable");

// Event images (can change)
response.setHeader("Cache-Control", "public, max-age=86400");

// API responses (no cache)
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
```

## Load Balancing

### Application Load Balancer (ALB)

- **Health Checks**: Every 30 seconds on `/actuator/health`
- **Connection Draining**: 300 seconds
- **Idle Timeout**: 60 seconds
- **Sticky Sessions**: Enabled for stateful operations

### Auto Scaling

```yaml
AutoScaling:
  MinSize: 2
  MaxSize: 10
  TargetCPUUtilization: 70%
  TargetMemoryUtilization: 80%
  ScaleUpCooldown: 300s
  ScaleDownCooldown: 600s
```

## JVM Optimization

### Memory Settings

```bash
# For services with moderate load (Auth, Notification)
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# For services with high load (Event, Ticket, Payment)
JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Garbage Collection

- **G1GC**: Recommended for low-latency applications
- **Max Pause Time**: 200ms target
- **GC Logging**: Enabled for monitoring

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/var/log/gc.log
```

## Database Connection Optimization

### HikariCP Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10        # Max connections
      minimum-idle: 5              # Min idle connections
      connection-timeout: 30000    # 30 seconds
      idle-timeout: 600000         # 10 minutes
      max-lifetime: 1800000        # 30 minutes
      leak-detection-threshold: 60000  # 60 seconds
```

### Best Practices

1. **Pool Size**: Set to `(core_count * 2) + effective_spindle_count`
2. **Timeout**: Keep connection timeout reasonable (30s)
3. **Leak Detection**: Enable to catch connection leaks
4. **Validation**: Use `SELECT 1` for connection validation

## Async Processing

### Message Queues (SQS)

Use asynchronous processing for non-critical operations:

- Email notifications
- Ticket generation
- Event updates
- Analytics processing

### Benefits

- Reduces API response times
- Improves system resilience
- Enables retry mechanisms
- Scales independently

## Monitoring and Metrics

### Key Performance Indicators

Monitor these metrics via CloudWatch/Prometheus:

1. **Response Time**: P50, P95, P99 latencies
2. **Throughput**: Requests per second
3. **Error Rate**: 4xx and 5xx errors
4. **Cache Hit Rate**: Redis cache effectiveness
5. **Database Performance**: Query execution times
6. **JVM Metrics**: Heap usage, GC pauses

### Alerting Thresholds

```yaml
Alerts:
  - Name: HighResponseTime
    Condition: P95 > 3000ms
    Action: Scale up instances
  
  - Name: LowCacheHitRate
    Condition: CacheHitRate < 80%
    Action: Review cache configuration
  
  - Name: HighErrorRate
    Condition: 5xxErrorRate > 1%
    Action: Investigate errors
  
  - Name: DatabaseSlowQueries
    Condition: QueryTime > 1000ms
    Action: Optimize queries
```

## Performance Testing

### Load Testing

Use JMeter or Gatling to simulate load:

```bash
# Test with 1000 concurrent users
jmeter -n -t load-test.jmx \
  -Jusers=1000 \
  -Jrampup=60 \
  -Jduration=600
```

### Stress Testing

Gradually increase load to find breaking point:

```bash
# Ramp up from 100 to 2000 users
gatling.sh -s StressTest \
  --users-start 100 \
  --users-end 2000 \
  --ramp-duration 300
```

### Performance Benchmarks

Target metrics for 1000 concurrent users:

| Metric | Target | Current |
|--------|--------|---------|
| Response Time (P95) | < 3s | TBD |
| Throughput | > 500 req/s | TBD |
| Error Rate | < 0.1% | TBD |
| Cache Hit Rate | > 80% | TBD |
| Database Query Time | < 100ms | TBD |

## Optimization Checklist

### Application Level
- [x] Enable Redis caching with appropriate TTLs
- [x] Add Spring Cache annotations to frequently accessed methods
- [x] Implement cache invalidation strategy
- [x] Enable response compression
- [x] Configure connection pooling

### Database Level
- [x] Add indexes for common query patterns
- [x] Create partial indexes for filtered queries
- [x] Enable full-text search with pg_trgm
- [x] Configure connection pooling
- [x] Enable query performance monitoring

### Infrastructure Level
- [x] Configure CDN for static assets
- [x] Set up auto-scaling policies
- [x] Enable load balancing with health checks
- [x] Configure appropriate JVM settings
- [ ] Set up monitoring and alerting

### Testing
- [ ] Run load tests with 1000+ concurrent users
- [ ] Measure response times under load
- [ ] Verify cache hit rates
- [ ] Test auto-scaling behavior
- [ ] Validate CDN cache effectiveness

## Troubleshooting

### High Response Times

1. Check database query performance
2. Verify cache hit rates
3. Review JVM garbage collection logs
4. Check external service latencies
5. Verify network connectivity

### Low Cache Hit Rate

1. Review cache TTL settings
2. Check cache key generation
3. Verify cache invalidation logic
4. Monitor cache memory usage
5. Review query patterns

### Database Performance Issues

1. Identify slow queries with pg_stat_statements
2. Add missing indexes
3. Optimize query structure
4. Increase connection pool size
5. Consider read replicas for read-heavy workloads

## Future Optimizations

1. **Read Replicas**: Separate read and write database instances
2. **Elasticsearch**: For advanced search capabilities
3. **GraphQL**: Reduce over-fetching of data
4. **HTTP/2**: Enable server push for critical resources
5. **Service Mesh**: Implement Istio for advanced traffic management
