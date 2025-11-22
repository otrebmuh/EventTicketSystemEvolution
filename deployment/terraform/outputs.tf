# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "IDs of private application subnets"
  value       = aws_subnet.private_app[*].id
}

output "private_db_subnet_ids" {
  description = "IDs of private database subnets"
  value       = aws_subnet.private_db[*].id
}

# Load Balancer Outputs
output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = aws_lb.main.arn
}

# ECS Cluster Outputs
output "ecs_cluster_id" {
  description = "ID of the ECS cluster"
  value       = aws_ecs_cluster.main.id
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = aws_ecs_cluster.main.arn
}

# Service Discovery Outputs
output "service_discovery_namespace_id" {
  description = "ID of the service discovery namespace"
  value       = aws_service_discovery_private_dns_namespace.main.id
}

output "service_discovery_namespace_name" {
  description = "Name of the service discovery namespace"
  value       = aws_service_discovery_private_dns_namespace.main.name
}

# Target Group Outputs
output "auth_service_blue_target_group_arn" {
  description = "ARN of auth service blue target group"
  value       = aws_lb_target_group.auth_service_blue.arn
}

output "auth_service_green_target_group_arn" {
  description = "ARN of auth service green target group"
  value       = aws_lb_target_group.auth_service_green.arn
}

output "event_service_blue_target_group_arn" {
  description = "ARN of event service blue target group"
  value       = aws_lb_target_group.event_service_blue.arn
}

output "event_service_green_target_group_arn" {
  description = "ARN of event service green target group"
  value       = aws_lb_target_group.event_service_green.arn
}

output "ticket_service_blue_target_group_arn" {
  description = "ARN of ticket service blue target group"
  value       = aws_lb_target_group.ticket_service_blue.arn
}

output "ticket_service_green_target_group_arn" {
  description = "ARN of ticket service green target group"
  value       = aws_lb_target_group.ticket_service_green.arn
}

output "payment_service_blue_target_group_arn" {
  description = "ARN of payment service blue target group"
  value       = aws_lb_target_group.payment_service_blue.arn
}

output "payment_service_green_target_group_arn" {
  description = "ARN of payment service green target group"
  value       = aws_lb_target_group.payment_service_green.arn
}

output "notification_service_blue_target_group_arn" {
  description = "ARN of notification service blue target group"
  value       = aws_lb_target_group.notification_service_blue.arn
}

output "notification_service_green_target_group_arn" {
  description = "ARN of notification service green target group"
  value       = aws_lb_target_group.notification_service_green.arn
}

# Security Group Outputs
output "alb_security_group_id" {
  description = "ID of ALB security group"
  value       = aws_security_group.alb.id
}

output "ecs_services_security_group_id" {
  description = "ID of ECS services security group"
  value       = aws_security_group.ecs_services.id
}

output "rds_security_group_id" {
  description = "ID of RDS security group"
  value       = aws_security_group.rds.id
}

output "redis_security_group_id" {
  description = "ID of Redis security group"
  value       = aws_security_group.redis.id
}

# IAM Role Outputs
output "ecs_task_execution_role_arn" {
  description = "ARN of ECS task execution role"
  value       = aws_iam_role.ecs_task_execution_role.arn
}

output "ecs_task_role_arn" {
  description = "ARN of ECS task role"
  value       = aws_iam_role.ecs_task_role.arn
}

output "codedeploy_role_arn" {
  description = "ARN of CodeDeploy role"
  value       = aws_iam_role.codedeploy.arn
}

# CodeDeploy Outputs
output "codedeploy_app_name" {
  description = "Name of CodeDeploy application"
  value       = aws_codedeploy_app.main.name
}

# CloudWatch Log Groups
output "auth_service_log_group" {
  description = "CloudWatch log group for auth service"
  value       = aws_cloudwatch_log_group.auth_service.name
}

output "event_service_log_group" {
  description = "CloudWatch log group for event service"
  value       = aws_cloudwatch_log_group.event_service.name
}

output "ticket_service_log_group" {
  description = "CloudWatch log group for ticket service"
  value       = aws_cloudwatch_log_group.ticket_service.name
}

output "payment_service_log_group" {
  description = "CloudWatch log group for payment service"
  value       = aws_cloudwatch_log_group.payment_service.name
}

output "notification_service_log_group" {
  description = "CloudWatch log group for notification service"
  value       = aws_cloudwatch_log_group.notification_service.name
}

# Service URLs (via Service Discovery)
output "auth_service_url" {
  description = "Internal URL for auth service"
  value       = "http://auth-service.${aws_service_discovery_private_dns_namespace.main.name}:8080"
}

output "event_service_url" {
  description = "Internal URL for event service"
  value       = "http://event-service.${aws_service_discovery_private_dns_namespace.main.name}:8080"
}

output "ticket_service_url" {
  description = "Internal URL for ticket service"
  value       = "http://ticket-service.${aws_service_discovery_private_dns_namespace.main.name}:8080"
}

output "payment_service_url" {
  description = "Internal URL for payment service"
  value       = "http://payment-service.${aws_service_discovery_private_dns_namespace.main.name}:8080"
}

output "notification_service_url" {
  description = "Internal URL for notification service"
  value       = "http://notification-service.${aws_service_discovery_private_dns_namespace.main.name}:8080"
}

# Summary Output
output "deployment_summary" {
  description = "Summary of deployment configuration"
  value = {
    environment          = var.environment
    region              = var.aws_region
    cluster_name        = aws_ecs_cluster.main.name
    alb_dns             = aws_lb.main.dns_name
    blue_green_enabled  = var.enable_blue_green_deployment
    service_discovery   = aws_service_discovery_private_dns_namespace.main.name
  }
}
