# BreadCost R1 — Manual Test Plan
**Version:** 1.0 | **Date:** 2026-03-04 | **Tester:** _____________  
**App URL:** http://localhost:8080 | **API Base:** http://localhost:8080/v1

---

## Test Accounts (password = username)

| Username | Password | Role | Default Screen |
|----------|----------|------|----------------|
| `admin` | `admin` | Admin | Dashboard |
| `manager` | `manager` | Manager | Dashboard |
| `technologist` | `technologist` | Technologist | Recipes |
| `production` | `production` | ProductionUser (Floor) | Floor |
| `finance` | `finance` | FinanceUser | Reports |
| `warehouse` | `warehouse` | Warehouse | Inventory |
| `cashier` | `cashier` | Cashier | POS |

**Tenant ID for all API calls:** `tenant1`

---

## How to Use This Document

Each test case has:
- **TC-ID**: Unique test case ID
- **Requirement**: Which FE/FR requirement it verifies
- **Precondition**: What must be true before you start
- **Steps**: Exact actions to perform (API calls via curl or a REST client like Postman/Insomnia)
- **Expected Result**: What you should see
- **Result**: ✅ Pass / ❌ Fail / ⏭ Skipped (fill in during testing)
- **Notes**: Space for observations

> **Note:** This is a backend-only app (no frontend yet). All tests use the REST API.
> Use Postman, Insomnia, curl, or the H2 console (http://localhost:8080/h2-console) to verify.

---

## Section 1: Authentication (FE-LOGIN)

### TC-AUTH-01 — Successful Login (Admin)
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-2, FE-LOGIN-3, FR-11.4 |
| **Precondition** | App is running, admin user seeded |
| **Steps** | 1. Send `POST /v1/auth/login` with body: `{"username":"admin","password":"admin"}` |
| **Expected** | HTTP 200. Response contains: `token` (JWT string), `username: "admin"`, `roles: ["Admin"]`, `tenantId: "tenant1"`, `primaryRole: "Admin"` |
| **Result** | |
| **Notes** | Save the token — you'll need it for all subsequent requests as `Authorization: Bearer <token>` |

### TC-AUTH-02 — Wrong Password
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-3 |
| **Precondition** | App is running |
| **Steps** | 1. Send `POST /v1/auth/login` with body: `{"username":"admin","password":"wrong"}` |
| **Expected** | HTTP 401. Body: `{"message":"Invalid username or password"}` |
| **Result** | |

### TC-AUTH-03 — Unknown Username
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-3 |
| **Precondition** | App is running |
| **Steps** | 1. Send `POST /v1/auth/login` with body: `{"username":"nobody","password":"pass"}` |
| **Expected** | HTTP 401. Body: `{"message":"Invalid username or password"}` |
| **Result** | |

### TC-AUTH-04 — Blank Fields Rejected
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-2 |
| **Precondition** | App is running |
| **Steps** | 1. Send `POST /v1/auth/login` with body: `{"username":"","password":""}` |
| **Expected** | HTTP 400 (validation error) |
| **Result** | |

### TC-AUTH-05 — No Token = 401
| Field | Value |
|-------|-------|
| **Requirement** | NFR-G.4, NFR-FE-11 |
| **Precondition** | App is running |
| **Steps** | 1. Send `GET /v1/orders?tenantId=tenant1` with NO Authorization header |
| **Expected** | HTTP 401 Unauthorized |
| **Result** | |

### TC-AUTH-06 — Login with Each Role
| Field | Value |
|-------|-------|
| **Requirement** | FR-11.4, FE-LOGIN-3 |
| **Precondition** | All 7 users seeded |
| **Steps** | For each user in the accounts table above: send `POST /v1/auth/login` with `{"username":"<user>","password":"<user>"}` |
| **Expected** | Each returns HTTP 200 with correct `username`, `roles`, `tenantId: "tenant1"` |
| **Result** | |

---

## Section 2: Orders (FE-ORD)

> **Login first:** All requests need `Authorization: Bearer <admin-token>`

### TC-ORD-01 — List Orders (Empty)
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-2 |
| **Precondition** | Logged in as admin. No orders created yet. |
| **Steps** | 1. `GET /v1/orders?tenantId=tenant1` |
| **Expected** | HTTP 200. Body: empty array `[]` |
| **Result** | |

### TC-ORD-02 — Create DRAFT Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-3, FR-1.1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/orders` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "customerName": "Al-Bukhara Restaurant",
  "requestedDeliveryTime": "2026-03-05T08:00:00Z",
  "forceRush": false,
  "notes": "Test order #1",
  "lines": [
    {"productId":"PROD-WHITE","productName":"White Bread","qty":50,"uom":"PCS","unitPrice":8000},
    {"productId":"PROD-SOUR","productName":"Sourdough Bread","qty":20,"uom":"PCS","unitPrice":15000}
  ]
}
```
| **Expected** | HTTP 201. Response has `orderId` (UUID), `status: "DRAFT"`, 2 lines with correct amounts, `tenantId: "tenant1"` |
| **Result** | |
| **Notes** | **Save the `orderId`** — needed for TC-ORD-03 through TC-ORD-06 |

### TC-ORD-03 — List Orders Shows Created Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-2 |
| **Precondition** | TC-ORD-02 completed |
| **Steps** | 1. `GET /v1/orders?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 1 order. Status = "DRAFT". Customer name and lines match. |
| **Result** | |

