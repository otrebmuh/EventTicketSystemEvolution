# Saga Pattern Implementation for Distributed Transactions

## Overview

This document describes the implementation of the Saga pattern for handling distributed transactions in the Event Ticket Booking System. The Saga pattern ensures data consistency across multiple microservices without using distributed transactions (2PC).

## Architecture

### Core Components

#### 1. Saga Framework (shared-common)

**SagaStep Interface**
- Defines the contract for individual saga steps
- Each step implements `execute()` for forward transaction
- Each step implements `compensate()` for rollback/compensation

**SagaContext**
- Carries state throughout saga execution
- Stores intermediate results and parameters
- Tracks saga status and error messages

**SagaOrchestrator**
- Coordinates execution of saga steps in order
- Handles failure detection and compensation
- Ensures all steps are executed or compensated

**SagaEventStore**
- Records all saga events for event sourcing
- Enables saga execution monitoring and debugging
- Provides audit trail for transactions

### 2. Ticket Purchase Saga (payment-service)

The ticket purchase flow is implemented as a saga with the following steps:

```
1. ValidateInventory → 2. CreateOrder → 3. ProcessPayment → 4. ConfirmOrder
```

#### Saga Steps

**Step 1: ValidateInventory**
- **Purpose**: Verify ticket availability before creating order
- **Execute**: Validates event, ticket type, and quantity
- **Compensate**: No compensation needed (read-only operation)

**Step 2: CreateOrder**
- **Purpose**: Create order record in payment service
- **Execute**: Creates order with PENDING status
- **Compensate**: Cancels the order if subsequent steps fail

**Step 3: ProcessPayment**
- **Purpose**: Process payment through payment gateway (Stripe)
- **Execute**: Charges customer's payment method
- **Compensate**: Refunds the payment if order confirmation fails

**Step 4: ConfirmOrder**
- **Purpose**: Mark order as confirmed after successful payment
- **Execute**: Updates order status to CONFIRMED
- **Compensate**: Marks order as PAYMENT_FAILED

## Event Sourcing

All saga events are recorded in the SagaEventStore for:

### Event Types
- `SAGA_STARTED` - Saga execution begins
- `STEP_STARTED` - Individual step begins
- `STEP_COMPLETED` - Step completes successfully
- `STEP_FAILED` - Step fails
- `SAGA_COMPLETED` - All steps complete successfully
- `SAGA_FAILED` - Saga fails
- `COMPENSATION_STARTED` - Compensation begins
- `COMPENSATION_STEP_STARTED` - Compensation step begins
- `COMPENSATION_STEP_COMPLETED` - Compensation step completes
- `COMPENSATION_COMPLETED` - All compensations complete

### Benefits
1. **Audit Trail**: Complete history of transaction execution
2. **Debugging**: Identify which step failed and why
3. **Monitoring**: Track saga execution metrics
4. **Replay**: Reconstruct saga state from events

## Usage

### API Endpoint

```http
POST /api/payments/purchase-tickets
Content-Type: application/json
X-User-Id: {userId}

{
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "unitPrice": 50.00,
  "paymentMethodId": "pm_123",
  "reservationId": "uuid"
}
```

### Response

**Success (200 OK)**
```json
{
  "success": true,
  "message": "Ticket purchase successful",
  "data": {
    "sagaId": "uuid",
    "orderId": "uuid",
    "orderNumber": "ORD-123456",
    "transactionId": "uuid",
    "status": "SUCCESS",
    "message": "Ticket purchase completed successfully"
  }
}
```

**Failure (400 Bad Request)**
```json
{
  "success": false,
  "message": "Ticket purchase failed: Payment declined",
  "data": {
    "sagaId": "uuid",
    "status": "FAILED",
    "message": "Payment declined"
  }
}
```

## Monitoring

### Saga Execution Summary

```http
GET /api/saga/{sagaId}/summary
```

Returns:
```json
{
  "sagaId": "uuid",
  "sagaType": "TICKET_PURCHASE",
  "status": "COMPLETED",
  "startTime": "2024-01-15T10:30:00Z",
  "endTime": "2024-01-15T10:30:02Z",
  "durationMs": 2000,
  "completedSteps": [
    "ValidateInventory",
    "CreateOrder",
    "ProcessPayment",
    "ConfirmOrder"
  ],
  "failedStep": null,
  "errorMessage": null,
  "compensated": false,
  "totalEvents": 10
}
```

### Saga Events

```http
GET /api/saga/{sagaId}/events
```

Returns detailed event log for debugging.

## Compensation Logic

When a saga step fails, the orchestrator automatically triggers compensation in reverse order:

### Example: Payment Failure

1. **ValidateInventory** executes ✓
2. **CreateOrder** executes ✓ (Order created)
3. **ProcessPayment** fails ✗ (Payment declined)
4. **Compensation starts**:
   - Compensate CreateOrder → Cancel order
   - Compensate ValidateInventory → No action needed

### Compensation Guarantees

- **Best Effort**: Compensations are attempted but may fail
- **Idempotent**: Compensations can be safely retried
- **Logged**: All compensation attempts are recorded
- **Non-Blocking**: One compensation failure doesn't stop others

## Transaction Consistency

### ACID Properties

- **Atomicity**: All steps complete or all are compensated
- **Consistency**: Business rules enforced at each step
- **Isolation**: Each saga execution is independent
- **Durability**: Events are persisted for recovery

### Eventual Consistency

The saga pattern provides eventual consistency:
- Temporary inconsistencies may exist during execution
- Final state is consistent after completion or compensation
- Suitable for distributed systems where strong consistency is not required

## Performance Considerations

### Execution Time
- Average saga execution: 2-5 seconds
- Includes network calls to payment gateway
- Compensation adds 1-2 seconds if triggered

### Scalability
- Sagas execute independently
- No distributed locks required
- Horizontal scaling supported

### Monitoring Metrics
- Saga success rate
- Average execution time
- Compensation frequency
- Step failure rates

## Future Enhancements

### 1. Persistent Event Store
Replace in-memory store with PostgreSQL or DynamoDB for:
- Durability across service restarts
- Long-term audit trail
- Advanced querying capabilities

### 2. Saga Recovery
Implement saga recovery mechanism:
- Resume failed sagas from last checkpoint
- Automatic retry with exponential backoff
- Manual intervention for stuck sagas

### 3. Parallel Steps
Support parallel execution of independent steps:
- Reduce total execution time
- Improve throughput
- Complex compensation logic

### 4. Saga Timeout
Add timeout handling:
- Prevent indefinitely running sagas
- Automatic compensation after timeout
- Configurable timeout per step

### 5. Distributed Tracing
Integrate with AWS X-Ray or OpenTelemetry:
- End-to-end transaction visibility
- Performance bottleneck identification
- Cross-service correlation

## Testing

### Unit Tests
- Individual saga step testing
- Compensation logic verification
- Event store functionality

### Integration Tests
- Full saga execution flow
- Failure scenarios and compensation
- Event sourcing verification

### Test Coverage
- Success path: All steps complete
- Failure paths: Each step failure scenario
- Compensation: Verify rollback logic
- Edge cases: Missing parameters, timeouts

## References

- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- Requirements: 8.3, 8.5, 10.1
