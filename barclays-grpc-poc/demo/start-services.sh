#!/bin/bash
echo "🚀 Starting Barclays gRPC POC services..."

# Start MongoDB
echo "Starting MongoDB..."
docker compose up -d mongodb

# Wait for MongoDB to initialize
echo "Waiting for MongoDB to initialize..."
sleep 10

# Build and start REST API
echo "Building and starting REST API..."
docker compose up -d --build rest-api

echo "✅ Services started!"
echo ""
echo "Services available at:"
echo "  - MongoDB: localhost:27017"
echo "  - REST API: http://localhost:8080"
echo ""
echo "Test REST API health: curl http://localhost:8080/health"