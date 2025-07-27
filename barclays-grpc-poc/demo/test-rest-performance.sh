#!/bin/bash
echo "🔍 Testing REST API Performance with Problematic Queries"
echo "======================================================="
echo ""

# Test 1: Multi-customer query with 50 customers
echo "Test 1: Querying 50 customers with metadata filters..."
customerIds='['
for i in {1..50}; do
  customerIds+='"CUST'$(printf "%06d" $i)'"'
  if [ $i -lt 50 ]; then
    customerIds+=','
  fi
done
customerIds+=']'

time curl -s -X POST http://localhost:8080/api/v1/customers/multi-query \
  -H "Content-Type: application/json" \
  -d '{
    "customerIds": '"$customerIds"',
    "metadataFilters": {
      "industry": "Banking",
      "riskRating": "Low"
    }
  }' | jq '{queryTimeMs, totalCount}'

echo ""
echo "Test 2: Transaction search with multiple metadata filters..."
time curl -s -X POST http://localhost:8080/api/v1/transactions/search \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumbers": ["ACC000001", "ACC000002", "ACC000003", "ACC000004", "ACC000005"],
    "metadataFilters": {
      "channel": "Online",
      "category": "Operational",
      "approvalCode": "APPR"
    }
  }' | jq '{queryTimeMs, totalCount}'

echo ""
echo "⚠️  Notice the high query times due to non-indexed metadata fields!"
echo "This is what's causing MongoDB CPU spikes in production."