### TC-ORD-04 — Filter Orders by Status
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-2 |
| **Precondition** | TC-ORD-02 completed |
| **Steps** | 1. `GET /v1/orders?tenantId=tenant1&status=DRAFT` → should return the order |
| | 2. `GET /v1/orders?tenantId=tenant1&status=CONFIRMED` → should return empty |
| **Expected** | Step 1: array with 1 order. Step 2: empty array. |
| **Result** | |

### TC-ORD-05 — Confirm Order (DRAFT → CONFIRMED)
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-3, FE-ORD-5, FR-1.17 |
| **Precondition** | TC-ORD-02 completed. You have the `orderId`. |
| **Steps** | 1. `POST /v1/orders/{orderId}/confirm?tenantId=tenant1` |
| **Expected** | HTTP 200. `status: "CONFIRMED"`, `confirmedAt` populated with timestamp |
| **Result** | |

### TC-ORD-06 — Double Confirm Rejected
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-5 |
| **Precondition** | TC-ORD-05 completed (order already CONFIRMED) |
| **Steps** | 1. `POST /v1/orders/{orderId}/confirm?tenantId=tenant1` (same order) |
| **Expected** | HTTP 409 Conflict. `"Only DRAFT orders can be confirmed"` |
| **Result** | |

### TC-ORD-07 — Cancel DRAFT Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-4, FR-1.16 |
| **Precondition** | Create a new DRAFT order (repeat TC-ORD-02 with different data) |
| **Steps** | 1. `POST /v1/orders/{newOrderId}/cancel?tenantId=tenant1&reason=Customer+changed+mind` |
| **Expected** | HTTP 200. `status: "CANCELLED"` |
| **Result** | |

### TC-ORD-08 — Cancel CONFIRMED Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-4, FR-1.16 |
| **Precondition** | Create a new order, confirm it |
| **Steps** | 1. Create order (POST /v1/orders) 2. Confirm it 3. `POST /v1/orders/{id}/cancel?tenantId=tenant1&reason=Out+of+stock` |
| **Expected** | HTTP 200. `status: "CANCELLED"` |
| **Result** | |

### TC-ORD-09 — Floor Worker Can Read Orders
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-1 (spec gap — BE allows ProductionUser) |
| **Precondition** | Login as `production` user, get token |
| **Steps** | 1. `GET /v1/orders?tenantId=tenant1` with production token |
| **Expected** | HTTP 200. Returns order list. (Note: BE allows ProductionUser; FE spec restricts to admin/management) |
| **Result** | |

### TC-ORD-10 — Warehouse Cannot Create Orders
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-1, NFR-G.5 |
| **Precondition** | Login as `warehouse` user |
| **Steps** | 1. `POST /v1/orders` (same body as TC-ORD-02) with warehouse token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

---

## Section 3: Production Plans (FE-PP)

### TC-PP-01 — Create Production Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-3, FR-3.1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/production-plans` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "planDate": "2026-03-05",
  "shift": "MORNING",
  "notes": "Test plan for tomorrow"
}
```
| **Expected** | HTTP 201. `planId` set, `status: "DRAFT"`, `shift: "MORNING"` |
| **Result** | |
| **Notes** | **Save the `planId`** |

### TC-PP-02 — List Plans
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-2 |
| **Precondition** | TC-PP-01 completed |
| **Steps** | 1. `GET /v1/production-plans?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 1 plan in DRAFT status. |
| **Result** | |

### TC-PP-03 — Generate Work Orders
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-4, FR-3.1, FR-3.2, FR-3.5 |
| **Precondition** | TC-PP-01 completed. Orders exist (from Section 2). |
| **Steps** | 1. `POST /v1/production-plans/{planId}/generate?tenantId=tenant1` |
| **Expected** | HTTP 200. Plan status becomes "GENERATED". Response may include work orders. |
| **Result** | |

### TC-PP-04 — View Work Orders for Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-5 |
| **Precondition** | TC-PP-03 completed |
| **Steps** | 1. `GET /v1/production-plans/{planId}?tenantId=tenant1` |
| **Expected** | HTTP 200. Plan detail with `workOrders` array containing generated WOs. |
| **Result** | |

