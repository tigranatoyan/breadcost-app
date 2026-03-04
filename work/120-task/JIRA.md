# BreadCost — JIRA Project Structure
**Project Key:** BC | **Date:** 2026-03-04 | **Methodology:** Scrum

---

## Definition of Done (DoD) — All Items

### Story DoD
- [ ] Code implements all acceptance criteria listed on the story
- [ ] Unit tests written and passing (≥ 80% branch coverage on new code)
- [ ] Functional/integration test added to the automated test suite
- [ ] API returns correct HTTP status codes (200/201/400/401/403/409/500)
- [ ] Role-based access enforced server-side via `@PreAuthorize`
- [ ] Tenant isolation enforced (`tenantId` required and filtered on all queries)
- [ ] No compilation warnings or lint errors
- [ ] Code reviewed by at least 1 team member (or AI review documented)
- [ ] Corresponding manual test cases documented in `MANUAL_TEST_PLAN.md`
- [ ] Swagger/API documentation updated (if endpoint changed)

### Epic DoD
- [ ] All stories in the epic are Done
- [ ] All acceptance criteria from the requirements doc verified
- [ ] End-to-end scenario tested manually using `MANUAL_TEST_PLAN.md`
- [ ] No open critical/blocker bugs against the epic

### Release DoD
- [ ] All epics in the release are Done
- [ ] Full regression suite passing (103+ automated tests)
- [ ] Manual test plan executed: all sections pass
- [ ] Role access matrix (Section 12 of MANUAL_TEST_PLAN) fully validated
- [ ] Performance: primary operations respond < 2s at p95
- [ ] No known critical or blocker bugs
- [ ] Release notes documented
- [ ] Git tag created on `main`

---

## Release 1 — Core MVP

### Acceptance Criteria (Release Level)

| # | Criterion | FRs | Verified By |
|---|-----------|-----|-------------|
| R1-AC-01 | Admin can log in with username/password and receive a JWT token | FR-11.4, NFR-G.4 | TC-AUTH-01 |
| R1-AC-02 | All 7 roles (Admin, Manager, Technologist, ProductionUser, FinanceUser, Warehouse, Cashier) can log in | FR-11.4 | TC-AUTH-06 |
| R1-AC-03 | Unauthenticated requests return 401 | NFR-G.4 | TC-AUTH-05 |
| R1-AC-04 | Orders can be created as DRAFT and confirmed/cancelled following the state machine | FR-1.1, FR-1.16, FR-1.17 | TC-ORD-02..08 |
| R1-AC-05 | Production plans can be created, work orders generated, plan approved and completed | FR-3.1..3.10 | TC-PP-01..07 |
| R1-AC-06 | Recipes can be created with ingredients and versioned; technology steps can be added | FR-4.1..4.9 | TC-REC-01..05 |
| R1-AC-07 | Raw materials can be received with lot tracking and FIFO cost | FR-5.1, FR-5.2, FR-5.6 | TC-INV-02..03 |
| R1-AC-08 | Inventory can be transferred between locations and adjusted with reason codes | FR-5.4, FR-5.5 | TC-INV-06, TC-INV-07 |
| R1-AC-09 | Stock alerts are generated for items below minimum threshold | FR-5.7 | TC-INV-08 |
| R1-AC-10 | POS sales can be processed (CASH and CARD) with correct total and change calculation | FR-8.1..8.3 | TC-POS-01..02 |
| R1-AC-11 | End-of-day POS reconciliation returns cash/card totals | FR-8.6 | TC-POS-04 |
| R1-AC-12 | Revenue summary, top products, production summary, and order summary reports available | FR-10.1, FR-10.4 | TC-RPT-01..04 |
| R1-AC-13 | Admin can create, update, deactivate users and reset passwords | FR-11.4 | TC-USER-01..08 |
| R1-AC-14 | Tenant configuration (cutoff time, rush premium, currency) is editable by admin | FR-11.5 | TC-CFG-01..02 |
| R1-AC-15 | Departments can be created and configured (lead time, warehouse mode) | FR-11.1 | TC-DEPT-01..02 |
| R1-AC-16 | Role-based access is enforced at the API layer for all endpoints | NFR-G.5 | TC-ROLE-01 |
| R1-AC-17 | Commands are idempotent — replaying same idempotency key does not duplicate data | NFR-G.11 | TC-INV-05 |
| R1-AC-18 | Event store records all domain state changes | NFR-G.10 | H2 console verification |

