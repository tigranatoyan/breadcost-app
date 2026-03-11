$B = "http://localhost:8085"
$pass = 0; $fail = 0

function Test-Ok($name) { $script:pass++; Write-Host "  PASS: $name" -ForegroundColor Green }
function Test-Fail($name, $err) { $script:fail++; Write-Host "  FAIL: $name - $err" -ForegroundColor Red }

function Req($method, $url, $token, $body) {
    $h = @{ Accept = "application/json" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    $params = @{ Uri = "$B$url"; Method = $method; Headers = $h }
    if ($body) { $params["ContentType"] = "application/json"; $params["Body"] = $body }
    return Invoke-RestMethod @params
}

function ReqStatus($method, $url, $token, $body) {
    try {
        $h = @{ Accept = "application/json" }
        if ($token) { $h["Authorization"] = "Bearer $token" }
        $params = @{ Uri = "$B$url"; Method = $method; Headers = $h; ErrorAction = "Stop"; UseBasicParsing = $true }
        if ($body) { $params["ContentType"] = "application/json"; $params["Body"] = $body }
        Invoke-WebRequest @params | Out-Null
        return 200
    } catch {
        return $_.Exception.Response.StatusCode.value__
    }
}

# ====================================================================
Write-Host "`n=== 1. AUTHENTICATION ===" -ForegroundColor Cyan
# ====================================================================
try {
    $r = Req POST "/v1/auth/login" $null '{"username":"admin","password":"admin"}'
    $adminToken = $r.token; Test-Ok "Admin login (roles=$($r.roles -join ','), tenant=$($r.tenantId))"
} catch { Test-Fail "Admin login" $_.Exception.Message }

try {
    $r = Req POST "/v1/auth/login" $null '{"username":"manager","password":"manager"}'
    $mgrToken = $r.token; Test-Ok "Manager login (roles=$($r.roles -join ','))"
} catch { Test-Fail "Manager login" $_.Exception.Message }

try {
    $r = Req POST "/v1/auth/login" $null '{"username":"production","password":"production"}'
    $prodToken = $r.token; Test-Ok "Production login (roles=$($r.roles -join ','))"
} catch { Test-Fail "Production login" $_.Exception.Message }

try {
    $r = Req POST "/v1/auth/login" $null '{"username":"cashier","password":"cashier"}'
    $cashToken = $r.token; Test-Ok "Cashier login (roles=$($r.roles -join ','))"
} catch { Test-Fail "Cashier login" $_.Exception.Message }

try {
    $r = Req POST "/v1/auth/login" $null '{"username":"finance","password":"finance"}'
    $finToken = $r.token; Test-Ok "Finance login (roles=$($r.roles -join ','))"
} catch { Test-Fail "Finance login" $_.Exception.Message }

$code = ReqStatus POST "/v1/auth/login" $null '{"username":"admin","password":"wrong"}'
if ($code -eq 401) { Test-Ok "Bad credentials rejected (401)" } else { Test-Fail "Bad credentials" "Expected 401, got $code" }

$code = ReqStatus GET "/v1/departments?tenantId=tenant1" $null
if ($code -eq 401 -or $code -eq 403) { Test-Ok "Unauthenticated request rejected ($code)" } else { Test-Fail "Unauthenticated request" "Expected 401/403, got $code" }

# ====================================================================
Write-Host "`n=== 2. DEPARTMENTS (CRUD) ===" -ForegroundColor Cyan
# ====================================================================
try {
    $dept = Req POST "/v1/departments" $adminToken '{"tenantId":"tenant1","name":"E2E Test Dept","leadTimeHours":6,"warehouseMode":"SHARED"}'
    $deptId = $dept.departmentId
    Test-Ok "Create department ($($dept.name), id=$deptId)"
} catch { Test-Fail "Create department" $_.Exception.Message }

try {
    $depts = Req GET "/v1/departments?tenantId=tenant1" $adminToken
    if ($depts.Count -ge 1) { Test-Ok "List departments ($($depts.Count) found)" } else { Test-Fail "List departments" "Empty" }
} catch { Test-Fail "List departments" $_.Exception.Message }

try {
    $d = Req GET "/v1/departments/$deptId" $adminToken
    if ($d.name -eq "E2E Test Dept") { Test-Ok "Get department by ID" } else { Test-Fail "Get dept by ID" "Wrong name: $($d.name)" }
} catch { Test-Fail "Get department by ID" $_.Exception.Message }

try {
    $upd = Req PUT "/v1/departments/$deptId" $adminToken '{"name":"E2E Updated","leadTimeHours":12,"warehouseMode":"ISOLATED","status":"ACTIVE"}'
    if ($upd.leadTimeHours -eq 12 -and $upd.warehouseMode -eq "ISOLATED") { Test-Ok "Update department (leadTime=12, ISOLATED)" } else { Test-Fail "Update dept" "Wrong values" }
} catch { Test-Fail "Update department" $_.Exception.Message }

# ====================================================================
Write-Host "`n=== 3. PRODUCTS ===" -ForegroundColor Cyan
# ====================================================================
try {
    $prods = Req GET "/v1/products?tenantId=tenant1" $adminToken
    Test-Ok "List products ($($prods.Count) found)"
    $firstProdId = $prods[0].productId
} catch { Test-Fail "List products" $_.Exception.Message }

if ($firstProdId) {
    try {
        $p = Req GET "/v1/products/$firstProdId" $adminToken
        Test-Ok "Get product by ID ($($p.name))"
    } catch { Test-Fail "Get product by ID" $_.Exception.Message }
}

# ====================================================================
Write-Host "`n=== 4. RECIPES ===" -ForegroundColor Cyan
# ====================================================================
if ($firstProdId) {
    try {
        $recipes = Req GET "/v1/recipes?tenantId=tenant1&productId=$firstProdId" $adminToken
        Test-Ok "List recipes for product ($($recipes.Count) found)"
    } catch { Test-Fail "List recipes" $_.Exception.Message }
}

# ====================================================================
Write-Host "`n=== 5. ROLE-BASED ACCESS CONTROL ===" -ForegroundColor Cyan
# ====================================================================
# Manager can list departments
$code = ReqStatus GET "/v1/departments?tenantId=tenant1" $mgrToken
if ($code -eq 200) { Test-Ok "Manager CAN list departments (200)" } else { Test-Fail "Manager list depts" "Expected 200, got $code" }

# Manager cannot create departments
$code = ReqStatus POST "/v1/departments" $mgrToken '{"tenantId":"tenant1","name":"Mgr Dept","leadTimeHours":1,"warehouseMode":"SHARED"}'
if ($code -eq 403) { Test-Ok "Manager CANNOT create dept (403)" } else { Test-Fail "Manager create dept" "Expected 403, got $code" }

# Cashier cannot list departments
$code = ReqStatus GET "/v1/departments?tenantId=tenant1" $cashToken
if ($code -eq 403) { Test-Ok "Cashier CANNOT list departments (403)" } else { Test-Fail "Cashier list depts" "Expected 403, got $code" }

# Manager can list inventory positions 
$code = ReqStatus GET "/v1/inventory/positions?tenantId=tenant1" $mgrToken
if ($code -eq 200) { Test-Ok "Manager CAN read inventory (200)" } else { Test-Fail "Manager inventory" "Expected 200, got $code" }

# Production user can list orders
$code = ReqStatus GET "/v1/orders?tenantId=tenant1" $prodToken
if ($code -eq 200) { Test-Ok "ProductionUser CAN list orders (200)" } else { Test-Fail "Production orders" "Expected 200, got $code" }

# ====================================================================
Write-Host "`n=== 6. ORDERS ===" -ForegroundColor Cyan
# ====================================================================
try {
    $orders = Req GET "/v1/orders?tenantId=tenant1" $adminToken
    Test-Ok "List orders ($($orders.Count) found)"
} catch { Test-Fail "List orders" $_.Exception.Message }

try {
    $order = Req POST "/v1/orders" $adminToken (@{
        tenantId = "tenant1"
        customerName = "E2E Customer"
        customerPhone = "+37494123456"
        orderDate = "2026-03-09"
        deliveryDate = "2026-03-10"
        notes = "E2E test order"
        lines = @(@{productId = $firstProdId; quantity = 5; unitPriceMinor = 1000})
    } | ConvertTo-Json)
    $orderId = $order.orderId
    Test-Ok "Create order (id=$orderId, customer=$($order.customerName))"
} catch { Test-Fail "Create order" $_.Exception.Message }

if ($orderId) {
    try {
        $o = Req GET "/v1/orders/${orderId}?tenantId=tenant1" $adminToken
        Test-Ok "Get order by ID (lines=$($o.lines.Count))"
    } catch { Test-Fail "Get order by ID" $_.Exception.Message }
}

# ====================================================================
Write-Host "`n=== 7. PRODUCTION PLANS ===" -ForegroundColor Cyan
# ====================================================================
try {
    $plan = Req POST "/v1/production-plans" $adminToken (@{
        tenantId = "tenant1"
        name = "E2E Plan"
        planDate = "2026-03-10"
        departmentId = $deptId
    } | ConvertTo-Json)
    $planId = $plan.planId
    Test-Ok "Create plan (id=$planId, status=$($plan.status))"
} catch { Test-Fail "Create plan" $_.Exception.Message }

if ($planId) {
    try {
        $plans = Req GET "/v1/production-plans?tenantId=tenant1" $adminToken
        Test-Ok "List plans ($($plans.Count) found)"
    } catch { Test-Fail "List plans" $_.Exception.Message }

    try {
        $p = Req GET "/v1/production-plans/$planId`?tenantId=tenant1" $adminToken
        Test-Ok "Get plan by ID (status=$($p.status))"
    } catch { Test-Fail "Get plan by ID" $_.Exception.Message }

    # Generate plan
    $code = ReqStatus POST "/v1/production-plans/$planId/generate?tenantId=tenant1" $adminToken
    if ($code -eq 200) { Test-Ok "Generate plan (200)" } else { Test-Fail "Generate plan" "Expected 200, got $code" }

    # Approve plan (Admin can)
    $code = ReqStatus POST "/v1/production-plans/$planId/approve?tenantId=tenant1" $adminToken
    if ($code -eq 200) { Test-Ok "Admin approve plan (200)" } else { Test-Fail "Admin approve" "Expected 200, got $code" }

    # Floor worker cannot approve (should be 403)
    try {
        $plan2 = Req POST "/v1/production-plans" $adminToken (@{
            tenantId = "tenant1"; name = "E2E Plan 2"; planDate = "2026-03-11"; departmentId = $deptId
        } | ConvertTo-Json)
        $plan2Id = $plan2.planId
        ReqStatus POST "/v1/production-plans/$plan2Id/generate?tenantId=tenant1" $adminToken | Out-Null
        $code = ReqStatus POST "/v1/production-plans/$plan2Id/approve?tenantId=tenant1" $prodToken
        if ($code -eq 403) { Test-Ok "ProductionUser CANNOT approve plan (403)" } else { Test-Fail "ProdUser approve" "Expected 403, got $code" }
    } catch { Test-Fail "ProdUser approve setup" $_.Exception.Message }
}

# ====================================================================
Write-Host "`n=== 8. INVENTORY ===" -ForegroundColor Cyan
# ====================================================================
try {
    $inv = Req GET "/v1/inventory/positions?tenantId=tenant1" $adminToken
    Test-Ok "List inventory positions ($($inv.Count) found)"
} catch { Test-Fail "List inventory" $_.Exception.Message }

try {
    $alerts = Req GET "/v1/inventory/alerts?tenantId=tenant1" $adminToken
    Test-Ok "List inventory alerts ($($alerts.Count) found)"
} catch { Test-Fail "Inventory alerts" $_.Exception.Message }

# ====================================================================
Write-Host "`n=== 9. POS / SALES ===" -ForegroundColor Cyan
# ====================================================================
try {
    $sales = Req GET "/v1/pos/sales?tenantId=tenant1" $adminToken
    Test-Ok "List POS sales ($($sales.Count) found)"
} catch { Test-Fail "List POS sales" $_.Exception.Message }

# ====================================================================
Write-Host "`n=== 10. USERS (Admin) ===" -ForegroundColor Cyan
# ====================================================================
try {
    $users = Req GET "/v1/users?tenantId=tenant1" $adminToken
    Test-Ok "List users ($($users.Count) found)"
} catch { Test-Fail "List users" $_.Exception.Message }

# ====================================================================
Write-Host "`n=== 11. REPORTS ===" -ForegroundColor Cyan
# ====================================================================
try {
    $rev = Req GET "/v1/reports/revenue-summary?tenantId=tenant1" $adminToken
    Test-Ok "Revenue summary report"
} catch { Test-Fail "Revenue summary" $_.Exception.Message }

try {
    $top = Req GET "/v1/reports/top-products?tenantId=tenant1&limit=5" $adminToken
    Test-Ok "Top products report"
} catch { Test-Fail "Top products" $_.Exception.Message }

# ====================================================================
Write-Host "`n`n=====================================" -ForegroundColor Yellow
Write-Host "  E2E RESULTS: $pass PASS, $fail FAIL" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "=====================================" -ForegroundColor Yellow
