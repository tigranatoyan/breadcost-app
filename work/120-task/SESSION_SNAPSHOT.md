# BreadCost App — Work Session Snapshot
**Last Updated:** 2026-03-04 (FE Requirements drafted — paused for review)
**Purpose:** Handoff context for continuing development in a new chat session

> **Frontend Requirements:** See `work/120-task/FE_REQUIREMENTS.md` — PENDING USER APPROVAL before implementation continues.
> **Frontend Spec:** See `work/120-task/FRONTEND_SPEC.md` — frozen implementation reference for what is already built.

---

## 🔄 IN PROGRESS — Active Development

### Completed today (resume point)
- `FE_REQUIREMENTS.md` drafted and accepted
- `/orders` page: **complete** — see Session 3 below

### Next recommended screens (in order)
1. `/inventory` — receive stock, view stock levels, adjust, transfer
2. `/admin` — user management (needed to create real accounts)
3. `/dashboard` — real data widgets
4. `/production-plans` — finish approve + WO transitions
5. `/pos` — POS sale screen
6. `/reports` — standard reports

---

## Session History — 2026-03-04

### Session 3 — Orders Screen Complete
**File:** `frontend/app/orders/page.tsx`

What was built / improved:
1. **Status filter dropdown** — filter by All / DRAFT / CONFIRMED / IN_PRODUCTION / READY / OUT_FOR_DELIVERY / DELIVERED / CANCELLED
2. **Customer search** — live filter by customer name
3. **Fixed `rushOrder` field** — was `isRushOrder` (wrong); corrected to match `OrderEntity` shape
4. **Rush order in create form** — checkbox to mark rush order + optional custom premium % field
5. **Full status advance flow** — buttons: CONFIRMED→▶ Start Production / IN_PROGRESS→✓ Mark Ready / READY→🚚 Out for Delivery / OUT_FOR_DELIVERY→✅ Mark Delivered
6. **Cancel with reason dialog** — modal with optional reason textarea before confirming cancel
7. **Optimistic list update** — actions update the single mutated order in state (no full reload)
8. **Order detail expand** — meta row (orderId, placed at, delivery, notes) + line table with totals row + lead time indicators
9. **Order count display** — "{N} orders" counter in filter bar

API calls:
- `GET /v1/orders?tenantId=` — list
- `POST /v1/orders?tenantId=` — create (with forceRush, customRushPremiumPct)
- `POST /v1/orders/{id}/confirm?tenantId=` — confirm
- `POST /v1/orders/{id}/cancel?tenantId=&reason=` — cancel with optional reason
- `POST /v1/orders/{id}/status?tenantId=&targetStatus=` — status advance

### Session 2 — FE Requirements Definition
- Generated `work/120-task/FE_REQUIREMENTS.md` — full frontend requirements document covering all 13 screens, 7 roles, 9 principles, FE NFRs, and R1 coverage analysis
- R1 FE completion assessed at **~30–35%**
- Key gaps identified: `/orders` (0%), `/inventory` (0%), `/pos` (0%), `/reports` (0%), `/admin` (~10%)

### Session 1 — Technology Steps + Floor Worker + Nav Fix
1. **TechnologyStep backend** — 4 new files: `TechnologyStepEntity`, `TechnologyStepRepository`, `TechnologyStepService`, `TechnologyStepController`
   - CRUD endpoints at `/v1/technology-steps` (GET by recipeId, POST, PUT, DELETE)
2. **recipes/page.tsx** — Technology Steps tab in recipe expand panel. Add/edit/delete steps per recipe.
3. **floor/page.tsx** — Full rewrite. Shift plan cards → click → WOPanel (tech steps with localStorage checkboxes + recipe ingredients). WO actions: Start / Complete / Cancel.
4. **AuthShell.tsx** — Floor workers see "My Shift" first, auto-redirect /dashboard → /floor, spinner on load.
5. **ProductionPlanService** — generateWorkOrders falls back to all confirmed orders if none match by date.

