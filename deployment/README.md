# Event Ticket Booking System - Production Deployment

This directory contains all the necessary files and configurations for deploying the Event Ticket Booking System to production on AWS.

## 📁 Directory Structure

```
deployment/
├── aws/
│   └── ecs-task-definitions/     # ECS Fargate task definitions
├── terraform/                     # Infrastructure as Code
├── scripts/                       # Deployment automation scripts
├── DEPLOYMENT_GUIDE.md           # Comprehensive deployment guide
├── QUICK_START.md                # Quick reference commands
├── PRE_DEPLOYMENT_CHECKLIST.md   # Pre-deployment checklist
└── README.md                     # This file
```

## 🚀 Quick Start

### 1. Prerequisites

- AWS CLI v2
- Terraform >= 1.0
- Docker >= 20.10
- jq (for JSON processing)

### 2. Configure AWS

```bash
aws configure
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

### 3. Deploy Infrastructure

```bash
cd terraform
terraform init
terraform apply -var="environment=prod"
```

### 4. Build and Deploy Services

```bash
cd ../scripts
./build-and-push.sh auth-service v1.0.0 $AWS_REGION $AWS_ACCOUNT_ID
./deploy-all-services.sh v1.0.0 prod
```

## 📚 Documentation

### For First-Time Deployment
Start with **[PRE_DEPLOYMENT_CHECKLIST.md](PRE_DEPLOYMENT_CHECKLIST.md)** to ensure you have everything ready.

### For Detailed Instructions
See **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** for comprehensive step-by-step instructions.

### For Quick Reference
Use **[QUICK_START.md](QUICK_START.md)** for common commands and operations.

## 🏗️ Infrastructure Components

### Networking
- VPC with public and private subnets across 3 AZs
- NAT Gateways for outbound internet access
- Internet Gateway for public access
- VPC endpoints for AWS services

### Compute
- ECS Fargate cluster for containerized services
- Auto-scaling based on CPU and memory
- Service discovery for inter-service communication

### Load Balancing
- Application Load Balancer with HTTPS
- Path-based routing to services
- Blue-green target groups for zero-downtime deployments

### Database
- RDS PostgreSQL (Multi-AZ)
- ElastiCache Redis cluster
- Automated backups

### Storage
- S3 for event images and static assets
- CloudFront CDN (optional)

### Security
- Security groups with least privilege
- AWS Secrets Manager for sensitive data
- IAM roles for service access
- HTTPS enforcement

### Monitoring
- CloudWatch Logs
- CloudWatch Metrics
- Container Insights
- X-Ray tracing (ready)

## 🔄 Deployment Strategies

### Blue-Green Deployment (Default)
- Zero-downtime deployments
- Automatic rollback on failure
- Traffic shifting strategies available

### Rolling Deployment (Alternative)
- Gradual replacement of tasks
- Lower resource usage
- Longer deployment time

## 📊 Service Configuration

| Service | Min Tasks | Max Tasks | CPU | Memory |
|---------|-----------|-----------|-----|--------|
| Auth | 2 | 10 | 512 | 1024 |
| Event | 2 | 10 | 512 | 1024 |
| Ticket | 3 | 15 | 1024 | 2048 |
| Payment | 3 | 15 | 1024 | 2048 |
| Notification | 2 | 10 | 512 | 1024 |

## 🛠️ Common Operations

### Deploy a Service Update
```bash
cd scripts
./build-and-push.sh <service-name> <version> $AWS_REGION $AWS_ACCOUNT_ID
./deploy-service.sh <service-name> <version> prod
```

### Rollback a Service
```bash
./rollback-service.sh <service-name> prod
```

### Scale a Service
```bash
aws ecs update-service \
  --cluster event-booking-cluster-prod \
  --service event-booking-<service-name> \
  --desired-count <count>
```

### View Logs
```bash
aws logs tail /ecs/event-booking-<service-name> --follow
```

## 🔐 Security Best Practices

1. **Secrets Management**: All sensitive data in AWS Secrets Manager
2. **Network Isolation**: Services in private subnets
3. **Least Privilege**: Minimal IAM permissions
4. **Encryption**: Data encrypted at rest and in transit
5. **HTTPS Only**: HTTP redirects to HTTPS
6. **Regular Updates**: Keep dependencies updated

## 📈 Monitoring and Alerting

### Key Metrics to Monitor
- CPU and memory utilization
- Request count and latency
- Error rates
- Database connections
- Cache hit rates

### Recommended Alarms
- High error rate (>5%)
- High response time (>3s)
- Low healthy host count
- Database connection failures
- High CPU/memory (>80%)

## 💰 Cost Optimization

### Tips
1. Use Fargate Spot for non-critical workloads
2. Right-size resources based on actual usage
3. Enable auto-scaling to match demand
4. Use S3 lifecycle policies
5. Optimize CloudWatch log retention
6. Consider Reserved Instances for long-term

### Estimated Monthly Costs (Production)
- ECS Fargate: $200-400
- RDS PostgreSQL: $150-300
- ElastiCache Redis: $100-200
- Application Load Balancer: $20-40
- Data Transfer: $50-100
- CloudWatch: $20-50
- **Total**: ~$540-1,090/month

*Costs vary based on usage and region*

## 🔧 Troubleshooting

### Service Won't Start
1. Check CloudWatch logs
2. Verify task definition
3. Check security groups
4. Verify secrets exist

### Health Check Failures
1. Check target group health
2. Verify health endpoint
3. Check security group rules
4. Review application logs

### Database Connection Issues
1. Verify security group rules
2. Check database endpoint
3. Verify credentials in Secrets Manager
4. Test connectivity from ECS task

## 🤝 CI/CD Integration

### GitHub Actions
The repository includes a GitHub Actions workflow for automated deployment:
- `.github/workflows/deploy-production.yml`

### Setup
1. Add AWS credentials to GitHub Secrets
2. Push to main branch to trigger deployment
3. Monitor workflow in GitHub Actions tab

### Manual Trigger
```bash
gh workflow run deploy-production.yml \
  --ref main \
  -f environment=prod \
  -f version=v1.0.0
```

## 📞 Support

### Documentation
- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Docker Documentation](https://docs.docker.com/)

### Internal Resources
- CloudWatch Logs: `/ecs/event-booking-*`
- ECS Console: AWS Console → ECS → Clusters
- Load Balancer: AWS Console → EC2 → Load Balancers

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-01-15 | Initial production deployment |

## 📝 License

Copyright © 2024 Event Booking System. All rights reserved.

## 🙏 Acknowledgments

Built with:
- Spring Boot
- React
- PostgreSQL
- Redis
- AWS ECS Fargate
- Terraform

---

**Need Help?** Check the [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for detailed instructions or contact the DevOps team.
