# Quick Start Guide - Production Deployment

This guide provides quick commands to deploy the Event Ticket Booking System to production.

## Prerequisites Check

```bash
# Verify AWS CLI
aws --version

# Verify Terraform
terraform --version

# Verify Docker
docker --version

# Configure AWS credentials
aws configure

# Set environment variables
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

## 1. Deploy Infrastructure (First Time Only)

```bash
cd deployment/terraform

# Initialize Terraform
terraform init

# Plan infrastructure
terraform plan -var="environment=prod" -out=tfplan

# Apply infrastructure
terraform apply tfplan

# Save outputs
terraform output > ../outputs.txt
```

## 2. Configure Secrets

```bash
# Create secrets in AWS Secrets Manager
# Replace with your actual values

# Auth Service
aws secretsmanager create-secret --name auth-service/db-url --secret-string "jdbc:postgresql://..."
aws secretsmanager create-secret --name auth-service/db-username --secret-string "admin"
aws secretsmanager create-secret --name auth-service/db-password --secret-string "your-password"
aws secretsmanager create-secret --name auth-service/jwt-secret --secret-string "your-jwt-secret"

# Event Service
aws secretsmanager create-secret --name event-service/db-url --secret-string "jdbc:postgresql://..."
aws secretsmanager create-secret --name event-service/db-username --secret-string "admin"
aws secretsmanager create-secret --name event-service/db-password --secret-string "your-password"
aws secretsmanager create-secret --name event-service/s3-bucket --secret-string "event-booking-images"

# Ticket Service
aws secretsmanager create-secret --name ticket-service/db-url --secret-string "jdbc:postgresql://..."
aws secretsmanager create-secret --name ticket-service/db-username --secret-string "admin"
aws secretsmanager create-secret --name ticket-service/db-password --secret-string "your-password"

# Payment Service
aws secretsmanager create-secret --name payment-service/db-url --secret-string "jdbc:postgresql://..."
aws secretsmanager create-secret --name payment-service/db-username --secret-string "admin"
aws secretsmanager create-secret --name payment-service/db-password --secret-string "your-password"
aws secretsmanager create-secret --name payment-service/stripe-secret-key --secret-string "sk_live_..."
aws secretsmanager create-secret --name payment-service/stripe-webhook-secret --secret-string "whsec_..."

# Notification Service
aws secretsmanager create-secret --name notification-service/db-url --secret-string "jdbc:postgresql://..."
aws secretsmanager create-secret --name notification-service/db-username --secret-string "admin"
aws secretsmanager create-secret --name notification-service/db-password --secret-string "your-password"
aws secretsmanager create-secret --name notification-service/ses-from-email --secret-string "noreply@event-booking.com"

# Redis
aws secretsmanager create-secret --name redis/host --secret-string "redis-cluster-endpoint"
```

## 3. Build and Push Docker Images

```bash
cd deployment/scripts
chmod +x *.sh

# Build all services
VERSION="v1.0.0"

./build-and-push.sh auth-service $VERSION $AWS_REGION $AWS_ACCOUNT_ID
./build-and-push.sh event-service $VERSION $AWS_REGION $AWS_ACCOUNT_ID
./build-and-push.sh ticket-service $VERSION $AWS_REGION $AWS_ACCOUNT_ID
./build-and-push.sh payment-service $VERSION $AWS_REGION $AWS_ACCOUNT_ID
./build-and-push.sh notification-service $VERSION $AWS_REGION $AWS_ACCOUNT_ID
```

## 4. Deploy Services

```bash
# Deploy all services at once
./deploy-all-services.sh v1.0.0 prod

# OR deploy individually
./deploy-service.sh auth-service v1.0.0 prod
./deploy-service.sh event-service v1.0.0 prod
./deploy-service.sh ticket-service v1.0.0 prod
./deploy-service.sh payment-service v1.0.0 prod
./deploy-service.sh notification-service v1.0.0 prod
```

## 5. Verify Deployment

```bash
# Check all services
aws ecs list-services --cluster event-booking-cluster-prod

# Check specific service
aws ecs describe-services \
  --cluster event-booking-cluster-prod \
  --services event-booking-auth-service

# View logs
aws logs tail /ecs/event-booking-auth-service --follow