### TC-PP-05 — Approve Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-6, FR-3.4 |
| **Precondition** | TC-PP-03 completed (plan is GENERATED) |
| **Steps** | 1. `POST /v1/production-plans/{planId}/approve?tenantId=tenant1` |
| **Expected** | HTTP 200. `status: "APPROVED"` |
| **Result** | |

### TC-PP-06 — Start Plan
| Field | Value |
|-------|-------|
| **Requirement** | FR-3.6 |
| **Precondition** | TC-PP-05 completed (plan is APPROVED) |
| **Steps** | 1. `POST /v1/production-plans/{planId}/start?tenantId=tenant1` |
| **Expected** | HTTP 200. `status: "IN_PROGRESS"` |
| **Result** | |

### TC-PP-07 — Complete Plan
| Field | Value |
|-------|-------|
| **Requirement** | FR-3.7 |
| **Precondition** | TC-PP-06 completed (plan is IN_PROGRESS) |
| **Steps** | 1. `POST /v1/production-plans/{planId}/complete?tenantId=tenant1` |
| **Expected** | HTTP 200. `status: "COMPLETED"` |
| **Result** | |

### TC-PP-08 — Work Order Lifecycle (Start → Complete)
| Field | Value |
|-------|-------|
| **Requirement** | FR-3.6, FR-3.7, FE-FLOOR-4 |
| **Precondition** | Plan generated with work orders (TC-PP-03). Get a workOrderId from TC-PP-04. |
| **Steps** | 1. `POST /v1/production-plans/work-orders/{woId}/start?tenantId=tenant1` → expect status IN_PROGRESS |
| | 2. `POST /v1/production-plans/work-orders/{woId}/complete?tenantId=tenant1` → expect COMPLETED |
| **Expected** | Step 1: WO status IN_PROGRESS. Step 2: WO status COMPLETED. |
| **Result** | |

### TC-PP-09 — Floor Worker Can Approve Plan (Spec Gap)
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-6 (spec says management only; BE allows ProductionUser) |
| **Precondition** | Create & generate a new plan. Login as production. |
| **Steps** | 1. `POST /v1/production-plans/{planId}/approve?tenantId=tenant1` with production token |
| **Expected** | HTTP 200 (BE allows it). **Note: FE spec says 403 — this is a known spec gap.** |
| **Result** | |

### TC-PP-10 — Finance User Can Read Plans
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-1 |
| **Precondition** | Login as finance |
| **Steps** | 1. `GET /v1/production-plans?tenantId=tenant1` with finance token |
| **Expected** | HTTP 200. Read access granted. |
| **Result** | |

---

## Section 4: Inventory (FE-INV)

### TC-INV-01 — View Inventory Positions (Empty)
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-2, FR-5.6 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `GET /v1/inventory/positions?tenantId=tenant1` |
| **Expected** | HTTP 200. Empty array (no stock received yet). |
| **Result** | |

### TC-INV-02 — Receive Stock (Wheat Flour)
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-3, FR-5.1, FR-5.2 |
| **Precondition** | Logged in as admin or warehouse |
| **Steps** | 1. `POST /v1/inventory/receipts` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "receiptId": "RCP-001",
  "itemId": "ITEM-FLOUR",
  "lotId": "LOT-FLOUR-001",
  "qty": 100,
  "uom": "KG",
  "unitCostBase": 5000,
  "occurredAtUtc": "2026-03-04T10:00:00Z",
  "idempotencyKey": "idem-flour-001"
}
```
| **Expected** | HTTP 200. `success: true`. |
| **Result** | |

### TC-INV-03 — Verify Stock After Receipt
| Field | Value |
|-------|-------|
| **Requirement** | FR-5.2, FR-5.6 |
| **Precondition** | TC-INV-02 completed |
| **Steps** | 1. `GET /v1/inventory/positions?tenantId=tenant1` |
| **Expected** | HTTP 200. Array includes ITEM-FLOUR with on-hand qty = 100 KG. |
| **Result** | |

### TC-INV-04 — Receive Multiple Items
| Field | Value |
|-------|-------|
| **Requirement** | FR-5.1 |
| **Precondition** | Logged in as admin |
| **Steps** | Receive each item: |
```json
// Sugar
{"tenantId":"tenant1","siteId":"MAIN","receiptId":"RCP-002","itemId":"ITEM-SUGAR","lotId":"LOT-SUGAR-001","qty":50,"uom":"KG","unitCostBase":8000,"occurredAtUtc":"2026-03-04T10:00:00Z","idempotencyKey":"idem-sugar-001"}

