# Production Deployment Guide

This guide covers the complete production deployment process for the Event Ticket Booking System.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Building Docker Images](#building-docker-images)
4. [Deploying Services](#deploying-services)
5. [Blue-Green Deployment](#blue-green-deployment)
6. [Monitoring and Rollback](#monitoring-and-rollback)
7. [CI/CD Pipeline](#cicd-pipeline)

## Prerequisites

### Required Tools

- AWS CLI v2 (configured with appropriate credentials)
- Terraform >= 1.0
- Docker >= 20.10
- jq (for JSON processing)
- kubectl (optional, for ECS Exec)

### AWS Permissions

Ensure your AWS credentials have permissions for:
- ECS (Fargate)
- ECR (Elastic Container Registry)
- RDS (PostgreSQL)
- ElastiCache (Redis)
- Application Load Balancer
- VPC and networking
- IAM roles and policies
- CloudWatch Logs
- CodeDeploy
- Secrets Manager

### Environment Variables

```bash
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export ENVIRONMENT=prod
```

## Infrastructure Setup

### 1. Initialize Terraform

```bash
cd deployment/terraform

# Initialize Terraform
terraform init

# Review the plan
terraform plan -var="environment=prod"

# Apply the infrastructure
terraform apply -var="environment=prod"
```

This will create:
- VPC with public and private subnets across 3 AZs
- Application Load Balancer with HTTPS
- ECS Fargate cluster
- RDS PostgreSQL instances (Multi-AZ)
- ElastiCache Redis cluster
- Security groups and IAM roles
- CloudWatch log groups
- CodeDeploy applications for blue-green deployment

### 2. Configure Secrets

Store sensitive configuration in AWS Secrets Manager:

```bash
# Database credentials
aws secretsmanager create-secret \
    --name auth-service/db-password \
    --secret-string "your-secure-password"

# JWT secret
aws secretsmanager create-secret \
    --name auth-service/jwt-secret \
    --secret-string "your-jwt-secret-key"

# Stripe API keys
aws secretsmanager create-secret \
    --name payment-service/stripe-secret-key \
    --secret-string "sk_live_..."

# Repeat for all services and secrets
```

### 3. Database Migration

Run database initialization scripts:

```bash
# Connect to RDS instances and run migration scripts
psql -h <rds-endpoint> -U admin -d auth_service -f scripts/init-auth-db.sql
psql -h <rds-endpoint> -U admin -d event_service -f scripts/init-event-db.sql
# ... repeat for all services
```

## Building Docker Images

### Build All Services

```bash
cd deployment/scripts

# Make scripts executable
chmod +x *.sh

# Build and push all services
./build-and-push.sh auth-service v1.0.0 us-east-1 $AWS_ACCOUNT_ID
./build-and-push.sh event-service v1.0.0 us-east-1 $AWS_ACCOUNT_ID
./build-and-push.sh ticket-service v1.0.0 us-east-1 $AWS_ACCOUNT_ID
./build-and-push.sh payment-service v1.0.0 us-east-1 $AWS_ACCOUNT_ID
./build-and-push.sh notification-service v1.0.0 us-east-1 $AWS_ACCOUNT_ID
```

### Verify Images in ECR

```bash
aws ecr describe-images \
    --repository-name event-booking-auth-service \
    --region us-east-1
```

## Deploying Services

### Deploy Individual Service

```bash
cd deployment/scripts

# Deploy a single service
./deploy-service.sh auth-service v1.0.0 prod
```

### Deploy All Services

```bash
# Deploy all services in order
./deploy-all-services.sh v1.0.0 prod
```

The script will deploy services in the correct order:
1. Auth Service (no dependencies)
2. Event Service (depends on Auth)
3. Ticket Service (depends on Auth and Event)
4. Payment Service (depends on Auth and Ticket)
5. Notification Service (independent)

### Verify Deployment

```bash
# Check service status
aws ecs describe-services \
    --cluster event-booking-cluster-prod \
    --services event-booking-auth-service \
    --region us-east-1

# Check running tasks
aws ecs list-tasks \
    --cluster event-booking-cluster-prod \
    --service-name event-booking-auth-service \
    --region us-east-1

# View logs
aws logs tail /ecs/event-booking-auth-service --follow
```

## Blue-Green Deployment

### How It Works

1. **Blue Environment**: Current production version
2. **Green Environment**: New version being deployed
3. **Traffic Shift**: ALB gradually shifts traffic from blue to green
4. **Validation**: Health checks ensure green is healthy
5. **Completion**: Blue environment is terminated after successful deployment

### Deployment Process

When you run `deploy-service.sh` with CodeDeploy enabled:

1. New task definition is registered
2. Green environment is created with new version
3. Health checks validate green environment
4. Traffic is shifted to green (configurable: all-at-once, canary, linear)
5. Blue environment is terminated after 5 minutes
6. Automatic rollback on failure

### Monitor Blue-Green Deployment

```bash
# Get deployment status
aws deploy get-deployment \
    --deployment-id <deployment-id> \
    --region us-east-1

# List recent deployments
aws deploy list-deployments \
    --application-name event-booking-prod \
    --deployment-group-name event-booking-auth-service-prod \
    --region us-east-1
```

### Traffic Shifting Strategies

Edit `deployment/terraform/codedeploy.tf` to change strategy:

```hcl
# All-at-once (default)
deployment_config_name = "CodeDeployDefault.ECSAllAtOnce"

# Canary (10% then 90%)
deployment_config_name = "CodeDeployDefault.ECSCanary10Percent5Minutes"

# Linear (10% every minute)
deployment_config_name = "CodeDeployDefault.ECSLinear10PercentEvery1Minutes"
```

## Monitoring and Rollback

### Health Checks

Services are monitored via:
- ALB target group health checks
- ECS task health checks
- CloudWatch alarms
- Application health endpoints (`/actuator/health`)

### View Metrics

```bash
# CPU utilization
aws cloudwatch get-metric-statistics \
    --namespace AWS/ECS \
    --metric-name CPUUtilization \
    --dimensions Name=ServiceName,Value=event-booking-auth-service \
    --start-time 2024-01-01T00:00:00Z \
    --end-time 2024-01-01T23:59:59Z \
    --period 300 \
    --statistics Average
```

### Rollback Service

If issues are detected:

```bash
cd deployment/scripts

# Rollback to previous version
./rollback-service.sh auth-service prod
```

This will:
- Stop current deployment (if using CodeDeploy)
- Revert to previous task definition
- Wait for service to stabilize
- Verify rollback success

### Manual Rollback

```bash
# List task definitions
aws ecs list-task-definitions \
    --family-prefix event-booking-auth-service \
    --sort DESC

# Update service to specific version
aws ecs update-service \
    --cluster event-booking-cluster-prod \
    --service event-booking-auth-service \
    --task-definition event-booking-auth-service:5 \
    --force-new-deployment
```

## CI/CD Pipeline

### GitHub Actions Workflow

The repository includes a GitHub Actions workflow (`.github/workflows/deploy.yml`) that:

1. Triggers on push to `main` branch
2. Builds all services
3. Runs tests
4. Builds Docker images
5. Pushes to ECR
6. Deploys to ECS using blue-green strategy
7. Runs smoke tests
8. Notifies team of deployment status

### Setup GitHub Secrets

Configure these secrets in your GitHub repository:

```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_ACCOUNT_ID
```

### Manual Deployment Trigger

```bash
# Trigger deployment via GitHub CLI
gh workflow run deploy.yml \
    --ref main \
    -f environment=prod \
    -f version=v1.0.0
```

## Auto-Scaling Configuration

Services automatically scale based on:

### CPU-Based Scaling
- Target: 70% CPU utilization
- Scale out: Add tasks when above target
- Scale in: Remove tasks when below target
- Cooldown: 60s scale-out, 300s scale-in

### Memory-Based Scaling
- Target: 80% memory utilization
- Similar behavior to CPU scaling

### Capacity Limits

| Service | Min | Max |
|---------|-----|-----|
| Auth | 2 | 10 |
| Event | 2 | 10 |
| Ticket | 3 | 15 |
| Payment | 3 | 15 |
| Notification | 2 | 10 |

Adjust in `deployment/terraform/variables.tf`

## Troubleshooting

### Service Won't Start

```bash
# Check task logs
aws logs tail /ecs/event-booking-auth-service --follow

# Describe task to see errors
aws ecs describe-tasks \
    --cluster event-booking-cluster-prod \
    --tasks <task-id>
```

### Health Check Failures

```bash
# Check target group health
aws elbv2 describe-target-health \
    --target-group-arn <target-group-arn>

# Test health endpoint
curl https://api.event-booking.com/api/auth/actuator/health
```

### Database Connection Issues

```bash
# Verify security group rules
aws ec2 describe-security-groups \
    --group-ids <security-group-id>

# Test connectivity from ECS task
aws ecs execute-command \
    --cluster event-booking-cluster-prod \
    --task <task-id> \
    --container auth-service \
    --interactive \
    --command "/bin/bash"
```

## Best Practices

1. **Always test in staging first**: Deploy to staging environment before production
2. **Use semantic versioning**: Tag images with meaningful versions (v1.0.0, v1.0.1)
3. **Monitor deployments**: Watch CloudWatch metrics during and after deployment
4. **Keep rollback ready**: Ensure previous version is available for quick rollback
5. **Database migrations**: Run migrations separately before deploying new code
6. **Gradual rollout**: Use canary or linear deployment for critical changes
7. **Backup before deployment**: Take RDS snapshots before major updates
8. **Document changes**: Update CHANGELOG.md with each deployment

## Support

For issues or questions:
- Check CloudWatch Logs: `/ecs/event-booking-*`
- Review ECS service events
- Contact DevOps team
- Create incident ticket

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS CodeDeploy Blue-Green](https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-steps-ecs.html)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
