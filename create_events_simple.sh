#!/bin/bash

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJmaXJzdE5hbWUiOiJUZXN0IiwibGFzdE5hbWUiOiJVc2VyIiwiZW1haWxWZXJpZmllZCI6dHJ1ZSwidG9rZW5UeXBlIjoiYWNjZXNzIiwidXNlcklkIjoiMDllNjhkMWYtNjY5NC00NzU3LWExNmEtOTc1NWVhNjliMWI2IiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwic3ViIjoidGVzdEBleGFtcGxlLmNvbSIsImlzcyI6ImV2ZW50LWJvb2tpbmctc3lzdGVtIiwiYXVkIjpbImV2ZW50LWJvb2tpbmctdXNlcnMiXSwiaWF0IjoxNzYzODU2Mjc2LCJleHAiOjE3NjM5NDI2NzZ9.MpdCyE9eelbyV_UZROgC56XDIa-xtVK4SunS5humHN8"
CATEGORY_ID="11f1a181-56ee-484e-9d34-2f27878f56c3"

# Create Event 1
curl -s -X POST http://127.0.0.1:8092/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"name\": \"Summer Music Festival\",
    \"description\": \"Join us for the biggest music festival of the summer featuring top artists and great vibes!\",
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
  }" | jq

# Create Event 2  
curl -s -X POST http://127.0.0.1:8092/api/events \
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
  }" | jq