---

## Epics & Stories

---

### Epic: BC-E01 — Authentication & Authorization
**Goal:** Secure login, JWT token management, and role enforcement across all endpoints.  
**Sprint:** 1 | **Requirements:** FR-11.3, FR-11.4, NFR-G.4, NFR-G.5, NFR-G.6

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-101 | **User login with JWT** | 1. `POST /v1/auth/login` accepts `{username, password}` 2. Returns JWT token, username, roles, tenantId 3. Invalid credentials return 401 4. Blank fields return 400 | P0 | ✅ Done |
| BC-102 | **JWT authentication filter** | 1. All `/v1/**` endpoints require Bearer token 2. Invalid/expired token returns 401 3. Token contains username, roles, tenantId claims | P0 | ✅ Done |
| BC-103 | **Role-based method security** | 1. `@PreAuthorize` on every controller method 2. Unauthorized role returns 403 3. Matches role access matrix in MANUAL_TEST_PLAN.md Section 12 | P0 | ✅ Done |
| BC-104 | **Password hashing with BCrypt** | 1. Passwords stored as BCrypt hashes 2. Raw passwords never stored or logged | P0 | ✅ Done |
| BC-105 | **Deactivated user cannot login** | 1. User with `active=false` receives 401 "Account is deactivated" | P1 | ✅ Done |

---

### Epic: BC-E02 — Order Management
**Goal:** Full order lifecycle from creation through confirmation, cancellation, and status tracking.  
**Sprint:** 1-2 | **Requirements:** FR-1.1, FR-1.6–FR-1.17

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-201 | **Create DRAFT order** | 1. `POST /v1/orders` creates order with status=DRAFT 2. Requires tenantId, at least 1 line 3. Returns 201 with orderId 4. Only Admin,ProductionUser can create | P0 | ✅ Done |
| BC-202 | **List orders with filters** | 1. `GET /v1/orders?tenantId=&status=` returns array 2. Filters by status, customerId 3. Empty filters return all 4. Role-restricted | P0 | ✅ Done |
| BC-203 | **Get order by ID** | 1. `GET /v1/orders/{id}?tenantId=` returns full order with lines 2. 404 if not found | P1 | ✅ Done |
| BC-204 | **Confirm order (DRAFT→CONFIRMED)** | 1. `POST /v1/orders/{id}/confirm` transitions DRAFT→CONFIRMED 2. Sets confirmedAt timestamp 3. Emits OrderConfirmedEvent 4. Rejects non-DRAFT orders with 409 | P0 | ✅ Done |
| BC-205 | **Cancel order (DRAFT/CONFIRMED→CANCELLED)** | 1. `POST /v1/orders/{id}/cancel?reason=` transitions to CANCELLED 2. Requires reason parameter 3. Only cancellable from DRAFT or CONFIRMED 4. Emits OrderCancelledEvent | P0 | ✅ Done |
| BC-206 | **Status transition enforcement** | 1. Invalid transitions (e.g., CANCELLED→CONFIRMED) return 409 2. Double-confirm returns 409 | P0 | ✅ Done |
| BC-207 | **Rush order support** | 1. `forceRush=true` creates rush order 2. Custom premium % can be set via `customRushPremiumPct` | P2 | ✅ Done |
| BC-208 | **Order status advance** | 1. `POST /v1/orders/{id}/status?targetStatus=` allows advancing (IN_PRODUCTION, READY, DELIVERED) | P1 | ✅ Done |

---