// Butter
{"tenantId":"tenant1","siteId":"MAIN","receiptId":"RCP-003","itemId":"ITEM-BUTTER","lotId":"LOT-BUTTER-001","qty":25,"uom":"KG","unitCostBase":45000,"occurredAtUtc":"2026-03-04T10:00:00Z","idempotencyKey":"idem-butter-001"}

// Yeast
{"tenantId":"tenant1","siteId":"MAIN","receiptId":"RCP-004","itemId":"ITEM-YEAST","lotId":"LOT-YEAST-001","qty":10,"uom":"KG","unitCostBase":25000,"occurredAtUtc":"2026-03-04T10:00:00Z","idempotencyKey":"idem-yeast-001"}
```
| **Expected** | Each returns HTTP 200 with `success: true`. Stock positions show all items. |
| **Result** | |

### TC-INV-05 — Idempotency (Same Key Ignored)
| Field | Value |
|-------|-------|
| **Requirement** | NFR-G.11 |
| **Precondition** | TC-INV-02 completed |
| **Steps** | 1. Resend the exact same receipt from TC-INV-02 (same `idempotencyKey`) |
| **Expected** | HTTP 200. `success: true` but stock does NOT increase (still 100 KG flour). Verify with GET positions. |
| **Result** | |

### TC-INV-06 — Inventory Transfer
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-5, FR-5.4 |
| **Precondition** | TC-INV-02 completed (flour in stock) |
| **Steps** | 1. `POST /v1/inventory/transfers` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "itemId": "ITEM-FLOUR",
  "qty": 20,
  "fromLocationId": "WAREHOUSE",
  "toLocationId": "PRODUCTION",
  "occurredAtUtc": "2026-03-04T11:00:00Z",
  "idempotencyKey": "idem-transfer-001"
}
```
| **Expected** | HTTP 200. `success: true`. |
| **Result** | |

### TC-INV-07 — Inventory Adjustment (Waste)
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-4, FR-5.5 |
| **Precondition** | TC-INV-02 completed |
| **Steps** | 1. `POST /v1/inventory/adjust` with body: |
```json
{
  "tenantId": "tenant1",
  "itemId": "ITEM-FLOUR",
  "adjustmentQty": -5,
  "reasonCode": "WASTE",
  "notes": "Test waste adjustment"
}
```
| **Expected** | HTTP 200. Response confirms adjustment of -5 with reason WASTE. |
| **Result** | |

### TC-INV-08 — Stock Alerts
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-2, FR-5.7 |
| **Precondition** | Some items have stock below their `minStockThreshold` (e.g., Eggs threshold=100 but 0 received) |
| **Steps** | 1. `GET /v1/inventory/alerts?tenantId=tenant1` |
| **Expected** | HTTP 200. Array of alerts showing items with stock below threshold (at minimum ITEM-EGGS, ITEM-MILK, ITEM-SALT). |
| **Result** | |

### TC-INV-09 — Warehouse User Access
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-1 |
| **Precondition** | Login as warehouse |
| **Steps** | 1. `GET /v1/inventory/positions?tenantId=tenant1` with warehouse token |
| | 2. `POST /v1/inventory/receipts` (same body as TC-INV-02 but new idempotencyKey) with warehouse token |
| **Expected** | Both succeed (200). Warehouse has full inventory access. |
| **Result** | |

### TC-INV-10 — Cashier Cannot Access Inventory
| Field | Value |
|-------|-------|
| **Requirement** | NFR-G.5, FE-INV-1 |
| **Precondition** | Login as cashier |
| **Steps** | 1. `GET /v1/inventory/positions?tenantId=tenant1` with cashier token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

---

## Section 5: POS — Point of Sale (FE-POS)

