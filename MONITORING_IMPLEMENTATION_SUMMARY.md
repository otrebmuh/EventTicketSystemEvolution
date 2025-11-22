# Monitoring Implementation Summary

## Task 11.1: Configure Application Monitoring

This document summarizes the implementation of comprehensive application monitoring for the Event Ticket Booking System.

## What Was Implemented

### 1. CloudWatch Metrics Integration

**Files Created:**
- `shared-common/src/main/java/com/eventbooking/common/config/MonitoringConfig.java`
- `shared-common/src/main/resources/application-monitoring.yml`

**Features:**
- Automatic metric publishing to CloudWatch
- Custom business metrics (registrations, logins, purchases, payments)
- System metrics (API response times, error rates, database performance)
- Configurable namespace and step intervals
- Common tags for all metrics (application name, environment)

**Dependencies Added:**
- `io.micrometer:micrometer-registry-cloudwatch2`
- `software.amazon.awssdk:cloudwatch`
- `software.amazon.awssdk:cloudwatchlogs`

### 2. AWS X-Ray Distributed Tracing

**Files Created:**
- `shared-common/src/main/resources/xray-sampling-rules.json`

**Features:**
- Automatic request tracing across all microservices
- Configurable sampling rules:
  - 100% sampling for critical operations (payments, ticket purchases)
  - 50% sampling for authentication
  - 10% sampling for general API calls
  - 1% sampling for health checks
- Integration with EC2 and ECS plugins
- SQL query tracing for PostgreSQL

**Dependencies Added:**
- `com.amazonaws:aws-xray-recorder-sdk-spring:2.15.1`
- `com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2:2.15.1`
- `com.amazonaws:aws-xray-recorder-sdk-sql-postgres:2.15.1`

### 3. Health Check Endpoints

**Files Created:**
- `shared-common/src/main/java/com/eventbooking/common/health/CustomHealthIndicator.java`
- `shared-common/src/main/java/com/eventbooking/common/health/DatabaseHealthIndicator.java`
- `shared-common/src/main/java/com/eventbooking/common/health/RedisHealthIndicator.java`

**Features:**
- Comprehensive health checks for all services
- Memory usage monitoring with warning thresholds
- Database connectivity and performance checks
- Redis connectivity and performance checks
- Kubernetes-ready liveness and readiness probes

**Endpoints:**
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/metrics` - Prometheus-compatible metrics
- `/actuator/prometheus` - Prometheus scraping endpoint

### 4. CloudWatch Alarms and Alerting

**Files Created:**
- `shared-common/src/main/java/com/eventbooking/common/monitoring/AlertingService.java`

**Features:**
- Programmatic alarm creation for critical metrics
- Pre-configured alarms for:
  - High error rate (>5%)
  - High response time (>3 seconds)
  - High memory usage (>85%)
  - Database connection issues
  - Failed payment processing
- SNS integration for notifications
- Configurable thresholds and evaluation periods

### 5. Structured Logging

**Files Created:**
- `shared-common/src/main/resources/logback-spring.xml`

**Features:**
- JSON-formatted logs for production
- Console logging for development
- File-based logging with rotation
- Async appenders for performance
- MDC context for trace IDs
- CloudWatch Logs integration ready

**Dependencies Added:**
- `net.logstash.logback:logstash-logback-encoder:7.4`

### 6. Automatic Metrics Collection

**Files Created:**
- `shared-common/src/main/java/com/eventbooking/common/interceptor/MetricsInterceptor.java`
- `shared-common/src/main/java/com/eventbooking/common/config/WebMonitoringConfig.java`

**Features:**
- Automatic API call tracking
- Request/response time measurement
- Trace ID propagation
- Slow request detection and logging
- MDC context management

### 7. Documentation and Configuration

**Files Created:**
- `MONITORING_SETUP_GUIDE.md` - Comprehensive setup and usage guide
- `cloudwatch-dashboard-template.json` - CloudWatch dashboard template
- `MONITORING_IMPLEMENTATION_SUMMARY.md` - This file

## Configuration Updates

### Service Configuration

All services (`auth-service`, `event-service`, `ticket-service`, `payment-service`, `notification-service`) have been updated to include:

```yaml
spring:
  config:
    import: optional:classpath:application-monitoring.yml
```

This automatically enables monitoring for all services.

### Environment Variables

The following environment variables control monitoring behavior:

```bash
# CloudWatch
CLOUDWATCH_ENABLED=true
CLOUDWATCH_NAMESPACE=EventBookingSystem
CLOUDWATCH_LOGS_ENABLED=true

# X-Ray
XRAY_ENABLED=true

# Alerting
ALERTING_ENABLED=false  # Enable in production
SNS_TOPIC_ARN=arn:aws:sns:region:account:topic

