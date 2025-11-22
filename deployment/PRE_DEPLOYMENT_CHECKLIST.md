# Pre-Deployment Checklist

Use this checklist to ensure you're ready for production deployment.

## Prerequisites

### AWS Account Setup
- [ ] AWS account created and configured
- [ ] IAM user with appropriate permissions created
- [ ] AWS CLI installed and configured (`aws configure`)
- [ ] AWS credentials tested (`aws sts get-caller-identity`)
- [ ] Billing alerts configured
- [ ] Cost budget set up

### Tools Installation
- [ ] Terraform >= 1.0 installed (`terraform --version`)
- [ ] Docker >= 20.10 installed (`docker --version`)
- [ ] jq installed (`jq --version`)
- [ ] Git installed (`git --version`)
- [ ] AWS CLI v2 installed (`aws --version`)

### Domain and SSL
- [ ] Domain name registered
- [ ] Route 53 hosted zone created (if using Route 53)
- [ ] SSL certificate requested in ACM or ready to import
- [ ] DNS validation completed for ACM certificate

## Infrastructure Preparation

### Network Planning
- [ ] VPC CIDR block decided (default: 10.0.0.0/16)
- [ ] Availability zones selected (default: 3 AZs)
- [ ] Subnet allocation planned
- [ ] NAT Gateway costs reviewed and approved

### Database Planning
- [ ] RDS instance class selected (default: db.t3.medium)
- [ ] Multi-AZ deployment confirmed
- [ ] Backup retention period decided
- [ ] Database migration scripts prepared
- [ ] Database credentials generated (strong passwords)

### Cache Planning
- [ ] Redis node type selected (default: cache.t3.medium)
- [ ] Redis cluster mode decision made
- [ ] Cache eviction policy decided

### Storage Planning
- [ ] S3 bucket names decided (must be globally unique)
- [ ] S3 lifecycle policies planned
- [ ] CloudFront distribution planned (optional)

## Security Configuration

### Secrets Management
- [ ] All database passwords generated (strong, unique)
- [ ] JWT secret key generated (256-bit minimum)
- [ ] Stripe API keys obtained (production keys)
- [ ] Stripe webhook secret obtained
- [ ] Email service credentials obtained (SES or SMTP)
- [ ] All secrets documented in secure location

### Access Control
- [ ] IAM roles reviewed and approved
- [ ] Security group rules reviewed
- [ ] Principle of least privilege applied
- [ ] MFA enabled on AWS root account
- [ ] MFA enabled on IAM admin users

### Compliance
- [ ] PCI DSS requirements reviewed (for payment processing)
- [ ] GDPR requirements reviewed (if applicable)
- [ ] Data retention policies defined
- [ ] Privacy policy updated
- [ ] Terms of service updated

## Application Configuration

### Service Configuration
- [ ] Production application.yml files reviewed
- [ ] Environment-specific configurations prepared
- [ ] Feature flags configured
- [ ] Rate limiting thresholds set
- [ ] Session timeout configured
- [ ] CORS origins configured

### Integration Configuration
- [ ] Stripe production account configured
- [ ] Stripe webhook endpoints configured
- [ ] Email templates created
- [ ] SES domain verified (if using SES)
- [ ] SES sending limits reviewed
- [ ] Third-party API keys obtained

## Monitoring and Alerting

### CloudWatch Setup
- [ ] Log retention periods decided
- [ ] CloudWatch alarms planned
- [ ] SNS topics for alerts created
- [ ] Alert recipients configured
- [ ] Dashboard layout planned

### Metrics and Tracing
- [ ] Key metrics identified
- [ ] X-Ray tracing enabled
- [ ] Custom metrics planned
- [ ] Performance baselines established

## Deployment Strategy

### Capacity Planning
- [ ] Initial service counts decided
- [ ] Auto-scaling thresholds set
- [ ] Maximum capacity limits set
- [ ] Load testing completed
- [ ] Performance benchmarks established

### Deployment Configuration
- [ ] Blue-green deployment strategy confirmed
- [ ] Rollback procedures documented
- [ ] Deployment windows scheduled
- [ ] Maintenance windows planned
- [ ] Stakeholders notified

## Testing