### TC-POS-01 — Create CASH Sale
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-4, FR-8.1, FR-8.3 |
| **Precondition** | Logged in as cashier |
| **Steps** | 1. `POST /v1/pos/sales` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "paymentMethod": "CASH",
  "cashReceived": 50000,
  "lines": [
    {"productId":"PROD-WHITE","productName":"White Bread","quantity":3,"unit":"PCS","unitPrice":8000},
    {"productId":"PROD-CROISS","productName":"Croissant","quantity":2,"unit":"PCS","unitPrice":10000}
  ]
}
```
| **Expected** | HTTP 201. `saleId` populated, `status: "COMPLETED"`, `paymentMethod: "CASH"`, `totalAmount: 44000`, `changeGiven: 6000`, `cashierId: "cashier"` |
| **Result** | |
| **Notes** | Total = 3×8000 + 2×10000 = 44,000. Change = 50,000 − 44,000 = 6,000 |

### TC-POS-02 — Create CARD Sale
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-4, FR-8.3 |
| **Precondition** | Logged in as cashier |
| **Steps** | 1. `POST /v1/pos/sales` with body: |
```json
{
  "tenantId": "tenant1",
  "siteId": "MAIN",
  "paymentMethod": "CARD",
  "cardReference": "TXN-9876",
  "lines": [
    {"productId":"PROD-SOUR","productName":"Sourdough Bread","quantity":1,"unit":"PCS","unitPrice":15000}
  ]
}
```
| **Expected** | HTTP 201. `paymentMethod: "CARD"`, `totalAmount: 15000`, `changeGiven: 0`, `cardReference: "TXN-9876"` |
| **Result** | |

### TC-POS-03 — List Sales
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-5 |
| **Precondition** | TC-POS-01 and TC-POS-02 completed |
| **Steps** | 1. `GET /v1/pos/sales?tenantId=tenant1` with admin token |
| **Expected** | HTTP 200. Array with at least 2 sales records. |
| **Result** | |

### TC-POS-04 — End-of-Day Reconciliation
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-6, FR-8.6 |
| **Precondition** | Sales created in TC-POS-01 and TC-POS-02 |
| **Steps** | 1. `POST /v1/pos/reconcile` with body: `{"tenantId":"tenant1"}` as admin |
| **Expected** | HTTP 200. Body has: `totalTransactions`, `cashTotal`, `cardTotal`, `netSales`. Cash total ≈ 44000, Card total ≈ 15000. |
| **Result** | |

### TC-POS-05 — Warehouse Cannot Create Sale
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-1, NFR-G.5 |
| **Precondition** | Login as warehouse |
| **Steps** | 1. `POST /v1/pos/sales` (same as TC-POS-01) with warehouse token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

### TC-POS-06 — Empty Lines Rejected
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-4 |
| **Precondition** | Logged in as cashier |
| **Steps** | 1. `POST /v1/pos/sales` with body: `{"tenantId":"tenant1","paymentMethod":"CASH","lines":[]}` |
| **Expected** | HTTP 400 Bad Request |
| **Result** | |

---

## Section 6: Recipes & Products (FE-REC, FE-PROD)

### TC-REC-01 — Create Recipe
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-5, FR-4.1 |
| **Precondition** | Logged in as admin or technologist. Products seeded. |
| **Steps** | 1. `POST /v1/recipes` with body: |
```json
{
  "tenantId": "tenant1",
  "productId": "PROD-WHITE",
  "batchSize": 100,
  "batchSizeUom": "PCS",
  "expectedYield": 95,
  "yieldUom": "PCS",
  "productionNotes": "Standard white bread recipe",
  "ingredients": [
    {"itemId":"ITEM-FLOUR","itemName":"Wheat Flour","unitMode":"WEIGHT","recipeQty":50,"recipeUom":"KG","purchasingUnitSize":1,"purchasingUom":"KG","wasteFactor":0.02},
    {"itemId":"ITEM-YEAST","itemName":"Dry Yeast","unitMode":"WEIGHT","recipeQty":1,"recipeUom":"KG","purchasingUnitSize":1,"purchasingUom":"KG","wasteFactor":0.01},
    {"itemId":"ITEM-SALT","itemName":"Table Salt","unitMode":"WEIGHT","recipeQty":1,"recipeUom":"KG","purchasingUnitSize":1,"purchasingUom":"KG","wasteFactor":0}
  ]
}
```
| **Expected** | HTTP 201. `recipeId` set, `version: 1`, 3 ingredients listed. |
| **Result** | |
| **Notes** | Save `recipeId` for subsequent tests. |

### TC-REC-02 — List Recipe Versions
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-2, FR-4.6 |
| **Precondition** | TC-REC-01 completed |
| **Steps** | 1. `GET /v1/recipes?tenantId=tenant1&productId=PROD-WHITE` |
| **Expected** | HTTP 200. Array with 1 recipe (version 1). |
| **Result** | |

### TC-REC-03 — Activate Recipe
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-6, FR-4.6, FR-4.7 |
| **Precondition** | TC-REC-01 completed |
| **Steps** | 1. `POST /v1/recipes/{recipeId}/activate?tenantId=tenant1` |
| **Expected** | HTTP 200. Recipe `active: true` (or status changes to ACTIVE). Product's `activeRecipeId` now points to this recipe. |
| **Result** | |

### TC-REC-04 — Add Technology Step
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-7, FR-4.1 |
| **Precondition** | TC-REC-01 completed. Have `recipeId`. |
| **Steps** | 1. `POST /v1/technology-steps?tenantId=tenant1` with body: |
```json
{
  "recipeId": "<recipeId>",
  "stepNumber": 1,
  "name": "Mix Dough",
  "activities": "Combine flour, yeast, salt with water. Mix for 10 minutes.",
  "instruments": "Industrial mixer",
  "durationMinutes": 10,
  "temperatureCelsius": 25
}
```
| | 2. Add step 2: `{"recipeId":"<id>","stepNumber":2,"name":"First Rise","activities":"Let dough rise for 60 min","durationMinutes":60,"temperatureCelsius":28}` |
| | 3. Add step 3: `{"recipeId":"<id>","stepNumber":3,"name":"Bake","activities":"Bake in oven","instruments":"Industrial oven","durationMinutes":35,"temperatureCelsius":220}` |
| **Expected** | Each returns HTTP 201. Steps have IDs and matching properties. |
| **Result** | |

### TC-REC-05 — List Technology Steps
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-4, FE-REC-3 |
| **Precondition** | TC-REC-04 completed |
| **Steps** | 1. `GET /v1/technology-steps?tenantId=tenant1&recipeId=<recipeId>` |
| **Expected** | HTTP 200. Array of 3 steps, ordered by stepNumber (1, 2, 3). |
| **Result** | |

### TC-REC-06 — Only Technologist/Admin Can Create Recipes
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-1, FR-4.9 |
| **Precondition** | Login as cashier |
| **Steps** | 1. `POST /v1/recipes` (same body as TC-REC-01) with cashier token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

### TC-PROD-01 — List Products
| Field | Value |
|-------|-------|
| **Requirement** | FE-PROD-2 |
| **Precondition** | Products seeded |
| **Steps** | 1. `GET /v1/products?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 5 products (White Bread, Sourdough, Baguette, Croissant, Birthday Cake). |
| **Result** | |