### Epic: BC-E03 — Production Planning
**Goal:** Plan creation, work order generation from confirmed orders, plan approval, and execution tracking.  
**Sprint:** 2-3 | **Requirements:** FR-3.1–FR-3.10

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-301 | **Create production plan** | 1. `POST /v1/production-plans` with date, shift, tenantId 2. Returns 201 with planId, status=DRAFT | P0 | ✅ Done |
| BC-302 | **Generate work orders** | 1. `POST /plans/{id}/generate` creates WOs from confirmed orders 2. Plan status → GENERATED 3. Each WO has product, target qty, department | P0 | ✅ Done |
| BC-303 | **Approve plan** | 1. `POST /plans/{id}/approve` transitions GENERATED→APPROVED 2. Only Admin,ProductionUser | P0 | ✅ Done |
| BC-304 | **Start/complete plan lifecycle** | 1. Start: APPROVED→IN_PROGRESS 2. Complete: IN_PROGRESS→COMPLETED | P1 | ✅ Done |
| BC-305 | **Work order lifecycle** | 1. Start WO: PLANNED→IN_PROGRESS 2. Complete WO: IN_PROGRESS→COMPLETED 3. Cancel WO: any→CANCELLED | P0 | ✅ Done |
| BC-306 | **Material requirements view** | 1. `GET /plans/{id}/material-requirements` returns ingredient needs based on recipes | P2 | ✅ Done |
| BC-307 | **Plan schedule and WO scheduling** | 1. `GET /plans/{id}/schedule` returns timeline 2. `PATCH /work-orders/{id}/schedule` adjusts start/duration | P2 | ✅ Done |
| BC-308 | **List work orders by department** | 1. `GET /work-orders?tenantId=&departmentId=` returns filtered WOs | P1 | ✅ Done |

---

### Epic: BC-E04 — Recipe & Product Management
**Goal:** Recipe CRUD with versioning, ingredient management, technology steps, and product catalog.  
**Sprint:** 2 | **Requirements:** FR-4.1–FR-4.9

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-401 | **Create recipe with ingredients** | 1. `POST /v1/recipes` with ingredients array 2. Returns 201 with version number 3. Only Admin,Technologist | P0 | ✅ Done |
| BC-402 | **Recipe versioning** | 1. Multiple recipes per product, each with version number 2. `GET /v1/recipes?productId=` returns version history | P1 | ✅ Done |
| BC-403 | **Activate recipe** | 1. `POST /recipes/{id}/activate` sets as active for product 2. Archives previous active version | P0 | ✅ Done |
| BC-404 | **Get active recipe** | 1. `GET /v1/recipes/active?productId=` returns current active recipe | P1 | ✅ Done |
| BC-405 | **Update recipe ingredients** | 1. `PUT /recipes/{id}/ingredients` replaces ingredient list | P1 | ✅ Done |
| BC-406 | **Material requirements calculation** | 1. `GET /recipes/{id}/material-requirements?batchMultiplier=` returns per-ingredient quantities needed | P2 | ✅ Done |
| BC-407 | **Technology steps CRUD** | 1. Create step: `POST /v1/technology-steps` 2. Edit: `PUT /technology-steps/{id}` 3. Delete: `DELETE /technology-steps/{id}` 4. List: `GET /technology-steps?recipeId=` ordered by stepNumber | P1 | ✅ Done |
| BC-408 | **Product CRUD** | 1. List: `GET /v1/products` 2. Create: `POST /v1/products` (201) 3. Update: `PUT /products/{id}` 4. Sale unit: PIECE/WEIGHT/BOTH | P0 | ✅ Done |

---

### Epic: BC-E05 — Inventory & Warehouse Management
**Goal:** Lot-based receiving with FIFO cost tracking, transfers, adjustments, and stock alerts.  
**Sprint:** 2-3 | **Requirements:** FR-5.1–FR-5.11

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-501 | **Receive lot (stock receipt)** | 1. `POST /v1/inventory/receipts` records lot with cost 2. Emits ReceiveLotEvent 3. Idempotent via idempotencyKey 4. Only Admin,Warehouse | P0 | ✅ Done |
| BC-502 | **View inventory positions** | 1. `GET /v1/inventory/positions` returns on-hand qty per item per site 2. Filter by siteId | P0 | ✅ Done |
| BC-503 | **Inventory transfer** | 1. `POST /v1/inventory/transfers` moves qty between locations 2. Emits TransferInventoryEvent 3. Idempotent | P1 | ✅ Done |
| BC-504 | **Inventory adjustment** | 1. `POST /v1/inventory/adjust` with reason code (WASTE/SPOILAGE/COUNT_CORRECTION/OTHER) 2. Positive or negative qty | P1 | ✅ Done |
| BC-505 | **Issue to batch (material consumption)** | 1. `POST /v1/batches/{id}/issues` issues material to production batch 2. FIFO lot selection 3. Emits IssueToBatchEvent | P0 | ✅ Done |
| BC-506 | **Stock alerts** | 1. `GET /v1/inventory/alerts` returns items below minStockThreshold 2. Each alert: itemId, onHandQty, minThreshold, severity | P1 | ✅ Done |
| BC-507 | **Idempotency enforcement** | 1. Resending receipt/transfer with same idempotencyKey does not duplicate 2. Returns success without re-processing | P0 | ✅ Done |