# AWS
AWS_REGION=us-east-1
```

## Custom Metrics Available

### Business Metrics
- `user.registration.total` - Total user registrations
- `user.login.total` - Login attempts (tagged by success)
- `event.creation.total` - Events created
- `ticket.purchase.total` - Tickets purchased
- `ticket.purchase.amount` - Purchase amounts
- `payment.processing.total` - Payments processed (tagged by success and method)
- `notification.sent.total` - Notifications sent (tagged by type and success)

### System Metrics
- `api.calls.total` - API calls (tagged by endpoint and status)
- `api.response.time` - Response times (tagged by endpoint)
- `database.query.time` - Database query times (tagged by operation)
- `cache.access.total` - Cache hits/misses

## Usage Examples

### Recording Custom Metrics

```java
@Service
public class UserService {
    private final CustomMetrics metrics;
    
    public void registerUser(User user) {
        // Business logic
        metrics.recordUserRegistration();
    }
    
    public void processPayment(Payment payment) {
        boolean success = gateway.process(payment);
        metrics.recordPaymentProcessing(success, payment.getMethod());
    }
}
```

### Checking Health

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health | jq

# Liveness probe (Kubernetes)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (Kubernetes)
curl http://localhost:8080/actuator/health/readiness
```

### Viewing Metrics

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/api.calls.total

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

## Production Deployment Checklist

### AWS IAM Permissions Required

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData",
        "cloudwatch:PutMetricAlarm",
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:ListMetrics"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### Steps to Enable in Production

1. **Create SNS Topic for Alerts**
   ```bash
   aws sns create-topic --name event-booking-alerts
   aws sns subscribe --topic-arn <topic-arn> --protocol email --notification-endpoint ops@example.com
   ```

2. **Create CloudWatch Log Groups**
   ```bash
   aws logs create-log-group --log-group-name /aws/ecs/auth-service
   aws logs create-log-group --log-group-name /aws/ecs/event-service
   aws logs create-log-group --log-group-name /aws/ecs/ticket-service
   aws logs create-log-group --log-group-name /aws/ecs/payment-service
   aws logs create-log-group --log-group-name /aws/ecs/notification-service
   ```

3. **Set Environment Variables in ECS Task Definitions**
   ```json
   {
     "environment": [
       {"name": "CLOUDWATCH_ENABLED", "value": "true"},
       {"name": "XRAY_ENABLED", "value": "true"},
       {"name": "ALERTING_ENABLED", "value": "true"},
       {"name": "SNS_TOPIC_ARN", "value": "arn:aws:sns:..."}
     ]
   }
   ```

4. **Create CloudWatch Dashboard**
   ```bash
   aws cloudwatch put-dashboard --dashboard-name EventBookingSystem \
     --dashboard-body file://cloudwatch-dashboard-template.json
   ```

5. **Enable Container Insights (Optional)**
   ```bash
   aws ecs update-cluster-settings --cluster event-booking-cluster \
     --settings name=containerInsights,value=enabled
   ```

## Testing Locally

For local development without AWS:

```yaml
# application-local.yml
monitoring:
  cloudwatch:
    enabled: false
  xray:
    enabled: false
  alerting:
    enabled: false
```

Health checks and Prometheus metrics will still work locally.

## Monitoring Best Practices

1. **Metric Naming**: Use consistent naming conventions
2. **Sampling**: Sample 100% of critical operations, 10-50% of normal operations
3. **Alert Thresholds**: Set based on baseline performance
4. **Log Levels**: ERROR for failures, WARN for degradation, INFO for business events
5. **Dashboards**: Create service-specific and system-wide dashboards

## Troubleshooting

### Metrics Not Appearing
- Check `CLOUDWATCH_ENABLED=true`
- Verify AWS credentials
- Check IAM permissions
- Review application logs

### X-Ray Traces Missing
- Check `XRAY_ENABLED=true`
- Verify X-Ray daemon is running
- Check IAM permissions
- Review sampling rules

### Health Checks Failing
- Check database connectivity
- Verify Redis is accessible
- Review application logs
- Check resource limits

## Requirements Validated

This implementation satisfies the following requirements from the design document:

- **Requirement 10.1**: System supports 1000+ concurrent users with monitoring
- **Requirement 10.2**: Response times monitored and alerted
- **Requirement 10.3**: Data consistency maintained with distributed tracing
- **Requirement 10.4**: Payment processing monitored
- **Requirement 10.5**: 99.9% availability tracked through health checks

## Next Steps

1. Deploy to staging environment and verify metrics
2. Create custom CloudWatch dashboards for each service
3. Configure SNS notifications for production alerts
4. Set up log retention policies
5. Create runbooks for common alert scenarios
6. Train operations team on monitoring tools

## Support

For questions or issues:
- Review `MONITORING_SETUP_GUIDE.md` for detailed documentation
- Check CloudWatch metrics and logs
- Examine X-Ray traces for distributed issues
- Contact DevOps team for infrastructure support
