#!/bin/bash

# Rollback service to previous version
# Usage: ./rollback-service.sh <service-name> <environment>

set -e

SERVICE_NAME=$1
ENVIRONMENT=${2:-prod}
AWS_REGION=${AWS_REGION:-us-east-1}

if [ -z "$SERVICE_NAME" ]; then
    echo "Usage: ./rollback-service.sh <service-name> <environment>"
    echo "Example: ./rollback-service.sh auth-service prod"
    exit 1
fi

PROJECT_NAME="event-booking"
CLUSTER_NAME="${PROJECT_NAME}-cluster-${ENVIRONMENT}"
SERVICE_FULL_NAME="${PROJECT_NAME}-${SERVICE_NAME}"

echo "Rolling back ${SERVICE_NAME} in ${ENVIRONMENT} environment..."

# Check deployment controller type
DEPLOYMENT_CONTROLLER=$(aws ecs describe-services \
    --cluster ${CLUSTER_NAME} \
    --services ${SERVICE_FULL_NAME} \
    --region ${AWS_REGION} \
    --query 'services[0].deploymentController.type' \
    --output text)

if [ "$DEPLOYMENT_CONTROLLER" == "CODE_DEPLOY" ]; then
    echo "Using CodeDeploy rollback..."
    
    # Get the latest deployment
    DEPLOYMENT_ID=$(aws deploy list-deployments \
        --application-name ${PROJECT_NAME}-${ENVIRONMENT} \
        --deployment-group-name ${SERVICE_FULL_NAME}-${ENVIRONMENT} \
        --max-items 1 \
        --region ${AWS_REGION} \
        --query 'deployments[0]' \
        --output text)
    
    if [ -z "$DEPLOYMENT_ID" ] || [ "$DEPLOYMENT_ID" == "None" ]; then
        echo "No deployment found to rollback"
        exit 1
    fi
    
    echo "Rolling back deployment: ${DEPLOYMENT_ID}"
    
    # Stop the deployment (triggers automatic rollback)
    aws deploy stop-deployment \
        --deployment-id ${DEPLOYMENT_ID} \
        --auto-rollback-enabled \
        --region ${AWS_REGION}
    
    echo "Rollback initiated. Monitoring..."
    
    # Wait for rollback to complete
    aws deploy wait deployment-successful \
        --deployment-id ${DEPLOYMENT_ID} \
        --region ${AWS_REGION} || true
    
    echo "Rollback completed!"
else
    echo "Using ECS rollback..."
    
    # Get previous task definition
    CURRENT_TASK_DEF=$(aws ecs describe-services \
        --cluster ${CLUSTER_NAME} \
        --services ${SERVICE_FULL_NAME} \
        --region ${AWS_REGION} \
        --query 'services[0].taskDefinition' \
        --output text)
    
    TASK_FAMILY=$(echo $CURRENT_TASK_DEF | cut -d'/' -f2 | cut -d':' -f1)
    CURRENT_REVISION=$(echo $CURRENT_TASK_DEF | cut -d':' -f2)
    PREVIOUS_REVISION=$((CURRENT_REVISION - 1))
    
    if [ $PREVIOUS_REVISION -lt 1 ]; then
        echo "No previous revision to rollback to"
        exit 1
    fi
    
    PREVIOUS_TASK_DEF="${TASK_FAMILY}:${PREVIOUS_REVISION}"
    
    echo "Current task definition: ${CURRENT_TASK_DEF}"
    echo "Rolling back to: ${PREVIOUS_TASK_DEF}"
    
    # Update service with previous task definition
    aws ecs update-service \
        --cluster ${CLUSTER_NAME} \
        --service ${SERVICE_FULL_NAME} \
        --task-definition ${PREVIOUS_TASK_DEF} \
        --region ${AWS_REGION} \
        --force-new-deployment
    
    echo "Rollback initiated. Waiting for service to stabilize..."
    
    # Wait for service to stabilize
    aws ecs wait services-stable \
        --cluster ${CLUSTER_NAME} \
        --services ${SERVICE_FULL_NAME} \
        --region ${AWS_REGION}
    
    echo "Rollback completed!"
fi

# Get service status
echo ""
echo "Service Status:"
aws ecs describe-services \
    --cluster ${CLUSTER_NAME} \
    --services ${SERVICE_FULL_NAME} \
    --region ${AWS_REGION} \
    --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,TaskDef:taskDefinition}' \
    --output table
