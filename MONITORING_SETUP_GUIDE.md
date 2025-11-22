# Application Monitoring Setup Guide

This guide explains how to configure and use the monitoring infrastructure for the Event Ticket Booking System.

## Overview

The monitoring system includes:
- **CloudWatch Metrics**: Application and business metrics
- **AWS X-Ray**: Distributed tracing across microservices
- **Health Checks**: Comprehensive health indicators for all services
- **CloudWatch Alarms**: Automated alerting for critical issues
- **Structured Logging**: JSON-formatted logs for easy analysis

## Components

### 1. CloudWatch Metrics

Custom metrics are automatically published to CloudWatch:

**System Metrics:**
- API response times
- Error rates
- Memory usage
- Database query performance
- Cache hit rates

**Business Metrics:**
- User registrations
- Login attempts (success/failure)
- Event creations
- Ticket purchases
- Payment processing (success/failure)
- Notification delivery

### 2. AWS X-Ray Distributed Tracing

X-Ray provides end-to-end request tracing across all microservices:

**Sampling Rules:**
- Health checks: 1% sampling
- Payment processing: 100% sampling
- Ticket purchases: 100% sampling
- Authentication: 50% sampling
- Other endpoints: 10% sampling

### 3. Health Check Endpoints

Each service exposes comprehensive health checks at `/actuator/health`:

**Health Indicators:**
- Application health (memory, threads)
- Database connectivity and performance
- Redis connectivity and performance
- Custom business logic health

**Endpoints:**
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

### 4. CloudWatch Alarms

Automated alarms for critical metrics:

**System Alarms:**
- High error rate (>5%)
- High response time (>3 seconds)
- High memory usage (>85%)
- Database connection issues

**Business Alarms:**
- Failed payment rate
- Notification delivery failures

## Configuration

### Environment Variables

```bash
# CloudWatch Configuration
CLOUDWATCH_ENABLED=true
CLOUDWATCH_NAMESPACE=EventBookingSystem
CLOUDWATCH_LOGS_ENABLED=true

# AWS X-Ray Configuration
XRAY_ENABLED=true

# Alerting Configuration
ALERTING_ENABLED=true
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:event-booking-alerts

# AWS Region
AWS_REGION=us-east-1
```

### Service Configuration

Add to each service's `application.yml`:

```yaml
spring:
  config:
    import: classpath:application-monitoring.yml
  application:
    name: auth-service  # Change per service

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Docker Configuration

Update `docker-compose.yml` to include monitoring environment variables:

```yaml
services:
  auth-service:
    environment:
      - CLOUDWATCH_ENABLED=true
      - XRAY_ENABLED=true
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
```

## Usage

### Viewing Metrics

**CloudWatch Console:**
1. Navigate to CloudWatch > Metrics
2. Select namespace: `EventBookingSystem`
3. View custom metrics by dimension (application, endpoint, etc.)

**Prometheus Endpoint:**
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Viewing Traces

**AWS X-Ray Console:**
1. Navigate to AWS X-Ray > Traces
2. Filter by service name or trace ID
3. View service map for inter-service dependencies

### Checking Health

**Health Check:**
```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health with components
curl http://localhost:8080/actuator/health | jq

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Recording Custom Metrics

Use the `CustomMetrics` bean in your services:

```java
@Service
public class UserService {
    private final CustomMetrics metrics;
    
    public UserService(CustomMetrics metrics) {
        this.metrics = metrics;
    }
    
    public void registerUser(User user) {
        // Business logic
        metrics.recordUserRegistration();
    }
    
    public void login(String email, String password) {
        boolean success = authenticate(email, password);
        metrics.recordLogin(success);
    }
}
```

### Creating Alarms

Alarms can be created programmatically or via AWS Console:

**Programmatic Creation:**
```java
@Service
public class MonitoringSetup {
    private final AlertingService alertingService;
    
    @PostConstruct
    public void setupAlarms() {
        alertingService.createSystemAlarms();
    }
}
```

**AWS Console:**
1. Navigate to CloudWatch > Alarms
2. Create alarm
3. Select metric from `EventBookingSystem` namespace
4. Configure threshold and actions

## Monitoring Best Practices

### 1. Metric Naming

Use consistent naming conventions:
- `{resource}.{action}.{metric}` (e.g., `user.login.total`)
- Include relevant dimensions (success, method, endpoint)

### 2. Sampling Strategy

- Sample 100% of critical operations (payments, purchases)
- Sample 10-50% of normal operations
- Sample 1% of health checks

### 3. Alert Thresholds

- Set thresholds based on baseline performance
- Use multiple evaluation periods to avoid false alarms
- Configure SNS topics for different severity levels

### 4. Log Levels

- **ERROR**: System failures requiring immediate attention
- **WARN**: Degraded performance or potential issues
- **INFO**: Important business events
- **DEBUG**: Detailed diagnostic information (dev only)

### 5. Dashboard Creation

Create CloudWatch dashboards for:
- System health overview
- Business metrics (registrations, purchases, revenue)
- Service-specific metrics
- Error rates and response times

## Troubleshooting

### Metrics Not Appearing

1. Check CloudWatch is enabled: `CLOUDWATCH_ENABLED=true`
2. Verify AWS credentials are configured
3. Check IAM permissions for CloudWatch PutMetricData
4. Review application logs for errors

### X-Ray Traces Missing

1. Check X-Ray is enabled: `XRAY_ENABLED=true`
2. Verify X-Ray daemon is running (in ECS or locally)
3. Check IAM permissions for X-Ray PutTraceSegments
4. Review sampling rules configuration

### Health Checks Failing

1. Check database connectivity
2. Verify Redis is accessible
3. Review application logs for errors
4. Check resource limits (memory, connections)

### Alarms Not Triggering

1. Verify SNS topic ARN is correct
2. Check alarm configuration (threshold, period)
3. Confirm metrics are being published
4. Review CloudWatch alarm history

## Local Development

For local development without AWS:

```yaml
monitoring:
  cloudwatch:
    enabled: false
  xray:
    enabled: false
  alerting:
    enabled: false
```

Health checks and Prometheus metrics will still work locally.

## Production Deployment

### Prerequisites

1. **IAM Roles**: Create IAM roles with permissions:
   - `cloudwatch:PutMetricData`
   - `cloudwatch:PutMetricAlarm`
   - `xray:PutTraceSegments`
   - `xray:PutTelemetryRecords`
   - `logs:CreateLogGroup`
   - `logs:CreateLogStream`
   - `logs:PutLogEvents`

2. **SNS Topic**: Create SNS topic for alerts
3. **CloudWatch Log Groups**: Create log groups for each service

### Deployment Steps

1. Configure environment variables in ECS task definitions
2. Enable CloudWatch Container Insights
3. Create CloudWatch dashboards
4. Set up alarms with SNS notifications
5. Configure log retention policies

## Cost Optimization

- Use appropriate sampling rates for X-Ray
- Set log retention periods (7-30 days)
- Use metric filters to reduce log ingestion
- Archive old logs to S3
- Use CloudWatch Insights for log analysis instead of storing all logs

## Support

For issues or questions:
1. Check application logs
2. Review CloudWatch metrics and alarms
3. Examine X-Ray traces for distributed issues
4. Contact DevOps team for infrastructure issues