### Pre-Production Testing
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Load testing completed (1000+ concurrent users)
- [ ] Security testing completed
- [ ] Penetration testing completed (if required)
- [ ] Disaster recovery tested

### Staging Environment
- [ ] Staging environment deployed
- [ ] End-to-end testing completed in staging
- [ ] Performance testing completed in staging
- [ ] Database migration tested in staging
- [ ] Rollback tested in staging

## Documentation

### Technical Documentation
- [ ] Architecture diagrams updated
- [ ] API documentation updated
- [ ] Database schema documented
- [ ] Deployment procedures documented
- [ ] Troubleshooting guide created

### Operational Documentation
- [ ] Runbooks created
- [ ] Incident response procedures documented
- [ ] On-call rotation established
- [ ] Escalation procedures defined
- [ ] Contact list updated

## Team Preparation

### Training
- [ ] Team trained on deployment procedures
- [ ] Team trained on monitoring tools
- [ ] Team trained on incident response
- [ ] Team trained on rollback procedures

### Communication
- [ ] Deployment communication plan created
- [ ] Stakeholders identified and notified
- [ ] Status page prepared (if applicable)
- [ ] Customer communication prepared

## Cost Management

### Budget
- [ ] Monthly cost estimate calculated
- [ ] Budget approved by management
- [ ] Cost allocation tags configured
- [ ] Cost monitoring alerts set up
- [ ] Reserved instances considered (for long-term)

### Optimization
- [ ] Right-sizing analysis completed
- [ ] Spot instances considered for non-critical workloads
- [ ] S3 lifecycle policies configured
- [ ] CloudWatch log retention optimized

## Backup and Recovery

### Backup Strategy
- [ ] RDS automated backups enabled
- [ ] Backup retention period set
- [ ] Point-in-time recovery tested
- [ ] S3 versioning enabled
- [ ] Cross-region backup considered

### Disaster Recovery
- [ ] RTO (Recovery Time Objective) defined
- [ ] RPO (Recovery Point Objective) defined
- [ ] Disaster recovery plan documented
- [ ] Disaster recovery tested
- [ ] Multi-region strategy considered (if needed)

## Legal and Compliance

### Agreements
- [ ] Terms of Service finalized
- [ ] Privacy Policy finalized
- [ ] Cookie Policy finalized (if applicable)
- [ ] Data Processing Agreement prepared (if applicable)

### Compliance
- [ ] Security audit completed (if required)
- [ ] Compliance certifications obtained (if required)
- [ ] Data residency requirements met
- [ ] Industry-specific regulations reviewed

## Final Checks

### Pre-Deployment
- [ ] All checklist items completed
- [ ] Deployment plan reviewed with team
- [ ] Rollback plan reviewed with team
- [ ] Emergency contacts verified
- [ ] Deployment window confirmed
- [ ] Change management approval obtained

### Go/No-Go Decision
- [ ] Technical readiness confirmed
- [ ] Business readiness confirmed
- [ ] Risk assessment completed
- [ ] Stakeholder approval obtained
- [ ] Final go/no-go decision made

## Post-Deployment

### Immediate Actions
- [ ] Smoke tests executed
- [ ] Health checks verified
- [ ] Monitoring dashboards checked
- [ ] Error rates monitored
- [ ] Performance metrics reviewed

### Follow-up Actions
- [ ] Post-deployment review scheduled
- [ ] Lessons learned documented
- [ ] Documentation updated
- [ ] Team debriefing scheduled
- [ ] Success metrics tracked

## Emergency Contacts

| Role | Name | Phone | Email |
|------|------|-------|-------|
| DevOps Lead | | | |
| Backend Lead | | | |
| Database Admin | | | |
| Security Lead | | | |
| Product Manager | | | |
| On-Call Engineer | | | |

## Rollback Criteria

Rollback should be initiated if:
- [ ] Error rate exceeds 5%
- [ ] Response time exceeds 5 seconds
- [ ] Database connection failures
- [ ] Payment processing failures
- [ ] Critical security vulnerability discovered
- [ ] Data corruption detected

## Notes

Use this section for deployment-specific notes:

---

**Deployment Date**: _______________

**Deployed By**: _______________

**Deployment Version**: _______________

**Sign-off**: _______________