### Current build state
- Backend: rebuilt ✅ JAR at `target/breadcost-app-1.0.0-SNAPSHOT.jar`, runs on :8080
- Frontend: Next.js dev server on :3000, no TypeScript errors in modified files
- To restart backend: `mvn clean package -DskipTests` then `java -jar target\breadcost-app-1.0.0-SNAPSHOT.jar`



---

## 1. What This Project Is

A Spring Boot (Java 25 / JDK 25) event-sourced CQRS application for managing a bread factory.
- Multi-tenant SaaS
- Pattern: Command → Handler → Event → EventStore → Projection (read model)
- Database: H2 in-memory (dev). PostgreSQL planned for production.
- Auth: Spring Security basic auth (admin/admin) — real user management is a future task
- Build: Maven 3.9 with Lombok edge-SNAPSHOT (required for Java 25 compatibility)

**Workspace:** `C:\workspace\breadcost-app`  
**Run command:** `mvn clean package -DskipTests` then `java -jar target\breadcost-app-1.0.0-SNAPSHOT.jar`  
**Key config:** `.mvn/jvm.config` has `--add-opens` flags for Lombok + Java 25  

---

## 2. Requirements Documents (already written)

All in `work/120-task/`:

| File | Contents |
|---|---|
| `REQUIREMENTS.md` | Full FR + NFR for all 12 domains + Release mapping (R1/R2/R3) |
| `ARCHITECTURE_REVIEW.md` | Existing codebase analysis vs R1 requirements — what exists, what's partially built, what's missing |

---

## 3. Completed Work

### R1 Domain 4 — Recipe & Product Management ✅ DONE
**New packages:**
- `com.breadcost.domain` — added `Department`, `Product`, `Recipe`, `RecipeIngredient`
- `com.breadcost.masterdata` — JPA entities + repositories + services:
  - `DepartmentEntity`, `DepartmentRepository`, `DepartmentService`
  - `ProductEntity`, `ProductRepository`, `ProductService`
  - `RecipeEntity`, `RecipeIngredientEntity`, `RecipeRepository`, `RecipeService`
- `com.breadcost.api` — added:
  - `DepartmentController` → `GET/POST/PUT /v1/departments`
  - `ProductController` → `GET/POST/PUT /v1/products`
  - `RecipeController` → `GET/POST /v1/recipes`, `POST /v1/recipes/{id}/activate`, `GET /v1/recipes/{id}/material-requirements`

**Key design decisions made:**
- Recipes are versioned — editing creates a new version (DRAFT). Activating a version archives the previous ACTIVE one and updates `product.activeRecipeId`
- Ingredients support 3 unit modes: WEIGHT, PIECE, COMBO — with purchasing unit conversion and per-ingredient waste factor
- Material requirements calculation: `totalWeight * (1 + wasteFactor) / purchasingUnitSize` → purchasing units needed per batch
- Departments have configurable lead times (hours) and warehouse mode (SHARED or ISOLATED)

**Tested and working** — all 3 POST endpoints verified with live API calls.

---

### R1 Domain 1 — Order Management ✅ DONE
**New files:**
- `domain/Order.java`, `domain/OrderLine.java`
- `events/OrderCreatedEvent.java`, `OrderConfirmedEvent.java`, `OrderCancelledEvent.java`
- `masterdata/OrderEntity.java`, `OrderLineEntity.java`, `OrderRepository.java`, `OrderService.java`
- `api/OrderController.java`

**Key design decisions:**
- Statuses: DRAFT → CONFIRMED → IN_PRODUCTION → READY → OUT_FOR_DELIVERY → DELIVERED | CANCELLED
- Rush order detection: `isAfterCutoff()` based on configurable hour (default 22 = 10 PM, Asia/Tashkent)
- Rush premium configurable via `breadcost.order.rush-premium-pct` (default 15%)
- Lead time conflict detection per line: department.leadTimeHours checked against requestedDeliveryTime
- Conflict is informational (does not block order) — `leadTimeConflict=true/false` per line
- `@JsonIgnore` on `OrderLineEntity.order` to prevent Jackson circular reference