### TC-PROD-02 — Create Product
| Field | Value |
|-------|-------|
| **Requirement** | FE-PROD-3, FR-4.8 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/products` with body: |
```json
{
  "tenantId": "tenant1",
  "departmentId": "DEPT-PASTRY",
  "name": "Cinnamon Roll",
  "saleUnit": "PIECE",
  "baseUom": "PCS",
  "price": 7500,
  "vatRatePct": 12
}
```
| **Expected** | HTTP 201. `productId` set, `name: "Cinnamon Roll"`, `status: "ACTIVE"`. |
| **Result** | |

---

## Section 7: Departments (FE-DEPT)

### TC-DEPT-01 — List Departments
| Field | Value |
|-------|-------|
| **Requirement** | FE-DEPT-2, FR-11.1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `GET /v1/departments?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 3 departments (Bread, Pastry, Confectionery). |
| **Result** | |

### TC-DEPT-02 — Create Department
| Field | Value |
|-------|-------|
| **Requirement** | FE-DEPT-3, FR-11.1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/departments` with body: `{"tenantId":"tenant1","name":"Prepared Foods","leadTimeHours":4,"warehouseMode":"SHARED"}` |
| **Expected** | HTTP 201. New department created. |
| **Result** | |

### TC-DEPT-03 — Non-Admin Cannot Create Department
| Field | Value |
|-------|-------|
| **Requirement** | FE-DEPT-1 |
| **Precondition** | Login as cashier |
| **Steps** | 1. `POST /v1/departments` (same body) with cashier token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

---

## Section 8: Reports (FE-RPT)

### TC-RPT-01 — Revenue Summary
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-2, FR-10.1, FE-DASH-2 |
| **Precondition** | POS sales created (Section 5). Login as admin. |
| **Steps** | 1. `GET /v1/reports/revenue-summary?tenantId=tenant1` |
| **Expected** | HTTP 200. Body has `today`, `week`, `month`, `allTime` fields with revenue values. `currency: "UZS"`. |
| **Result** | |

### TC-RPT-02 — Top Products
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-2, FE-DASH-2 |
| **Precondition** | POS sales created. Login as admin. |
| **Steps** | 1. `GET /v1/reports/top-products?tenantId=tenant1&limit=5` |
| **Expected** | HTTP 200. Array of products ranked by revenue/quantity. |
| **Result** | |

### TC-RPT-03 — Production Summary
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-2 |
| **Precondition** | Plans created (Section 3). Login as admin. |
| **Steps** | 1. `GET /v1/reports/production-summary?tenantId=tenant1` |
| **Expected** | HTTP 200. Has `totalPlans`, `completed`, `inProgress`, `draft`, `completionRate`. |
| **Result** | |

### TC-RPT-04 — Orders Summary
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-2 |
| **Precondition** | Orders created (Section 2). Login as admin. |
| **Steps** | 1. `GET /v1/reports/orders-summary?tenantId=tenant1` |
| **Expected** | HTTP 200. Has `todayCount`, `todayValue`, `byStatus`, `total`. |
| **Result** | |

### TC-RPT-05 — Finance Role Access
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-1 |
| **Precondition** | Login as finance |
| **Steps** | 1. `GET /v1/reports/revenue-summary?tenantId=tenant1` with finance token |
| | 2. `GET /v1/reports/top-products?tenantId=tenant1` with finance token |
| **Expected** | Both return HTTP 200. Finance has read access. |
| **Result** | |

### TC-RPT-06 — Cashier Cannot Access Reports
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-1, NFR-G.5 |
| **Precondition** | Login as cashier |
| **Steps** | 1. `GET /v1/reports/revenue-summary?tenantId=tenant1` with cashier token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

---

## Section 9: User Management (FE-ADMIN)

### TC-USER-01 — List Users
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3, FR-11.4 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `GET /v1/users?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 7 seeded users. Password hashes should be masked/hidden. |
| **Result** | |

