#!/bin/bash
echo "🚀 Starting Barclays gRPC POC services..."

# Clean up any existing containers
echo "Cleaning up existing containers..."
docker compose down

# Start MongoDB
echo "Starting MongoDB..."
docker compose up -d mongodb

# Wait for MongoDB to initialize
echo "Waiting for MongoDB to initialize..."
sleep 15

# Build and start REST API
echo "Building and starting REST API..."
docker compose up -d --build rest-api

# Build and start gRPC API
echo "Building and starting gRPC API..."
docker compose up -d --build grpc-api

echo "Waiting for services to start..."
sleep 30

echo "✅ Services started!"
echo ""
echo "Services available at:"
echo "  - MongoDB: localhost:27017"
echo "  - REST API: http://localhost:8080"
echo "  - gRPC API: localhost:9090"
echo ""
echo "Testing service health..."

# Test REST API
REST_HEALTH=$(curl -s http://localhost:8080/health 2>/dev/null || echo "❌ Not responding")
echo "REST API: $REST_HEALTH"

# Test gRPC connectivity
echo "gRPC API: Testing connection..."
if docker logs barclays-grpc-api 2>&1 | grep -q "Started.*in"; then
    echo "gRPC API: ✅ Running"
else
    echo "gRPC API: ⚠️ Check logs with: docker logs barclays-grpc-api"
    echo ""
    echo "Recent gRPC logs:"
    docker logs barclays-grpc-api --tail 10
fi

echo ""
echo "Ready for performance testing! Run: ./demo/run-comparison.sh"