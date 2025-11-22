# Performance Optimization Quick Reference

## Quick Start

### 1. Apply Database Optimizations
```bash
# Run the optimization script on your PostgreSQL instance
psql -U postgres -f scripts/optimize-databases.sql
```

### 2. Verify Redis is Running
```bash
# Check Redis connection
redis-cli ping
# Should return: PONG
```

### 3. Start Services with Caching Enabled
```bash
# All services now have caching enabled by default
docker-compose up -d
```

## Cache Regions and TTLs

### Event Service
| Cache | TTL | Use Case |
|-------|-----|----------|
| events | 1h | Event details |
| searchResults | 15m | Search queries |
| categories | 24h | Category list |
| venues | 6h | Venue info |
| availability | 5m | Ticket counts |

### Auth Service
| Cache | TTL | Use Case |
|-------|-----|----------|
| userSessions | 24h | Active sessions |
| userProfiles | 1h | User data |
| tokenValidation | 15m | JWT checks |

### Ticket Service
| Cache | TTL | Use Case |
|-------|-----|----------|
| ticketTypes | 30m | Ticket types |
| inventory | 5m | Availability |
| tickets | 1h | Generated tickets |
| reservations | 15m | Active holds |

## Monitoring Cache Performance

### Check Cache Hit Rate
```bash
# Connect to Redis
redis-cli

# Get cache statistics
INFO stats

# Monitor cache operations in real-time
MONITOR
```

### View Cached Keys
```bash
# List all event cache keys
redis-cli KEYS "events::*"

# List all search cache keys
redis-cli KEYS "searchResults::*"

# Get cache entry
redis-cli GET "events::123e4567-e89b-12d3-a456-426614174000"
```

## Database Performance

### Check Slow Queries
```sql
-- Connect to database
psql -U postgres -d event_service

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

### Verify Indexes
```sql
-- List all indexes on events table
\d+ events

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'events'
ORDER BY idx_scan DESC;
```

## API Response Times

### Test Endpoint Performance
```bash
# Test event details endpoint
time curl -X GET http://localhost:8081/api/events/{event-id}

# Test search endpoint
time curl -X GET "http://localhost:8081/api/search?query=concert&city=New+York"

# Test with compression
curl -H "Accept-Encoding: gzip" \
     -X GET http://localhost:8081/api/events/{event-id} \
     --compressed -w "\nTime: %{time_total}s\n"
```

## Troubleshooting

### Cache Not Working
1. Check Redis is running: `redis-cli ping`
2. Verify Redis connection in logs
3. Check cache annotations are present
4. Verify `@EnableCaching` is on config class

### Slow Queries
1. Run `EXPLAIN ANALYZE` on slow queries
2. Check if indexes are being used
3. Verify statistics are up to date: `ANALYZE table_name`
4. Consider adding missing indexes

### High Memory Usage
1. Check Redis memory: `redis-cli INFO memory`
2. Review cache TTLs (may be too long)
3. Monitor JVM heap usage
4. Check for memory leaks with profiler

## Performance Targets

| Metric | Target | How to Measure |
|--------|--------|----------------|
| API Response Time (P95) | <3s | Load testing, APM tools |
| Cache Hit Rate | >80% | Redis INFO stats |
| Database Query Time | <100ms | pg_stat_statements |
| Concurrent Users | 1000+ | Load testing |
| Availability | 99.9% | Uptime monitoring |

## Load Testing Commands

### JMeter
```bash
# Run load test with 1000 users
jmeter -n -t load-test.jmx \
  -Jusers=1000 \
  -Jrampup=60 \
  -Jduration=600 \
  -l results.jtl
```

### Apache Bench
```bash
# Quick performance test
ab -n 10000 -c 100 http://localhost:8081/api/events
```

### wrk
```bash
# HTTP benchmarking
wrk -t12 -c400 -d30s http://localhost:8081/api/events
```

## Configuration Files

### Redis Configuration
- Auth: `auth-service/src/main/java/com/eventbooking/auth/config/RedisConfig.java`
- Event: `event-service/src/main/java/com/eventbooking/event/config/RedisConfig.java`
- Ticket: `ticket-service/src/main/java/com/eventbooking/ticket/config/RedisConfig.java`

### Database Optimization
- Script: `scripts/optimize-databases.sql`
- Indexes: Defined in init scripts and optimization script

### Compression
- All services: `application.yml` → `server.compression.enabled: true`

### CDN
- Documentation: `docs/cdn-configuration.md`
- Lambda@Edge: Security headers function

## Common Cache Operations

### Clear All Caches
```bash
# Clear all Redis caches
redis-cli FLUSHALL

# Clear specific cache region
redis-cli KEYS "events::*" | xargs redis-cli DEL
```

### Warm Up Cache
```bash
# Pre-populate frequently accessed data
curl http://localhost:8081/api/categories
curl http://localhost:8081/api/events?status=PUBLISHED
```

### Monitor Cache in Real-Time
```bash
# Watch cache operations
redis-cli MONITOR | grep -E "GET|SET|DEL"
```

## Best Practices

1. **Cache Invalidation**: Always invalidate related caches on updates
2. **TTL Selection**: Balance freshness vs. performance
3. **Cache Keys**: Use consistent, predictable key patterns
4. **Monitoring**: Track cache hit rates and adjust TTLs
5. **Testing**: Load test with realistic data volumes

## Support

For detailed information, see:
- `PERFORMANCE_OPTIMIZATION_SUMMARY.md` - Implementation details
- `docs/performance-optimization-guide.md` - Comprehensive guide
- `docs/cdn-configuration.md` - CDN setup