### TC-USER-02 — Create New User
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3, FR-11.4 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/users` with body: |
```json
{
  "tenantId": "tenant1",
  "username": "newcashier",
  "password": "SecurePass1!",
  "displayName": "New Cashier",
  "roles": "Cashier",
  "departmentId": "DEPT-BREAD"
}
```
| **Expected** | HTTP 201. New user created with `active: true`. |
| **Result** | |

### TC-USER-03 — New User Can Login
| Field | Value |
|-------|-------|
| **Requirement** | FR-11.4 |
| **Precondition** | TC-USER-02 completed |
| **Steps** | 1. `POST /v1/auth/login` with `{"username":"newcashier","password":"SecurePass1!"}` |
| **Expected** | HTTP 200. Token received, `roles: ["Cashier"]`. |
| **Result** | |

### TC-USER-04 — Update User Roles
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3 |
| **Precondition** | TC-USER-02 completed. Get the new user's `userId`. |
| **Steps** | 1. `PUT /v1/users/{userId}?tenantId=tenant1` with body: `{"roles":"Cashier,FinanceUser"}` |
| **Expected** | HTTP 200. Roles updated. |
| **Result** | |

### TC-USER-05 — Deactivate User
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3, FR-11.4 |
| **Precondition** | TC-USER-02 completed |
| **Steps** | 1. `PUT /v1/users/{userId}?tenantId=tenant1` with body: `{"active":false}` |
| | 2. Try logging in as `newcashier` |
| **Expected** | Step 1: 200, user deactivated. Step 2: 401 "Account is deactivated". |
| **Result** | |

### TC-USER-06 — Duplicate Username Rejected
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/users` with body: `{"tenantId":"tenant1","username":"admin","password":"Test123!","roles":"Cashier"}` |
| **Expected** | HTTP 409 Conflict (duplicate username). |
| **Result** | |

### TC-USER-07 — Non-Admin Cannot Manage Users
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-1, NFR-G.5 |
| **Precondition** | Login as manager |
| **Steps** | 1. `GET /v1/users?tenantId=tenant1` with manager token |
| | 2. `POST /v1/users` (any body) with manager token |
| **Expected** | Both return HTTP 403 Forbidden. |
| **Result** | |

### TC-USER-08 — Reset Password
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3 |
| **Precondition** | TC-USER-02 completed. Have userId. |
| **Steps** | 1. `POST /v1/users/{userId}/reset-password?tenantId=tenant1` with body: `{"newPassword":"NewPass2!"}` |
| | 2. Login as `newcashier` with old password → should fail |
| | 3. Login as `newcashier` with `NewPass2!` → should succeed |
| **Expected** | Step 1: 200. Step 2: 401. Step 3: 200. |
| **Result** | |

---

## Section 10: Configuration (FE-ADMIN-4)

### TC-CFG-01 — Get Tenant Config
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-4 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `GET /v1/config?tenantId=tenant1` |
| **Expected** | HTTP 200. `displayName: "BreadCost Demo Bakery"`, `orderCutoffTime: "22:00"`, `rushOrderPremiumPct: 15`, `mainCurrency: "UZS"`. |
| **Result** | |

### TC-CFG-02 — Update Tenant Config
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-4, FR-11.5 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `PUT /v1/config?tenantId=tenant1` with body: `{"orderCutoffTime":"21:00","rushOrderPremiumPct":20}` |
| | 2. `GET /v1/config?tenantId=tenant1` |
| **Expected** | Step 1: 200. Step 2: `orderCutoffTime: "21:00"`, `rushOrderPremiumPct: 20`. |
| **Result** | |

### TC-CFG-03 — Non-Admin Cannot Update Config
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-1 |
| **Precondition** | Login as finance |
| **Steps** | 1. `PUT /v1/config?tenantId=tenant1` with body: `{"mainCurrency":"USD"}` with finance token |
| **Expected** | HTTP 403 Forbidden |
| **Result** | |

---

## Section 11: Items (Raw Materials)

