# Asynchronous Messaging System

## Overview

The Event Ticket Booking System uses Amazon SQS (Simple Queue Service) and SNS (Simple Notification Service) for asynchronous communication between microservices. This enables loose coupling, improved scalability, and reliable message delivery.

## Architecture

### Message Flow

```
Payment Service → SQS Queue → Notification Service
                ↓ SNS Topic → Multiple Subscribers
                
Ticket Service → SQS Queue → Notification Service
               ↓ SNS Topic → Multiple Subscribers
               
Event Service → SNS Topic → Multiple Subscribers
```

### Components

#### 1. Message Publishers
- **PaymentEventPublisher**: Publishes payment and order events
- **TicketEventPublisher**: Publishes ticket generation and delivery events
- **EventManagementEventPublisher**: Publishes event lifecycle events

#### 2. Message Consumers
- **PaymentEventConsumer**: Consumes payment events for notifications
- **TicketEventConsumer**: Consumes ticket events for delivery
- **DeadLetterQueueMonitor**: Monitors and processes failed messages

#### 3. Message Types

**PaymentEvent**
- PAYMENT_COMPLETED: Payment successfully processed
- PAYMENT_FAILED: Payment processing failed
- ORDER_CONFIRMED: Order confirmed after payment
- ORDER_CANCELLED: Order cancelled by user
- REFUND_PROCESSED: Refund successfully processed

**TicketEvent**
- TICKETS_GENERATED: Digital tickets created
- TICKET_CANCELLED: Ticket cancelled
- TICKETS_DELIVERED: Tickets delivered to user
- TICKET_DELIVERY_FAILED: Ticket delivery failed

**EventManagementEvent**
- EVENT_CREATED: New event created
- EVENT_UPDATED: Event details updated
- EVENT_CANCELLED: Event cancelled
- EVENT_PUBLISHED: Event published and available

## Queue Configuration

### SQS Queues

#### Payment Events Queue
- **Name**: `payment-events-queue`
- **Purpose**: Point-to-point delivery of payment events to notification service
- **Visibility Timeout**: 30 seconds
- **Message Retention**: 4 days
- **Dead Letter Queue**: `payment-events-dlq`
- **Max Receive Count**: 3

#### Ticket Events Queue
- **Name**: `ticket-events-queue`
- **Purpose**: Point-to-point delivery of ticket events to notification service
- **Visibility Timeout**: 30 seconds
- **Message Retention**: 4 days
- **Dead Letter Queue**: `ticket-events-dlq`
- **Max Receive Count**: 3

### SNS Topics

#### Payment Events Topic
- **ARN**: `arn:aws:sns:us-east-1:000000000000:payment-events`
- **Purpose**: Broadcast payment events to multiple subscribers
- **Subscribers**: Notification Service, Analytics Service (future)

#### Ticket Events Topic
- **ARN**: `arn:aws:sns:us-east-1:000000000000:ticket-events`
- **Purpose**: Broadcast ticket events to multiple subscribers
- **Subscribers**: Notification Service, Analytics Service (future)

#### Event Management Topic
- **ARN**: `arn:aws:sns:us-east-1:000000000000:event-management-events`
- **Purpose**: Broadcast event lifecycle events
- **Subscribers**: Notification Service, Search Indexer (future)

## Dead Letter Queue Handling

### Purpose
Dead Letter Queues (DLQ) capture messages that fail processing after multiple retry attempts, preventing message loss and enabling manual inspection.

### DLQ Configuration
- **Max Receive Count**: 3 attempts before moving to DLQ
- **Retention Period**: 14 days
- **Monitoring**: Automated monitoring every 5 minutes

### DLQ Processing
1. **Automatic Monitoring**: `DeadLetterQueueMonitor` checks DLQs every 5 minutes
2. **Logging**: Failed messages are logged with full context
3. **Manual Inspection**: Operations team can review failed messages
4. **Requeue Option**: Messages can be requeued for retry after fixing issues

### Example DLQ Message
```json
{
  "messageId": "abc123",
  "receiveCount": "3",
  "sentTimestamp": "1234567890",
  "message": {
    "eventType": "PAYMENT_COMPLETED",
    "orderId": "uuid",
    "userId": "uuid",
    "amount": 100.00
  }
}
```

## Message Processing

### Consumer Pattern
Consumers use long polling (20 seconds) to efficiently retrieve messages:

```java
@Scheduled(fixedDelay = 10000, initialDelay = 5000)
public void consumePaymentEvents() {
    messageConsumer.pollMessages(
        paymentEventsQueue, 
        PaymentEvent.class, 
        this::handlePaymentEvent
    );
}
```

