#!/bin/bash

# Brokerage API Test Suite
# Usage: ./test_api.sh

BASE_URL="http://localhost:8080"
ADMIN_TOKEN=""
CUSTOMER1_TOKEN=""
CUSTOMER2_TOKEN=""

echo "🚀 Starting Brokerage API Test Suite..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

test_endpoint() {
    local name="$1"
    local expected_status="$2"
    local curl_command="$3"
    
    echo -e "\n📋 Testing: $name"
    echo "Command: $curl_command"
    
    response=$(eval "$curl_command")
    status=$?
    
    if [ $status -eq 0 ]; then
        echo -e "${GREEN}✅ $name PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ $name FAILED${NC}"
        echo "Response: $response"
        ((TESTS_FAILED++))
    fi
}

# 1. Authentication Tests
echo -e "\n🔐 === AUTHENTICATION TESTS ==="

# Admin Login
echo "Getting admin token..."
ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | jq -r '.token')
echo "Admin token: ${ADMIN_TOKEN:0:20}..."

# Customer1 Login
echo "Getting customer1 token..."
CUSTOMER1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"customer123"}')

CUSTOMER1_TOKEN=$(echo $CUSTOMER1_RESPONSE | jq -r '.token')
echo "Customer1 token: ${CUSTOMER1_TOKEN:0:20}..."

# 2. Asset Tests
echo -e "\n💰 === ASSET TESTS ==="

test_endpoint "List CUST001 Assets (Customer1)" 200 \
  "curl -s -X GET '$BASE_URL/api/assets?customerId=CUST001' -H 'Authorization: Bearer $CUSTOMER1_TOKEN'"

test_endpoint "Get TRY Asset (Customer1)" 200 \
  "curl -s -X GET '$BASE_URL/api/assets/TRY?customerId=CUST001' -H 'Authorization: Bearer $CUSTOMER1_TOKEN'"

# 3. Order Tests  
echo -e "\n📈 === ORDER TESTS ==="

test_endpoint "Create BUY Order" 200 \
  "curl -s -X POST '$BASE_URL/api/orders' \
   -H 'Authorization: Bearer $CUSTOMER1_TOKEN' \
   -H 'Content-Type: application/json' \
   -d '{\"customerId\":\"CUST001\",\"assetName\":\"AAPL\",\"orderSide\":\"BUY\",\"size\":10,\"price\":150.00}'"

test_endpoint "List Customer Orders" 200 \
  "curl -s -X GET '$BASE_URL/api/orders?customerId=CUST001' -H 'Authorization: Bearer $CUSTOMER1_TOKEN'"

# 4. Admin Tests
echo -e "\n👨‍💼 === ADMIN TESTS ==="

test_endpoint "List Pending Orders (Admin)" 200 \
  "curl -s -X GET '$BASE_URL/api/orders/pending' -H 'Authorization: Bearer $ADMIN_TOKEN'"

test_endpoint "List Pending Orders (Customer - Should Fail)" 403 \
  "curl -s -w '%{http_code}' -X GET '$BASE_URL/api/orders/pending' -H 'Authorization: Bearer $CUSTOMER1_TOKEN'"

# 5. Security Tests
echo -e "\n🚫 === SECURITY TESTS ==="

test_endpoint "No Token Access (Should Fail)" 401 \
  "curl -s -w '%{http_code}' -X GET '$BASE_URL/api/orders?customerId=CUST001'"

test_endpoint "Invalid Token (Should Fail)" 401 \
  "curl -s -w '%{http_code}' -X GET '$BASE_URL/api/orders?customerId=CUST001' -H 'Authorization: Bearer invalid.token'"

# Results
echo -e "\n📊 === TEST RESULTS ==="
echo -e "${GREEN}✅ Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}❌ Tests Failed: $TESTS_FAILED${NC}"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
SUCCESS_RATE=$(( TESTS_PASSED * 100 / TOTAL_TESTS ))
echo -e "${YELLOW}📈 Success Rate: $SUCCESS_RATE%${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n🎉 ${GREEN}ALL TESTS PASSED!${NC} 🎉"
    echo "API is working perfectly! 'I Will Get This Job!' 🚀"
else
    echo -e "\n⚠️  ${YELLOW}Some tests failed. Check the output above.${NC}"
fi
