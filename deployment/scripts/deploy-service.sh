#!/bin/bash

# Deploy service using blue-green deployment
# Usage: ./deploy-service.sh <service-name> <image-tag> <environment>

set -e

SERVICE_NAME=$1
IMAGE_TAG=${2:-latest}
ENVIRONMENT=${3:-prod}
AWS_REGION=${AWS_REGION:-us-east-1}
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

if [ -z "$SERVICE_NAME" ]; then
    echo "Usage: ./deploy-service.sh <service-name> <image-tag> <environment>"
    echo "Example: ./deploy-service.sh auth-service v1.0.0 prod"
    exit 1
fi

PROJECT_NAME="event-booking"
CLUSTER_NAME="${PROJECT_NAME}-cluster-${ENVIRONMENT}"
SERVICE_FULL_NAME="${PROJECT_NAME}-${SERVICE_NAME}"
TASK_FAMILY="${PROJECT_NAME}-${SERVICE_NAME}"

echo "Deploying ${SERVICE_NAME} to ${ENVIRONMENT} environment..."
echo "Cluster: ${CLUSTER_NAME}"
echo "Service: ${SERVICE_FULL_NAME}"
echo "Image Tag: ${IMAGE_TAG}"

# Get the task definition template
TASK_DEF_FILE="../aws/ecs-task-definitions/${SERVICE_NAME}-task.json"

if [ ! -f "$TASK_DEF_FILE" ]; then
    echo "Error: Task definition file not found: $TASK_DEF_FILE"
    exit 1
fi

# Replace placeholders in task definition
TASK_DEFINITION=$(cat $TASK_DEF_FILE | \
    sed "s/\${AWS_ACCOUNT_ID}/${AWS_ACCOUNT_ID}/g" | \
    sed "s/\${AWS_REGION}/${AWS_REGION}/g" | \
    sed "s/\${IMAGE_TAG}/${IMAGE_TAG}/g")

# Register new task definition
echo "Registering new task definition..."
NEW_TASK_DEF=$(echo $TASK_DEFINITION | \
    aws ecs register-task-definition \
    --cli-input-json file:///dev/stdin \
    --region ${AWS_REGION})

NEW_TASK_DEF_ARN=$(echo $NEW_TASK_DEF | jq -r '.taskDefinition.taskDefinitionArn')
echo "New task definition registered: ${NEW_TASK_DEF_ARN}"

# Check if blue-green deployment is enabled
DEPLOYMENT_CONTROLLER=$(aws ecs describe-services \
    --cluster ${CLUSTER_NAME} \
    --services ${SERVICE_FULL_NAME} \
    --region ${AWS_REGION} \
    --query 'services[0].deploymentController.type' \
    --output text)

if [ "$DEPLOYMENT_CONTROLLER" == "CODE_DEPLOY" ]; then
    echo "Using CodeDeploy for blue-green deployment..."
    
    # Create appspec.json for CodeDeploy
    APPSPEC=$(cat <<EOF
{
  "version": 1,
  "Resources": [
    {
      "TargetService": {
        "Type": "AWS::ECS::Service",
        "Properties": {
          "TaskDefinition": "${NEW_TASK_DEF_ARN}",
          "LoadBalancerInfo": {
            "ContainerName": "${SERVICE_NAME}",
            "ContainerPort": 8080
          }
        }
      }
    }
  ]
}
EOF
)
    
    # Create deployment
    DEPLOYMENT_ID=$(aws deploy create-deployment \
        --application-name ${PROJECT_NAME}-${ENVIRONMENT} \
        --deployment-group-name ${SERVICE_FULL_NAME}-${ENVIRONMENT} \
        --revision "{\"revisionType\":\"AppSpecContent\",\"appSpecContent\":{\"content\":\"${APPSPEC}\"}}" \
        --region ${AWS_REGION} \
        --query 'deploymentId' \
        --output text)
    
    echo "CodeDeploy deployment created: ${DEPLOYMENT_ID}"
    echo "Monitor deployment: aws deploy get-deployment --deployment-id ${DEPLOYMENT_ID} --region ${AWS_REGION}"
    
    # Wait for deployment to complete
    echo "Waiting for deployment to complete..."
    aws deploy wait deployment-successful \
        --deployment-id ${DEPLOYMENT_ID} \
        --region ${AWS_REGION}
    
    echo "Deployment completed successfully!"
else
    echo "Using ECS rolling deployment..."
    
    # Update service with new task definition
    aws ecs update-service \
        --cluster ${CLUSTER_NAME} \
        --service ${SERVICE_FULL_NAME} \
        --task-definition ${NEW_TASK_DEF_ARN} \
        --region ${AWS_REGION} \
        --force-new-deployment
    
    echo "Service update initiated. Waiting for deployment to stabilize..."
    
    # Wait for service to stabilize
    aws ecs wait services-stable \
        --cluster ${CLUSTER_NAME} \
        --services ${SERVICE_FULL_NAME} \
        --region ${AWS_REGION}
    
    echo "Deployment completed successfully!"
fi

# Get service status
echo ""
echo "Service Status:"
aws ecs describe-services \
    --cluster ${CLUSTER_NAME} \
    --services ${SERVICE_FULL_NAME} \
    --region ${AWS_REGION} \
    --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,Pending:pendingCount}' \
    --output table