# Test health endpoints
ALB_DNS=$(terraform output -raw alb_dns_name)
curl https://${ALB_DNS}/api/auth/actuator/health
curl https://${ALB_DNS}/api/events/actuator/health
curl https://${ALB_DNS}/api/tickets/actuator/health
curl https://${ALB_DNS}/api/payments/actuator/health
curl https://${ALB_DNS}/api/notifications/actuator/health
```

## 6. Monitor Deployment

```bash
# Watch service events
aws ecs describe-services \
  --cluster event-booking-cluster-prod \
  --services event-booking-auth-service \
  --query 'services[0].events[0:5]'

# Check task status
aws ecs list-tasks \
  --cluster event-booking-cluster-prod \
  --service-name event-booking-auth-service

# View CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=event-booking-auth-service \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

## Common Operations

### Update a Service

```bash
# Build new version
./build-and-push.sh auth-service v1.0.1 $AWS_REGION $AWS_ACCOUNT_ID

# Deploy update
./deploy-service.sh auth-service v1.0.1 prod
```

### Rollback a Service

```bash
./rollback-service.sh auth-service prod
```

### Scale a Service

```bash
aws ecs update-service \
  --cluster event-booking-cluster-prod \
  --service event-booking-auth-service \
  --desired-count 5
```

### View Logs

```bash
# Tail logs
aws logs tail /ecs/event-booking-auth-service --follow

# Get recent logs
aws logs tail /ecs/event-booking-auth-service --since 1h

# Filter logs
aws logs tail /ecs/event-booking-auth-service --filter-pattern "ERROR"
```

### Execute Command in Container

```bash
# Get task ID
TASK_ID=$(aws ecs list-tasks \
  --cluster event-booking-cluster-prod \
  --service-name event-booking-auth-service \
  --query 'taskArns[0]' \
  --output text | cut -d'/' -f3)

# Execute command
aws ecs execute-command \
  --cluster event-booking-cluster-prod \
  --task $TASK_ID \
  --container auth-service \
  --interactive \
  --command "/bin/bash"
```

## Troubleshooting

### Service Won't Start

```bash
# Check task definition
aws ecs describe-task-definition \
  --task-definition event-booking-auth-service

# Check stopped tasks
aws ecs list-tasks \
  --cluster event-booking-cluster-prod \
  --desired-status STOPPED \
  --max-items 5

# Describe stopped task
aws ecs describe-tasks \
  --cluster event-booking-cluster-prod \
  --tasks <task-id>
```

### Health Check Failures

```bash
# Check target group health
aws elbv2 describe-target-health \
  --target-group-arn <target-group-arn>

# Check security groups
aws ec2 describe-security-groups \
  --group-ids <security-group-id>
```

### Database Connection Issues

```bash
# Test from ECS task
aws ecs execute-command \
  --cluster event-booking-cluster-prod \
  --task <task-id> \
  --container auth-service \
  --interactive \
  --command "nc -zv <db-endpoint> 5432"
```

## Cleanup (Destroy Infrastructure)

⚠️ **WARNING**: This will destroy all resources!

```bash
cd deployment/terraform

# Destroy infrastructure
terraform destroy -var="environment=prod"

# Delete ECR images
for repo in auth-service event-service ticket-service payment-service notification-service; do
  aws ecr delete-repository \
    --repository-name event-booking-$repo \
    --force
done

# Delete secrets
aws secretsmanager list-secrets \
  --query 'SecretList[?starts_with(Name, `auth-service`) || starts_with(Name, `event-service`) || starts_with(Name, `ticket-service`) || starts_with(Name, `payment-service`) || starts_with(Name, `notification-service`)].Name' \
  --output text | xargs -n1 aws secretsmanager delete-secret --secret-id
```

## CI/CD with GitHub Actions

### Setup

1. Add secrets to GitHub repository:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION`
   - `AWS_ACCOUNT_ID`

2. Push to main branch to trigger deployment:

```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

3. Monitor workflow:
   - Go to GitHub Actions tab
   - Watch deployment progress
   - Check logs for any issues

### Manual Trigger

```bash
# Using GitHub CLI
gh workflow run deploy-production.yml \
  --ref main \
  -f environment=prod \
  -f version=v1.0.0
```

## Support

- **Documentation**: See `DEPLOYMENT_GUIDE.md` for detailed information
- **Logs**: Check CloudWatch Logs at `/ecs/event-booking-*`
- **Metrics**: View CloudWatch dashboard for service metrics
- **Alerts**: Configure SNS topics for critical alerts

## Next Steps

1. Configure custom domain with Route 53
2. Set up CloudWatch alarms
3. Configure backup policies for RDS
4. Set up WAF rules for security
5. Configure CloudFront for frontend
6. Set up monitoring dashboards
7. Configure log aggregation
8. Set up cost monitoring