---

### Epic: BC-E06 — Point of Sale (POS)
**Goal:** Walk-in B2C sales with cash/card payment, receipt data, and end-of-day reconciliation.  
**Sprint:** 3 | **Requirements:** FR-8.1–FR-8.6

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-601 | **Create POS sale (CASH)** | 1. `POST /v1/pos/sales` with paymentMethod=CASH 2. Calculates totalAmount, changeGiven 3. Returns 201 with saleId 4. Status=COMPLETED 5. Records cashierId from JWT | P0 | ✅ Done |
| BC-602 | **Create POS sale (CARD)** | 1. paymentMethod=CARD, cardReference required 2. changeGiven=0 3. Returns 201 | P0 | ✅ Done |
| BC-603 | **List sales** | 1. `GET /v1/pos/sales?tenantId=` returns sale history 2. Filter by date | P1 | ✅ Done |
| BC-604 | **End-of-day reconciliation** | 1. `POST /v1/pos/reconcile` returns cash/card totals, refunds, net sales 2. Grouped by date | P1 | ✅ Done |
| BC-605 | **POS validation** | 1. Empty lines → 400 2. Missing payment method → 400 | P1 | ✅ Done |
| BC-606 | **POS role enforcement** | 1. Only Admin,Cashier can create sales 2. Warehouse, Finance, Production → 403 | P0 | ✅ Done |

---

### Epic: BC-E07 — Reporting & Dashboard
**Goal:** Revenue summary, top products, production summary, and order summary for management decision-making.  
**Sprint:** 3 | **Requirements:** FR-10.1, FR-10.4–FR-10.7

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-701 | **Revenue summary report** | 1. `GET /reports/revenue-summary` returns today/week/month/allTime revenue 2. Includes currency 3. Admin,FinanceUser,Viewer | P0 | ✅ Done |
| BC-702 | **Top products report** | 1. `GET /reports/top-products?limit=N` returns ranked products 2. Each: productName, totalQty, totalRevenue | P1 | ✅ Done |
| BC-703 | **Production summary report** | 1. `GET /reports/production-summary` returns plan counts by status, WO completion rate 2. Date range filter | P1 | ✅ Done |
| BC-704 | **Orders summary report** | 1. `GET /reports/orders-summary` returns today count, value, breakdown by status | P1 | ✅ Done |
| BC-705 | **Report role enforcement** | 1. Admin, FinanceUser, Viewer can access 2. Cashier → 403 | P0 | ✅ Done |

---

### Epic: BC-E08 — User Management & Configuration
**Goal:** Admin can manage staff accounts, roles, and tenant-wide settings.  
**Sprint:** 3 | **Requirements:** FR-11.1–FR-11.9

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-801 | **List users** | 1. `GET /v1/users?tenantId=` returns user list 2. Password hashes masked 3. Admin only | P0 | ✅ Done |
| BC-802 | **Create user** | 1. `POST /v1/users` with username, password, roles 2. Returns 201 3. New user can log in 4. Duplicate username → 409 | P0 | ✅ Done |
| BC-803 | **Update user roles** | 1. `PUT /v1/users/{id}` updates roles, displayName, department 2. Changes take effect on next login | P1 | ✅ Done |
| BC-804 | **Deactivate user** | 1. `PUT /v1/users/{id}` with `active:false` 2. Deactivated user cannot log in (401) | P1 | ✅ Done |
| BC-805 | **Reset password** | 1. `POST /v1/users/{id}/reset-password` 2. Old password stops working, new one works | P1 | ✅ Done |
| BC-806 | **Department CRUD** | 1. Create: `POST /v1/departments` (201) 2. List: `GET /v1/departments` 3. Update: `PUT /departments/{id}` 4. Admin only | P1 | ✅ Done |
| BC-807 | **Tenant config management** | 1. `GET /v1/config` returns cutoff time, rush premium, currency 2. `PUT /v1/config` updates settings 3. Admin only for writes | P1 | ✅ Done |

---

