# Auth Service
resource "aws_ecs_service" "auth_service" {
  name            = "${var.project_name}-auth-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = "${var.project_name}-auth-service:${var.environment == "prod" ? "latest" : "1"}"
  desired_count   = var.auth_service_desired_count
  launch_type     = "FARGATE"
  
  deployment_controller {
    type = var.enable_blue_green_deployment ? "CODE_DEPLOY" : "ECS"
  }
  
  network_configuration {
    subnets          = aws_subnet.private_app[*].id
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.auth_service_blue.arn
    container_name   = "auth-service"
    container_port   = 8080
  }
  
  service_registries {
    registry_arn = aws_service_discovery_service.auth_service.arn
  }
  
  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy
  ]
  
  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }
}

# Auto Scaling for Auth Service
resource "aws_appautoscaling_target" "auth_service" {
  max_capacity       = 10
  min_capacity       = var.auth_service_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.auth_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "auth_service_cpu" {
  name               = "${var.project_name}-auth-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.auth_service.resource_id
  scalable_dimension = aws_appautoscaling_target.auth_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.auth_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

resource "aws_appautoscaling_policy" "auth_service_memory" {
  name               = "${var.project_name}-auth-memory-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.auth_service.resource_id
  scalable_dimension = aws_appautoscaling_target.auth_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.auth_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value       = 80.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# Event Service
resource "aws_ecs_service" "event_service" {
  name            = "${var.project_name}-event-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = "${var.project_name}-event-service:${var.environment == "prod" ? "latest" : "1"}"
  desired_count   = var.event_service_desired_count
  launch_type     = "FARGATE"
  
  deployment_controller {
    type = var.enable_blue_green_deployment ? "CODE_DEPLOY" : "ECS"
  }
  
  network_configuration {
    subnets          = aws_subnet.private_app[*].id
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.event_service_blue.arn
    container_name   = "event-service"
    container_port   = 8080
  }
  
  service_registries {
    registry_arn = aws_service_discovery_service.event_service.arn
  }
  
  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy,
    aws_ecs_service.auth_service
  ]
  
  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }
}

# Auto Scaling for Event Service
resource "aws_appautoscaling_target" "event_service" {
  max_capacity       = 10
  min_capacity       = var.event_service_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.event_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "event_service_cpu" {
  name               = "${var.project_name}-event-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.event_service.resource_id
  scalable_dimension = aws_appautoscaling_target.event_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.event_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# Ticket Service
resource "aws_ecs_service" "ticket_service" {
  name            = "${var.project_name}-ticket-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = "${var.project_name}-ticket-service:${var.environment == "prod" ? "latest" : "1"}"
  desired_count   = var.ticket_service_desired_count
  launch_type     = "FARGATE"
  
  deployment_controller {
    type = var.enable_blue_green_deployment ? "CODE_DEPLOY" : "ECS"
  }
  
  network_configuration {
    subnets          = aws_subnet.private_app[*].id
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.ticket_service_blue.arn
    container_name   = "ticket-service"
    container_port   = 8080
  }
  
  service_registries {
    registry_arn = aws_service_discovery_service.ticket_service.arn
  }
  
  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy,
    aws_ecs_service.auth_service,
    aws_ecs_service.event_service
  ]
  
  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }
}

# Auto Scaling for Ticket Service
resource "aws_appautoscaling_target" "ticket_service" {
  max_capacity       = 15
  min_capacity       = var.ticket_service_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.ticket_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ticket_service_cpu" {
  name               = "${var.project_name}-ticket-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ticket_service.resource_id
  scalable_dimension = aws_appautoscaling_target.ticket_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ticket_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# Payment Service
resource "aws_ecs_service" "payment_service" {
  name            = "${var.project_name}-payment-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = "${var.project_name}-payment-service:${var.environment == "prod" ? "latest" : "1"}"
  desired_count   = var.payment_service_desired_count
  launch_type     = "FARGATE"
  
  deployment_controller {
    type = var.enable_blue_green_deployment ? "CODE_DEPLOY" : "ECS"
  }
  
  network_configuration {
    subnets          = aws_subnet.private_app[*].id
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.payment_service_blue.arn
    container_name   = "payment-service"
    container_port   = 8080
  }
  
  service_registries {
    registry_arn = aws_service_discovery_service.payment_service.arn
  }
  
  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy,
    aws_ecs_service.auth_service,
    aws_ecs_service.ticket_service
  ]
  
  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }
}

# Auto Scaling for Payment Service
resource "aws_appautoscaling_target" "payment_service" {
  max_capacity       = 15
  min_capacity       = var.payment_service_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.payment_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "payment_service_cpu" {
  name               = "${var.project_name}-payment-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.payment_service.resource_id
  scalable_dimension = aws_appautoscaling_target.payment_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.payment_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# Notification Service
resource "aws_ecs_service" "notification_service" {
  name            = "${var.project_name}-notification-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = "${var.project_name}-notification-service:${var.environment == "prod" ? "latest" : "1"}"
  desired_count   = var.notification_service_desired_count
  launch_type     = "FARGATE"
  
  deployment_controller {
    type = var.enable_blue_green_deployment ? "CODE_DEPLOY" : "ECS"
  }
  
  network_configuration {
    subnets          = aws_subnet.private_app[*].id
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.notification_service_blue.arn
    container_name   = "notification-service"
    container_port   = 8080
  }
  
  service_registries {
    registry_arn = aws_service_discovery_service.notification_service.arn
  }
  
  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy
  ]
  
  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }
}

# Auto Scaling for Notification Service
resource "aws_appautoscaling_target" "notification_service" {
  max_capacity       = 10
  min_capacity       = var.notification_service_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.notification_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "notification_service_cpu" {
  name               = "${var.project_name}-notification-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.notification_service.resource_id
  scalable_dimension = aws_appautoscaling_target.notification_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.notification_service.service_namespace
  
  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
