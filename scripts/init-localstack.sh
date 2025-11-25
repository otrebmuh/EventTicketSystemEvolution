#!/bin/bash

# Initialize LocalStack with required SQS queues using curl
# This script creates the necessary queues for payment events

echo "Waiting for LocalStack to be ready..."
until curl -s http://localhost:4566/_localstack/health | grep -q '"sqs": "available"'; do
  echo "Waiting for LocalStack SQS to be available..."
  sleep 2
done

echo "LocalStack is ready. Creating SQS queues..."
echo ""

# Create payment events queue
echo "Creating payment-events-queue..."
curl -s -X POST "http://localhost:4566/" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "Action=CreateQueue&QueueName=payment-events-queue&Version=2012-11-05" | grep -o "payment-events-queue" && echo "✓ Created"

# Create payment events DLQ (Dead Letter Queue)
echo "Creating payment-events-dlq..."
curl -s -X POST "http://localhost:4566/" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "Action=CreateQueue&QueueName=payment-events-dlq&Version=2012-11-05" | grep -o "payment-events-dlq" && echo "✓ Created"

echo ""
# List all queues to verify
echo "Verifying created queues:"
curl -s -X POST "http://localhost:4566/" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "Action=ListQueues&Version=2012-11-05" | grep -o "payment-events-[a-z]*"

echo ""
echo "LocalStack SQS initialization complete!"
