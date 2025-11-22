#!/bin/bash

# Deploy all services
# Usage: ./deploy-all-services.sh <image-tag> <environment>

set -e

IMAGE_TAG=${1:-latest}
ENVIRONMENT=${2:-prod}

SERVICES=("auth-service" "event-service" "ticket-service" "payment-service" "notification-service")

echo "Deploying all services to ${ENVIRONMENT} environment with tag ${IMAGE_TAG}"
echo "Services: ${SERVICES[@]}"
echo ""

# Deploy services in order (respecting dependencies)
for SERVICE in "${SERVICES[@]}"; do
    echo "========================================="
    echo "Deploying ${SERVICE}..."
    echo "========================================="
    
    ./deploy-service.sh ${SERVICE} ${IMAGE_TAG} ${ENVIRONMENT}
    
    echo ""
    echo "${SERVICE} deployment completed!"
    echo ""
    
    # Wait a bit between deployments to avoid overwhelming the cluster
    sleep 10
done

echo "========================================="
echo "All services deployed successfully!"
echo "========================================="
