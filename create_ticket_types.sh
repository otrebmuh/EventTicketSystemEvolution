#!/bin/bash

# Event ID
EVENT_ID="0d3062b0-d39f-4077-8bcb-0bac27e058b3"
ORGANIZER_ID="09e68d1f-6694-4757-a16a-9755ea69b1b6"

# Auth Token
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJmaXJzdE5hbWUiOiJUZXN0IiwibGFzdE5hbWUiOiJVc2VyIiwiZW1haWxWZXJpZmllZCI6dHJ1ZSwidG9rZW5UeXBlIjoiYWNjZXNzIiwidXNlcklkIjoiMDllNjhkMWYtNjY5NC00NzU3LWExNmEtOTc1NWVhNjliMWI2IiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwic3ViIjoidGVzdEBleGFtcGxlLmNvbSIsImlzcyI6ImV2ZW50LWJvb2tpbmctc3lzdGVtIiwiYXVkIjpbImV2ZW50LWJvb2tpbmctdXNlcnMiXSwiaWF0IjoxNzYzODU2Mjc2LCJleHAiOjE3NjM5NDI2NzZ9.MpdCyE9eelbyV_UZROgC56XDIa-xtVK4SunS5humHN8"

# Create VIP Ticket Type
echo "Creating VIP Ticket Type..."
curl -X POST http://localhost:8093/api/ticket-types \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $ORGANIZER_ID" \
  -d "{
    \"eventId\": \"$EVENT_ID\",
    \"name\": \"VIP Access\",
    \"description\": \"Front row seats and backstage access\",
    \"price\": 150.00,
    \"quantityAvailable\": 100,
    \"saleStartDate\": \"$(date -u +%Y-%m-%dT%H:%M:%S)\",
    \"saleEndDate\": \"$(date -v+1m -u +%Y-%m-%dT%H:%M:%S)\",
    \"perPersonLimit\": 4,
    \"venueZone\": \"Front Row\"
  }"
echo -e "\n"

# Create General Admission Ticket Type
echo "Creating General Admission Ticket Type..."
curl -X POST http://localhost:8093/api/ticket-types \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $ORGANIZER_ID" \
  -d "{
    \"eventId\": \"$EVENT_ID\",
    \"name\": \"General Admission\",
    \"description\": \"Standard entry ticket\",
    \"price\": 50.00,
    \"quantityAvailable\": 1000,
    \"saleStartDate\": \"$(date -u +%Y-%m-%dT%H:%M:%S)\",
    \"saleEndDate\": \"$(date -v+1m -u +%Y-%m-%dT%H:%M:%S)\",
    \"perPersonLimit\": 10,
    \"venueZone\": \"General Area\"
  }"
echo -e "\n"
