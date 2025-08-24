# Simple Brokerage API Test Script
# Usage: .\simple_test.ps1

$BASE_URL = "http://localhost:8080"

Write-Host "🚀 Testing Brokerage API..." -ForegroundColor Green

# Test 1: Admin Login
Write-Host "`n🔐 Testing Admin Login..." -ForegroundColor Yellow
try {
    $adminData = @{
        username = "admin"
        password = "admin123"
    } | ConvertTo-Json

    $adminResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $adminData -ContentType "application/json"
    $adminToken = $adminResponse.token
    Write-Host "✅ Admin login successful!" -ForegroundColor Green
    Write-Host "Token: $($adminToken.Substring(0,20))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Customer Login
Write-Host "`n🔐 Testing Customer Login..." -ForegroundColor Yellow
try {
    $customerData = @{
        username = "customer1"
        password = "customer123"
    } | ConvertTo-Json

    $customerResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $customerData -ContentType "application/json"
    $customerToken = $customerResponse.token
    Write-Host "✅ Customer login successful!" -ForegroundColor Green
    Write-Host "Token: $($customerToken.Substring(0,20))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ Customer login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 3: List Assets
Write-Host "`n💰 Testing Asset Listing..." -ForegroundColor Yellow
try {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $assets = Invoke-RestMethod -Uri "$BASE_URL/api/assets?customerId=CUST001" -Headers $headers
    Write-Host "✅ Asset listing successful!" -ForegroundColor Green
    Write-Host "Found $($assets.Count) assets" -ForegroundColor Gray
} catch {
    Write-Host "❌ Asset listing failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Create Order
Write-Host "`n📈 Testing Order Creation..." -ForegroundColor Yellow
try {
    $orderData = @{
        customerId = "CUST001"
        assetName = "AAPL"
        orderSide = "BUY"
        size = 10
        price = 150.00
    } | ConvertTo-Json

    $headers = @{ 
        Authorization = "Bearer $customerToken"
        "Content-Type" = "application/json"
    }
    
    $order = Invoke-RestMethod -Uri "$BASE_URL/api/orders" -Method POST -Body $orderData -Headers $headers
    Write-Host "✅ Order creation successful!" -ForegroundColor Green
    Write-Host "Order ID: $($order.id)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Order creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: List Orders
Write-Host "`n📋 Testing Order Listing..." -ForegroundColor Yellow
try {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $orders = Invoke-RestMethod -Uri "$BASE_URL/api/orders?customerId=CUST001" -Headers $headers
    Write-Host "✅ Order listing successful!" -ForegroundColor Green
    Write-Host "Found $($orders.Count) orders" -ForegroundColor Gray
} catch {
    Write-Host "❌ Order listing failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Admin Pending Orders
Write-Host "`n👨‍💼 Testing Admin Pending Orders..." -ForegroundColor Yellow
try {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $pendingOrders = Invoke-RestMethod -Uri "$BASE_URL/api/orders/pending" -Headers $headers
    Write-Host "✅ Admin pending orders successful!" -ForegroundColor Green
    Write-Host "Found $($pendingOrders.Count) pending orders" -ForegroundColor Gray
} catch {
    Write-Host "❌ Admin pending orders failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 7: Security Test (No Token)
Write-Host "`n🚫 Testing Security (No Token)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/api/orders?customerId=CUST001"
    Write-Host "❌ Security test failed - Should have been denied!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Security test passed - Access denied as expected!" -ForegroundColor Green
    } else {
        Write-Host "❌ Unexpected error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n🎉 API Testing Completed!" -ForegroundColor Green
Write-Host "'I Will Get This Job!' 🚀" -ForegroundColor Cyan