### Epic: BC-E09 — Event Sourcing & Infrastructure
**Goal:** All domain state changes recorded as immutable events with idempotency and CQRS projections.  
**Sprint:** 1 | **Requirements:** NFR-G.10, NFR-G.11

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-901 | **Event store implementation** | 1. Append-only event store 2. Events: ReceiveLot, IssueToBatch, TransferInventory, OrderCreated/Confirmed/Cancelled, CloseBatch, etc. 3. Each event has idempotencyKey, occurredAtUtc, tenantId | P0 | ✅ Done |
| BC-902 | **Idempotency service** | 1. Duplicate idempotencyKey detected and rejected 2. Command handler returns success without re-executing | P0 | ✅ Done |
| BC-903 | **Inventory projection** | 1. Reads events to build on-hand positions 2. FIFO cost layers maintained per lot | P0 | ✅ Done |
| BC-904 | **Global exception handler** | 1. IllegalArgumentException → 400 2. AccessDeniedException → 403 3. IllegalStateException → 409 4. DataIntegrityViolationException → 409 5. Unhandled → 500 | P0 | ✅ Done |

---

### Epic: BC-E10 — Items & Raw Material Catalog
**Goal:** Maintain a catalog of raw materials with units and stock thresholds.  
**Sprint:** 1 | **Requirements:** FR-5.1

| Story ID | Title | Acceptance Criteria | Priority | Status |
|----------|-------|-------------------|----------|--------|
| BC-1001 | **Item CRUD** | 1. Create: `POST /v1/items` (201) 2. List: `GET /v1/items?type=&activeOnly=` 3. Update: `PUT /items/{id}` 4. Fields: name, type, baseUom, minStockThreshold | P0 | ✅ Done |
| BC-1002 | **Item types** | 1. Supports INGREDIENT, PACKAGING, FG, BYPRODUCT, WIP 2. Filter by type query param | P1 | ✅ Done |

---

## Release 2 — Growth (Planned)

### Acceptance Criteria (Release Level)

| # | Criterion | FRs |
|---|-----------|-----|
| R2-AC-01 | Customers can register and log into a web portal | FR-2.1..2.3 |
| R2-AC-02 | Loyalty points are awarded per purchase and redeemable | FR-2.6, FR-2.9 |
| R2-AC-03 | Supplier PO suggestions auto-generated from stock thresholds | FR-6.2, FR-6.3 |
| R2-AC-04 | POs exportable to Excel | FR-6.4 |
| R2-AC-05 | Delivery runs and manifests can be managed | FR-7.1..FR-7.6 |
| R2-AC-06 | B2B invoices generated from delivered orders with payment terms | FR-9.8 |
| R2-AC-07 | Credit limits enforced — overdue customers blocked from ordering | FR-9.10 |
| R2-AC-08 | Report constructor allows assembling custom reports from KPI blocks | FR-10.2, FR-10.3 |

### Epics (Planned)

| Epic | Title | Key FRs | Sprint Target |
|------|-------|---------|---------------|
| BC-E11 | Customer Portal & Registration | FR-2.1..2.5 | R2-S1 |
| BC-E12 | Loyalty Program | FR-2.6..2.10 | R2-S2 |
| BC-E13 | Supplier Management & PO Workflow | FR-6.1..6.7 | R2-S1 |
| BC-E14 | Delivery Management | FR-7.1..7.6 | R2-S2 |
| BC-E15 | B2B Invoicing & Credit Control | FR-9.8..9.11 | R2-S3 |
| BC-E16 | Advanced Report Constructor | FR-10.2..10.4 | R2-S3 |
| BC-E17 | Subscription Tier Management | FR-11.6 | R2-S1 |

---

## Release 3 — AI + Mobile (Planned)

### Acceptance Criteria (Release Level)

| # | Criterion | FRs |
|---|-----------|-----|
| R3-AC-01 | WhatsApp AI bot processes orders end-to-end with confirmation conversation | FR-12.1, FR-12.2 |
| R3-AC-02 | AI replenishment hints displayed to purchasing manager | FR-12.3 |
| R3-AC-03 | AI demand forecasting available per product per period | FR-12.7 |
| R3-AC-04 | Driver mobile app with delivery tracking | FR-7.7 |
| R3-AC-05 | Exchange rates pulled from external API | FR-9.7 |
| R3-AC-06 | Mobile iOS/Android customer app available | FR-2.1 (mobile) |