**Tested and working** — create/confirm/cancel all verified with live API calls.

---

## 4. Existing Codebase (pre-session)

Already implemented before this session:

| Package | Contents |
|---|---|
| `domain` | `Batch`, `Item`, `Lot`, `Location`, `Site`, `Period`, `LedgerEntry`, `CommandIdempotency`, `ExceptionCase`, `RecognitionOutputSet` |
| `commands` | `ReceiveLotCommand/Handler`, `IssueToBatchCommand/Handler`, `TransferInventoryCommand/Handler`, `CloseBatchCommand`, `ClosePeriodCommand`, `CommandResult` |
| `events` | `DomainEvent` (interface), `ReceiveLotEvent`, `IssueToBatchEvent`, `TransferInventoryEvent`, `CloseBatchEvent`, `BackflushConsumptionEvent`, `FGValueAdjustmentEvent`, `LateEntryNotEligibleForFGAdjEvent`, `RecognizeProductionEvent` |
| `eventstore` | `EventStore` (in-memory, append-only, ledgerSeq ordered), `IdempotencyService`, `StoredEvent` |
| `finance` | `FinanceService` (posting rules: RM→WIP, WIP→FG) |
| `projections` | `InventoryProjection` (⚠️ uses weighted avg cost, needs FIFO), `BatchCostView`, `InventoryValuationView`, `ProjectionEngine` |
| `api` | `BatchController`, `InventoryController`, `ViewController`, `GlobalExceptionHandler` |
| `security` | `SecurityConfig` |
| `validation` | `ValidationService` |

---

## 5. Known Issues / Technical Debt

| Issue | Details |
|---|---|
| EventStore is in-memory | All data lost on restart. Replace with PostgreSQL for production. |
| H2 in-memory for JPA | All projection data lost on restart. Replace with PostgreSQL. |
| Inventory projection uses weighted average cost | Must be replaced with FIFO cost layers (projection-only change, no event model change needed) |
| Single hardcoded user (admin/admin) | No real user/role management yet |
| `getPrincipalName()` returns "system" | Needs real Spring Security principal injection |
| No tenant isolation at API layer | `tenantId` comes from request param — needs JWT claim enforcement |

---

## 6. Next Tasks (R1 Build Order)

### ✅ DONE: Order Management Domain
**Files created:**
- `domain/Order.java`, `domain/OrderLine.java`
- `events/OrderCreatedEvent.java`, `OrderConfirmedEvent.java`, `OrderCancelledEvent.java`
- `masterdata/OrderEntity.java`, `OrderLineEntity.java`, `OrderRepository.java`, `OrderService.java`
- `api/OrderController.java`

**Tested and working:**
- `POST /v1/orders` → DRAFT, total calculated, leadTimeConflict detected
- `POST /v1/orders/{id}/confirm` → CONFIRMED
- `POST /v1/orders/{id}/cancel` → CANCELLED
- `POST /v1/orders/{id}/status?targetStatus=X` → state machine transitions

**Key fixes applied:**
- `@JsonIgnore` on `OrderLineEntity.order` to break circular reference
- Role names in `@PreAuthorize` must match SecurityConfig exactly: `'Admin'`, `'ProductionUser'`, `'FinanceUser'`, `'Viewer'` — NOT all-caps

### ✅ DONE: Production Planning Domain
**Files created:**
- `domain/ProductionPlan.java` — Status enum (DRAFT/PUBLISHED/IN_PROGRESS/COMPLETED), Shift enum (MORNING/AFTERNOON/NIGHT)
- `domain/WorkOrder.java` — Status enum (PENDING/STARTED/COMPLETED/CANCELLED)
- `masterdata/ProductionPlanEntity.java` — JPA entity, `@OneToMany(fetch=EAGER)` to WorkOrderEntity
- `masterdata/WorkOrderEntity.java` — `@JsonIgnore @ManyToOne` back-ref prevents circular Jackson serialization
- `masterdata/ProductionPlanRepository.java` — `findByTenantIdAndStatus` accepts `ProductionPlan.Status` enum (not String)
- `masterdata/WorkOrderRepository.java`
- `masterdata/ProductionPlanService.java` — generateWorkOrders, plan/WO lifecycle, getMaterialRequirements
- `api/ProductionPlanController.java` — full REST API

