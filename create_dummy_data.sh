#!/bin/bash
set -x

# Base URL
API_URL="http://127.0.0.1:8091/api"
EVENT_API_URL="http://127.0.0.1:8092/api"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Starting dummy data creation...${NC}"

# 1. Register User
echo "Registering user..."
REGISTER_RESPONSE=$(curl -v -X POST "$API_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "confirmPassword": "Password123!",
    "firstName": "Test",
    "lastName": "User",
    "dateOfBirth": "1990-01-01"
  }')

echo "Register response: $REGISTER_RESPONSE"

# 2. Login
echo "Logging in..."
LOGIN_RESPONSE=$(curl -v -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }')

echo "Login response: $LOGIN_RESPONSE"
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')

if [ "$TOKEN" == "null" ]; then
    echo -e "${RED}Login failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Login successful! Token obtained.${NC}"

# 3. Get Categories
echo "Fetching categories..."
CATEGORIES_RESPONSE=$(curl -s -X GET "$EVENT_API_URL/categories")
echo "Categories response: $CATEGORIES_RESPONSE"
CATEGORY_ID=$(echo $CATEGORIES_RESPONSE | jq -r '.data[0].id')

if [ "$CATEGORY_ID" == "null" ]; then
    echo -e "${RED}Failed to get categories!${NC}"
    exit 1
fi

echo "Using Category ID: $CATEGORY_ID"

# 4. Create Event 1
echo "Creating Event 1..."
EVENT1_RESPONSE=$(curl -s -X POST "$EVENT_API_URL/events" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"name\": \"Summer Music Festival\",
    \"description\": \"Join us for the biggest music festival of the summer! Featuring top artists and great vibes.\",
    \"eventDate\": \"2026-07-15T18:00:00\",
    \"venue\": {
      \"name\": \"Central Park\",
      \"address\": \"59th St to 110th St\",
      \"city\": \"New York\",
      \"state\": \"NY\",
      \"zipCode\": \"10022\",
      \"country\": \"USA\",
      \"maxCapacity\": 5000
    },
    \"categoryId\": \"$CATEGORY_ID\",
    \"maxCapacity\": 5000,
    \"tags\": [\"music\", \"festival\", \"summer\"]
  }")

echo "Event 1 Response: $EVENT1_RESPONSE"

# 5. Create Event 2
echo "Creating Event 2..."
EVENT2_RESPONSE=$(curl -s -X POST "$EVENT_API_URL/events" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"name\": \"Tech Innovation Summit\",
    \"description\": \"A gathering of the brightest minds in technology. Discussing AI, Blockchain, and the future.\",
    \"eventDate\": \"2026-09-20T09:00:00\",
    \"venue\": {
      \"name\": \"Convention Center\",
      \"address\": \"123 Tech Blvd\",
      \"city\": \"San Francisco\",
      \"state\": \"CA\",
      \"zipCode\": \"94103\",
      \"country\": \"USA\",
      \"maxCapacity\": 2000
    },
    \"categoryId\": \"$CATEGORY_ID\",
    \"maxCapacity\": 2000,
    \"tags\": [\"tech\", \"innovation\", \"conference\"]
  }")

echo "Event 2 Response: $EVENT2_RESPONSE"

echo -e "${GREEN}Dummy data creation complete!${NC}"