### Epics (Planned)

| Epic | Title | Key FRs | Sprint Target |
|------|-------|---------|---------------|
| BC-E18 | AI WhatsApp Order Intake | FR-12.1, FR-12.2 | R3-S1 |
| BC-E19 | AI Replenishment & Forecasting | FR-12.3, FR-12.7 | R3-S2 |
| BC-E20 | AI Pricing & Anomaly Alerts | FR-12.5, FR-12.6 | R3-S3 |
| BC-E21 | Driver Mobile App | FR-7.7, FR-8.7, FR-8.8 | R3-S2 |
| BC-E22 | Exchange Rate & Supplier API | FR-6.7, FR-9.7 | R3-S1 |
| BC-E23 | Mobile Customer App | FR-2.1 (mobile) | R3-S3 |

---

## Story-to-Test Traceability Matrix

| Story | Manual Test Cases | Automated Test Class |
|-------|------------------|---------------------|
| BC-101 | TC-AUTH-01, 02, 03, 04 | `AuthFunctionalTest` |
| BC-102 | TC-AUTH-05 | `AuthFunctionalTest` |
| BC-103 | TC-ROLE-01 | `RoleAccessFunctionalTest` |
| BC-105 | TC-USER-05 (step 2) | `AuthFunctionalTest` |
| BC-201 | TC-ORD-02, 10 | `OrdersFunctionalTest` |
| BC-202 | TC-ORD-01, 03, 04, 09 | `OrdersFunctionalTest` |
| BC-204 | TC-ORD-05, 06 | `OrdersFunctionalTest` |
| BC-205 | TC-ORD-07, 08 | `OrdersFunctionalTest` |
| BC-301 | TC-PP-01, 02 | `ProductionPlanFunctionalTest` |
| BC-302 | TC-PP-03, 04 | `ProductionPlanFunctionalTest` |
| BC-303 | TC-PP-05, 09 | `ProductionPlanFunctionalTest` |
| BC-305 | TC-PP-08 | `ProductionPlanFunctionalTest` |
| BC-401 | TC-REC-01 | — |
| BC-403 | TC-REC-03 | — |
| BC-407 | TC-REC-04, 05 | — |
| BC-408 | TC-PROD-01, 02 | — |
| BC-501 | TC-INV-02, 04 | `InventoryFunctionalTest` |
| BC-502 | TC-INV-01, 03 | `InventoryFunctionalTest` |
| BC-503 | TC-INV-06 | `InventoryFunctionalTest` |
| BC-504 | TC-INV-07 | `InventoryFunctionalTest` |
| BC-506 | TC-INV-08 | `InventoryFunctionalTest` |
| BC-507 | TC-INV-05 | `InventoryFunctionalTest` |
| BC-601 | TC-POS-01 | `PosFunctionalTest` |
| BC-602 | TC-POS-02 | `PosFunctionalTest` |
| BC-604 | TC-POS-04 | `PosFunctionalTest` |
| BC-701 | TC-RPT-01 | `ReportsFunctionalTest` |
| BC-702 | TC-RPT-02 | `ReportsFunctionalTest` |
| BC-801 | TC-USER-01 | `UserManagementFunctionalTest` |
| BC-802 | TC-USER-02, 03, 06 | `UserManagementFunctionalTest` |
| BC-804 | TC-USER-05 | `UserManagementFunctionalTest` |
| BC-805 | TC-USER-08 | `UserManagementFunctionalTest` |
| BC-806 | TC-DEPT-01, 02, 03 | — |
| BC-807 | TC-CFG-01, 02, 03 | — |

---

## Known Spec Gaps (Tracked)

| ID | Description | Impact | Story |
|----|-------------|--------|-------|
| GAP-01 | FE spec Manager role doesn't map to any BE `@PreAuthorize` role | Manager user gets 403 on most endpoints | BC-103 |
| GAP-02 | FE-ORD-1 says floor workers can't see orders; BE allows ProductionUser | Floor worker gets 200, not 403 | BC-202 |
| GAP-03 | FE-PP-6 says only management can approve plans; BE allows ProductionUser | Floor worker can approve plans | BC-303 |
| GAP-04 | FE-INV-1 says Manager has read access to inventory; BE doesn't include Manager role | Manager gets 403 on inventory | BC-502 |

---

*End of JIRA Project Structure v1.0*