**Key design decisions:**
- `generateWorkOrders()` scans all CONFIRMED orders whose `requestedDeliveryTime` falls on `planDate` (UTC)
- Groups order lines by (departmentId + productId), calculates `batchCount = ceil(targetQty / batchSize)`
- Idempotent: `forceRegenerate=false` skips products already in the plan
- `completeWorkOrder()` auto-completes the plan when all WOs are COMPLETED or CANCELLED
- Material requirements: `batchCount × ingredientQtyPerBatch × (1 + wasteFactor)` → purchasing units
- `findByTenantIdAndStatus` repository method MUST take `ProductionPlan.Status` enum, not String; service converts via `.valueOf()`

**Live test results (2026-03-04):**
```
Plan: ddd7adb2-468a-4be8-8c83-f8102ca57d26
  Created: DRAFT
  generateWorkOrders: 1 WO — White Bread 300.0 PCS, 6 batches (2 confirmed orders × 150 PCS)
  DRAFT → PUBLISHED → IN_PROGRESS: OK
  WO f55d789f-...: PENDING → STARTED → COMPLETED
  Plan auto-completed: COMPLETED ✅
  Material requirements: [{Wheat Flour: 0.3672 G}] ✅
```

**Build state:** `mvn clean package -DskipTests` → BUILD SUCCESS (79 source files)

### R1 status — ALL DOMAINS COMPLETE ✅

### Next Tasks (R2):
1. **FIFO fix** in `InventoryProjection` — replace weighted avg with per-lot cost layers
2. **POS domain** — point-of-sale transactions
3. **Min-stock alerts**
4. **Period close enforcement**
5. **Basic reporting / dashboard**
**Files to create:**
- `domain/Order.java` — Order aggregate with status machine
- `domain/OrderLine.java` — Line items with product, qty, price
- `masterdata/OrderEntity.java`, `OrderLineEntity.java`
- `masterdata/OrderRepository.java`, `OrderLineRepository.java`
- `masterdata/OrderService.java` — create, confirm, modify, cancel order; cutoff enforcement; rush order flagging
- `events/OrderCreatedEvent.java`, `OrderConfirmedEvent.java`, `OrderCancelledEvent.java`
- `api/OrderController.java` — REST API

**Key business rules for Order domain:**
- Orders are for B2B customers (by operator on their behalf)
- Statuses: DRAFT → CONFIRMED → IN_PRODUCTION → READY → DELIVERED → CANCELLED
- Cutoff time configurable (default 8–10 PM). After cutoff: standard orders blocked.
- Rush orders allowed after cutoff: configurable premium % applied, manually overridable
- Lead time per department — if product can't meet delivery time, customer notified (flag on order line)
- Orders can contain products from multiple departments with different lead times
- Order lines have: productId, departmentId, qty, unitPrice, requestedDeliveryTime, leadTimeConflict flag

### After Order Management:
1. **Production Planning** — `ProductionPlan`, `WorkOrder` generated from confirmed orders + recipes
2. **FIFO fix** in `InventoryProjection` — replace weighted avg with per-lot cost layers
3. **POS domain**
4. **Min-stock alerts**
5. **Period close enforcement**
6. **Basic reporting / dashboard**

---

## 7. Architecture Pattern to Follow (for all new domains)

