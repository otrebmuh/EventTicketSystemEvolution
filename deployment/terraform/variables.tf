variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (prod, staging)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "event-booking"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones for deployment"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "auth_service_desired_count" {
  description = "Desired number of auth service tasks"
  type        = number
  default     = 2
}

variable "event_service_desired_count" {
  description = "Desired number of event service tasks"
  type        = number
  default     = 2
}

variable "ticket_service_desired_count" {
  description = "Desired number of ticket service tasks"
  type        = number
  default     = 3
}

variable "payment_service_desired_count" {
  description = "Desired number of payment service tasks"
  type        = number
  default     = 3
}

variable "notification_service_desired_count" {
  description = "Desired number of notification service tasks"
  type        = number
  default     = 2
}

variable "enable_blue_green_deployment" {
  description = "Enable blue-green deployment strategy"
  type        = bool
  default     = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.t3.medium"
}