### Error Handling
1. **Transient Errors**: Message remains in queue for retry
2. **Permanent Errors**: Message moved to DLQ after max retries
3. **Logging**: All errors logged with full context
4. **Alerting**: Critical failures trigger alerts

## Configuration

### Environment Variables

#### AWS Credentials
```yaml
AWS_REGION: us-east-1
AWS_ACCESS_KEY_ID: your-access-key
AWS_SECRET_ACCESS_KEY: your-secret-key
```

#### SQS Configuration
```yaml
AWS_SQS_ENDPOINT: http://localhost:4566  # LocalStack for local dev
AWS_SQS_PAYMENT_EVENTS_QUEUE: payment-events-queue
AWS_SQS_TICKET_EVENTS_QUEUE: ticket-events-queue
AWS_SQS_PAYMENT_EVENTS_DLQ: payment-events-dlq
AWS_SQS_TICKET_EVENTS_DLQ: ticket-events-dlq
```

#### SNS Configuration
```yaml
AWS_SNS_ENDPOINT: http://localhost:4566  # LocalStack for local dev
AWS_SNS_PAYMENT_EVENTS_TOPIC: arn:aws:sns:us-east-1:000000000000:payment-events
AWS_SNS_TICKET_EVENTS_TOPIC: arn:aws:sns:us-east-1:000000000000:ticket-events
AWS_SNS_EVENT_MANAGEMENT_TOPIC: arn:aws:sns:us-east-1:000000000000:event-management-events
```

## Local Development with LocalStack

### Setup LocalStack
```bash
# Install LocalStack
pip install localstack

# Start LocalStack
localstack start

# Or use Docker
docker run -d -p 4566:4566 localstack/localstack
```

### Create Queues and Topics
```bash
# Create SQS queues
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name payment-events-queue
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name payment-events-dlq
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name ticket-events-queue
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name ticket-events-dlq

# Create SNS topics
aws --endpoint-url=http://localhost:4566 sns create-topic --name payment-events
aws --endpoint-url=http://localhost:4566 sns create-topic --name ticket-events
aws --endpoint-url=http://localhost:4566 sns create-topic --name event-management-events

# Configure DLQ redrive policy
aws --endpoint-url=http://localhost:4566 sqs set-queue-attributes \
  --queue-url http://localhost:4566/000000000000/payment-events-queue \
  --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:payment-events-dlq\",\"maxReceiveCount\":\"3\"}"}'
```

## Monitoring and Observability

### Metrics
- Message publish rate
- Message consumption rate
- DLQ message count
- Processing latency
- Error rate

### Logging
All message operations are logged with:
- Message ID
- Event type
- Timestamp
- Processing status
- Error details (if applicable)

### Alerts
- DLQ message threshold exceeded
- High error rate
- Processing latency threshold exceeded
- Queue depth threshold exceeded

## Best Practices

### Message Design
1. **Idempotency**: Design message handlers to be idempotent
2. **Small Messages**: Keep message size under 256KB
3. **Versioning**: Include schema version in messages
4. **Timestamps**: Always include event timestamps

### Error Handling
1. **Retry Logic**: Implement exponential backoff
2. **DLQ Monitoring**: Regularly check DLQs
3. **Logging**: Log all errors with context
4. **Alerting**: Set up alerts for critical failures

### Performance
1. **Batch Processing**: Process messages in batches when possible
2. **Long Polling**: Use long polling to reduce API calls
3. **Parallel Processing**: Process independent messages in parallel
4. **Connection Pooling**: Reuse SQS/SNS clients

## Future Enhancements

1. **Message Filtering**: Use SNS message filtering for targeted delivery
2. **FIFO Queues**: Implement FIFO queues for ordered processing
3. **Message Deduplication**: Add deduplication for exactly-once processing
4. **Cross-Region Replication**: Replicate messages across regions
5. **Event Sourcing**: Implement full event sourcing pattern
6. **Saga Pattern**: Implement distributed transactions with compensation

## Troubleshooting

### Messages Not Being Consumed
1. Check consumer is running and scheduled
2. Verify queue URL configuration
3. Check AWS credentials
4. Review CloudWatch logs

### Messages in DLQ
1. Check DLQ monitor logs
2. Review error messages
3. Fix underlying issue
4. Requeue messages if needed

### High Latency
1. Check queue depth
2. Scale consumers horizontally
3. Optimize message processing
4. Review network connectivity

## References

- [AWS SQS Documentation](https://docs.aws.amazon.com/sqs/)
- [AWS SNS Documentation](https://docs.aws.amazon.com/sns/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Spring Cloud AWS](https://spring.io/projects/spring-cloud-aws)