Every new domain MUST follow this exact pattern:
```
Command (DTO with validation) 
  → CommandHandler (validate, idempotency check, emit event)
    → DomainEvent (implements DomainEvent interface)
      → EventStore.appendEvent(event, EntryClass.FINANCIAL or OPERATIONAL)
        → Projection (listens via registerListener, rebuilds on startup)
          → JPA Entity (read model, stored in H2/PostgreSQL)

Separately:
  → REST Controller (@PreAuthorize with role check)
    → Service (business logic, calls repository)
      → JPA Repository (Spring Data)
```

Key rules:
- Every command has an `idempotencyKey` field
- Every event implements `DomainEvent` interface (tenantId, siteId, occurredAtUtc, idempotencyKey, getEventType())
- `FinanceService.applyPostingRule()` must have a case for each new financial event type
- All entities have `tenantId` for multi-tenancy
- `@PreAuthorize` on all controller methods

---

## 8. pom.xml Key Settings

```xml
<java.version>21</java.version>
<maven.compiler.release>21</maven.compiler.release>  <!-- compiles to Java 21 bytecode, runs on JDK 25 -->
<lombok.version>edge-SNAPSHOT</lombok.version>        <!-- required for JDK 25 annotation processing -->

<repositories>
  <repository>
    <id>lombok-edge</id>
    <url>https://projectlombok.org/edge-releases</url>
  </repository>
</repositories>
```

`.mvn/jvm.config` — has 10 `--add-opens` flags for Lombok/javac on Java 25.

---

## 9. API Test Examples (working)

```powershell
# Auth header helper
$cred = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes("admin:admin"))

# Create department
$body = '{"tenantId":"tenant1","name":"Bakery","leadTimeHours":8,"warehouseMode":"ISOLATED"}'
Invoke-RestMethod -Uri "http://localhost:8080/v1/departments" -Method POST -Body $body -ContentType "application/json" -Headers @{Authorization="Basic $cred"}

# Create product
$body = '{"tenantId":"tenant1","departmentId":"<deptId>","name":"Sourdough Loaf 400g","saleUnit":"PIECE","baseUom":"PCS"}'
Invoke-RestMethod -Uri "http://localhost:8080/v1/products" -Method POST -Body $body -ContentType "application/json" -Headers @{Authorization="Basic $cred"}

# Create recipe (DRAFT)
Invoke-RestMethod -Uri "http://localhost:8080/v1/recipes" -Method POST -Body $body -ContentType "application/json" -Headers @{Authorization="Basic $cred"}

# Activate recipe
Invoke-RestMethod -Uri "http://localhost:8080/v1/recipes/<recipeId>/activate?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}

# Get material requirements for 3 batches
Invoke-RestMethod -Uri "http://localhost:8080/v1/recipes/<recipeId>/material-requirements?tenantId=tenant1&batchMultiplier=3" -Headers @{Authorization="Basic $cred"}

# ── PRODUCTION PLANNING ──────────────────────────────────────────────────────
# Create plan (DRAFT)
$body = '{"tenantId":"tenant1","siteId":"s1","planDate":"2026-03-04","shift":"MORNING","notes":"Daily plan"}'
$plan = Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans" -Method POST -Body $body -ContentType "application/json" -Headers @{Authorization="Basic $cred"}
$planId = $plan.planId

# Generate work orders from CONFIRMED orders on that date
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/$planId/generate?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}

# Publish → Start → Complete plan
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/$planId/publish?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/$planId/start?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}

# Work order lifecycle
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/work-orders/<woId>/start?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/work-orders/<woId>/complete?tenantId=tenant1" -Method POST -Headers @{Authorization="Basic $cred"}

# Material requirements (purchasing shopping list)
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/$planId/material-requirements?tenantId=tenant1" -Headers @{Authorization="Basic $cred"}

# List work orders by department
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/work-orders?tenantId=tenant1&departmentId=<deptId>" -Headers @{Authorization="Basic $cred"}

# GET plan by ID — MUST include tenantId param or returns 500
Invoke-RestMethod -Uri "http://localhost:8080/v1/production-plans/$planId`?tenantId=tenant1" -Headers @{Authorization="Basic $cred"}
```