### TC-ITEM-01 — List Items
| Field | Value |
|-------|-------|
| **Requirement** | FR-5.1 |
| **Precondition** | Items seeded |
| **Steps** | 1. `GET /v1/items?tenantId=tenant1` |
| **Expected** | HTTP 200. Array with 7 items (Flour, Sugar, Butter, Yeast, Salt, Eggs, Milk). |
| **Result** | |

### TC-ITEM-02 — Create Item
| Field | Value |
|-------|-------|
| **Requirement** | FR-5.1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. `POST /v1/items?tenantId=tenant1` with body: `{"name":"Vanilla Extract","type":"INGREDIENT","baseUom":"L","minStockThreshold":2}` |
| **Expected** | HTTP 201. Item created. |
| **Result** | |

---

## Section 12: Role Access Matrix (Cross-Cutting)

The following table summarizes expected access per role for key endpoints. Each cell = expected HTTP status.

| Endpoint | admin | manager | technologist | production | finance | warehouse | cashier |
|----------|-------|---------|-------------|-----------|---------|-----------|---------|
| `GET /v1/orders` | 200 | **403** | **403** | 200 | 200 | **403** | **403** |
| `POST /v1/orders` | 200 | **403** | **403** | 200 | **403** | **403** | **403** |
| `GET /v1/production-plans` | 200 | **403** | **403** | 200 | 200 | **403** | **403** |
| `GET /v1/inventory/positions` | 200 | **403** | **403** | 200 | 200 | 200 | **403** |
| `POST /v1/inventory/receipts` | 200 | **403** | **403** | **403** | **403** | 200 | **403** |
| `POST /v1/pos/sales` | 200 | **403** | **403** | **403** | **403** | **403** | 200 |
| `GET /v1/reports/revenue-summary` | 200 | **403** | **403** | **403** | 200 | **403** | **403** |
| `GET /v1/users` | 200 | **403** | **403** | **403** | **403** | **403** | **403** |
| `POST /v1/recipes` | 200 | **403** | 200 | **403** | **403** | **403** | **403** |
| `POST /v1/departments` | 200 | **403** | **403** | **403** | **403** | **403** | **403** |

> **Note:** "Manager" role in the BE is not included in most `@PreAuthorize` lists — the BE uses `Admin`, `ProductionUser`, `FinanceUser`, `Viewer`, `Warehouse`, `Cashier`, `Technologist`. The FE spec's "Management" role doesn't map directly to any BE role. Tests expect 403 for manager on most endpoints.

### TC-ROLE-01 — Validate Full Access Matrix
| Field | Value |
|-------|-------|
| **Requirement** | FE-SHELL-2, NFR-FE-12, NFR-FE-13, NFR-G.5 |
| **Precondition** | Login as each of the 7 users, save all tokens |
| **Steps** | For each row in the matrix above, call the endpoint with each user's token and verify the expected status code. |
| **Expected** | All cells match the table above. |
| **Result** | |

---

## Execution Checklist

| Section | Tests | Passed | Failed | Notes |
|---------|-------|--------|--------|-------|
| 1. Authentication | 6 | | | |
| 2. Orders | 10 | | | |
| 3. Production Plans | 10 | | | |
| 4. Inventory | 10 | | | |
| 5. POS | 6 | | | |
| 6. Recipes & Products | 8 | | | |
| 7. Departments | 3 | | | |
| 8. Reports | 6 | | | |
| 9. User Management | 8 | | | |
| 10. Configuration | 3 | | | |
| 11. Items | 2 | | | |
| 12. Role Access Matrix | 1 (covers ~70 checks) | | | |
| **TOTAL** | **73** | | | |

---

## Suggested Test Execution Order

Run sections in this order to build up data progressively:

1. **Authentication** (TC-AUTH-01 to 06) — get tokens
2. **Configuration** (TC-CFG-01 to 03) — verify tenant setup
3. **Items** (TC-ITEM-01 to 02) — verify raw materials
4. **Departments** (TC-DEPT-01 to 03) — verify departments
5. **Products** (TC-PROD-01 to 02) — verify products
6. **Recipes** (TC-REC-01 to 06) — create recipes with ingredients and steps
7. **Inventory** (TC-INV-01 to 10) — receive stock
8. **Orders** (TC-ORD-01 to 10) — create and manage orders
9. **Production Plans** (TC-PP-01 to 10) — create plans, generate WOs
10. **POS** (TC-POS-01 to 06) — process sales
11. **Reports** (TC-RPT-01 to 06) — verify data aggregation
12. **User Management** (TC-USER-01 to 08) — manage users
13. **Role Access Matrix** (TC-ROLE-01) — final cross-cutting verification

---
*End of Manual Test Plan v1.0*
