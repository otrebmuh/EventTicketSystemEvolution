#!/bin/bash

# Build and push Docker images to ECR
# Usage: ./build-and-push.sh <service-name> <image-tag> <aws-region> <aws-account-id>

set -e

SERVICE_NAME=$1
IMAGE_TAG=${2:-latest}
AWS_REGION=${3:-us-east-1}
AWS_ACCOUNT_ID=$4

if [ -z "$SERVICE_NAME" ] || [ -z "$AWS_ACCOUNT_ID" ]; then
    echo "Usage: ./build-and-push.sh <service-name> <image-tag> <aws-region> <aws-account-id>"
    echo "Example: ./build-and-push.sh auth-service v1.0.0 us-east-1 123456789012"
    exit 1
fi

# ECR repository URL
ECR_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/event-booking-${SERVICE_NAME}"

echo "Building Docker image for ${SERVICE_NAME}..."

# Navigate to project root
cd "$(dirname "$0")/../.."

# Build the Docker image using production Dockerfile
docker build \
    -f ${SERVICE_NAME}/Dockerfile.prod \
    -t event-booking-${SERVICE_NAME}:${IMAGE_TAG} \
    -t event-booking-${SERVICE_NAME}:latest \
    .

echo "Logging in to ECR..."
aws ecr get-login-password --region ${AWS_REGION} | \
    docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# Create ECR repository if it doesn't exist
aws ecr describe-repositories --repository-names event-booking-${SERVICE_NAME} --region ${AWS_REGION} || \
    aws ecr create-repository --repository-name event-booking-${SERVICE_NAME} --region ${AWS_REGION}

echo "Tagging image..."
docker tag event-booking-${SERVICE_NAME}:${IMAGE_TAG} ${ECR_REPO}:${IMAGE_TAG}
docker tag event-booking-${SERVICE_NAME}:latest ${ECR_REPO}:latest

echo "Pushing image to ECR..."
docker push ${ECR_REPO}:${IMAGE_TAG}
docker push ${ECR_REPO}:latest

echo "Successfully pushed ${SERVICE_NAME} image with tag ${IMAGE_TAG}"
echo "Image URI: ${ECR_REPO}:${IMAGE_TAG}"
