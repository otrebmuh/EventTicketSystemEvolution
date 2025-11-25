# AWS Configuration for Cloud Deployment

When deploying to AWS (production environment), you need to configure the services to use real AWS services instead of LocalStack.

## Environment Variables to Update

For **production/cloud deployment**, remove or empty the following environment variables in your deployment configuration:

### In `docker-compose.yml` or your cloud deployment config:

```yaml
# For PRODUCTION - Remove or set to empty
AWS_ENDPOINT_OVERRIDE: ""  # Remove this or set to empty string

# These should be set to real AWS credentials
AWS_REGION: "us-east-1"  # Your actual AWS region
AWS_ACCESS_KEY_ID: "<your-real-access-key>"
AWS_SECRET_ACCESS_KEY: "<your-real-secret-key>"
```

## How It  Works

- **Local Development (current setup)**: 
  - `AWS_ENDPOINT_OVERRIDE` is set to `http://localstack:4566`
  - Services connect to LocalStack for SQS/SNS/CloudWatch
  
- **Cloud/Production Deployment**:
  - `AWS_ENDPOINT_OVERRIDE` should be empty or removed
  - Services will connect to real AWS services
  - Use IAM roles or proper AWS credentials

## Services Using AWS

The following services currently use AWS SDK:
- `payment-service` - Uses SQS for payment events

## Next Steps for Complete Local Setup

LocalStack is running and available, but SQS queues need to be created. Two options:

1. **Create queues in LocalStack** (recommended for full local testing)
2. **Make messaging non-fatal** (quick fix to unblock development)

Currently the payment flow fails when trying to publish to non-existent SQS queues.
