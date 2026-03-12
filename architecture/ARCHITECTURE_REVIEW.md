# Architecture Review — Existing Codebase vs R1 Requirements
**Date:** 2026-03-04  
**Scope:** R1 MVP only  

---

## 1. Existing Architecture Summary

| Layer | Tech | Notes |
|---|---|---|
| Framework | Spring Boot 3.x / Java | |
| Event Store | In-memory (`ConcurrentHashMap`) | ⚠️ No persistence — lost on restart |
| Read Models (Projections) | H2 in-memory via JPA | ⚠️ Lost on restart |
| Pattern | Event Sourcing + CQRS | Well structured |
| API | Spring MVC REST | Partially implemented |
| Security | Spring Security — single hardcoded user (`admin/admin`) | ⚠️ Not production-ready |
| Build | Maven | |

---

## 2. What Is Already Built — Component by Component

### ✅ Solid & Reusable

| Component | Files | Quality | Notes |
|---|---|---|---|
| Event sourcing infrastructure | `EventStore`, `StoredEvent`, `IdempotencyService` | Good | Append-only, strict `ledgerSeq` ordering, listener pattern |
| Command idempotency | `CommandIdempotency`, `IdempotencyService` | Good | Idempotency key per command type — correct pattern |
| Immutable ledger entry | `LedgerEntry` | Excellent | Rich model: financial/operational class, PPA flags, reversal tracking, multi-tenant fields |
| Inventory receipt | `ReceiveLotCommand/Handler/Event` | Good | Validates qty + cost, emits event, idempotent |
| Material issue to batch | `IssueToBatchCommand/Handler/Event` | Good | Emergency mode + approval logic, override reason codes |
| Inventory transfer | `TransferInventoryCommand/Handler/Event` | Good | Location-to-location transfer |
| Batch lifecycle | `Batch`, `CloseBatchCommand/Event` | Good | CREATED → RELEASED → IN\_PROGRESS → CLOSED |
| Accounting period | `Period`, `ClosePeriodCommand` | Good | OPEN → CLOSING → LOCKED |
| FG recognition | `RecognizeProductionEvent`, `FGValueAdjustmentEvent` | Good | WIP → FG posting, late-entry eligibility logic |
| Financial posting rules | `FinanceService` | Good | RM→WIP, WIP→FG postings; extensible switch pattern |
| Domain entities | `Item`, `Lot`, `Site`, `Location`, `ExceptionCase` | Good | Comprehensive ItemType enum (INGREDIENT, FG, WIP, etc.) |
| Global exception handler | `GlobalExceptionHandler` | Good | Centralized API error handling |

### ⚠️ Partially Built — Needs Extension

| Component | Files | Gap |
|---|---|---|
| Inventory projection | `InventoryProjection` | Uses **weighted average cost**, not FIFO. Needs FIFO cost layer tracking per lot. |
| Batch cost view | `BatchCostView` | Schema exists, but projection engine to populate it is not wired. |
| Inventory valuation view | `InventoryValuationView` | Entity exists; query/projection logic incomplete. |
| Batch controller | `BatchController` | Only `IssueToBatch` endpoint wired. `CreateBatch`, `ReleaseBatch`, `CloseBatch` missing. |
| Inventory controller | `InventoryController` | Needs review — unclear what is implemented. |
| Validation service | `ValidationService` | Exists but scope unclear — needs to cover all R1 commands. |
| Security | `SecurityConfig` | RBAC annotations (`@PreAuthorize`) used correctly on API, but backed by single hardcoded user only. No real user store. |
| Multi-tenancy | Throughout | `tenantId` field present on all events and ledger entries — correct. But no tenant isolation enforcement at API/query layer. |

---

## 3. What Is Missing — R1 Gaps

### 🔴 Critical Infrastructure Gaps

| Gap | Impact | Action Needed |
|---|---|---|
| **Event store has no persistence** | All data lost on restart | Replace in-memory event store with database-backed store (PostgreSQL recommended). EventStore interface already exists — swap implementation. |
| **H2 in-memory for projections** | All projections lost on restart | Switch to persistent database. H2 file mode is acceptable for early dev; PostgreSQL for production. |
| **Single hardcoded user** | Cannot support real users or roles | Implement proper user store + JWT or session auth. |

### 🔴 Missing Domains (net new work)

| Domain | Missing Entities / Concepts | FR Coverage |
|---|---|---|
| **Order Management** | `Order`, `OrderLine`, `OrderStatus` — no trace in codebase | FR-1.1, FR-1.5 → FR-1.17 |
| **Customer Management** | `Customer` entity, pricing/discount per customer | FR-9.10, FR-9.11 |
| **Recipe & Product Management** | `Recipe`, `RecipeIngredient`, `Product`, unit conversions, yield, waste | FR-4.1 → FR-4.9 |
| **Department Configuration** | `Department` entity with lead times, warehouse mode, assignable recipes | FR-11.1, FR-11.2 |
| **Production Planning** | `ProductionPlan`, `WorkOrder` — plan generation from orders | FR-3.1 → FR-3.10 |
| **Point of Sale** | POS session, sale transaction, receipt, reconciliation | FR-8.1 → FR-8.6 |
| **User & Role Management** | `User`, `Role`, configurable permissions from UI | FR-11.2 → FR-11.4 |
| **Reporting & Dashboard** | Report engine, KPI projections, dashboard aggregates | FR-10.1, FR-10.4 → FR-10.7 |

### 🟡 Existing Domain Gaps (extend existing work)

| Domain | Current State | Gap |
|---|---|---|
| **Inventory (FIFO)** | Weighted average cost in projection | Implement FIFO cost layers. Each lot receipt creates a cost layer. Issues consume oldest layers first. |
| **Inventory (min stock alerts)** | No threshold logic | Add configurable min threshold per item per site. Trigger alert when position falls below threshold. |
| **Bookkeeping (currency)** | `unitCostBase` field exists on receipt — good foundation | Add `originalCurrency`, `originalUnitCost`, `exchangeRate` fields to `ReceiveLotCommand` and event. Exchange rate table entity needed. |
| **Period close** | `ClosePeriodCommand` exists | Needs full enforcement: block backdated entries after lock, generate period snapshot. |
| **Batch — actual yield** | `CloseBatchEvent` exists | Add `actualYield` recording on close; link to recipe expected yield for variance computation. |
| **Notifications** | None | Add notification service (in-app + WhatsApp stub for R1 alerts: stock shortage, production halt). |

---

## 4. FIFO vs Weighted Average — Action Required

The current `InventoryProjection` uses **weighted average cost**. R1 requires **FIFO**.

**Required change:**
- Replace the single `InventoryPosition` per `(site, item, lot, location)` with a **FIFO cost layer stack** per `(site, item)` (or `(site, item, location)` depending on warehouse mode).
- Each `ReceiveLot` pushes a new cost layer: `{ lotId, qty, unitCost }`.
- Each `IssueToBatch` pops from the front of the stack (oldest first), consuming partial layers as needed.
- The `LedgerEntry` already records `lotId` and `amountBase` — the layer data just needs to be projected correctly.

> This is a projection-layer change only. The event model does not need to change.

---

## 5. Architectural Strengths to Preserve

1. **Event sourcing is the right choice** for this domain. Keep it. Every inventory movement, cost entry, and state transition should remain an immutable event.
2. **CQRS command/handler pattern** is clean and consistent. All new domains should follow the same pattern: `Command → Handler → Event → EventStore → Projection`.
3. **Idempotency on every command** — correct. Extend this to all new commands.
4. **`LedgerEntry` model is comprehensive** — reuse it across all financial domains (orders, POS sales, invoices) by extending `eventType` and posting rules.
5. **`tenantId` on every entity** — multi-tenancy foundation is already correct.

---

## 6. Recommended Tech Stack Additions for R1

| Need | Recommendation | Rationale |
|---|---|---|
| Persistent event store | PostgreSQL | Replace in-memory store. Single source of truth for all events. |
| Persistent projections | PostgreSQL (same DB, separate schema) | Replace H2. Projections rebuild from event store if needed. |
| Authentication | Spring Security + JWT | Replace single hardcoded user. Stateless, scalable, role-per-user. |
| Notification (stub) | Internal event + WhatsApp HTTP stub | In-app alerts R1; WhatsApp message via simple HTTP call in R1 as placeholder for R2 full integration. |
| Frontend | React (new) or Thymeleaf (quick) | `ViewController.java` suggests server-side view is started. Decision needed. |

---

## 7. R1 Build Plan — What to Do in Order

| Priority | Task | Builds On |
|---|---|---|
| 1 | Persist event store (PostgreSQL) | Everything depends on this |
| 2 | Persist projections (PostgreSQL) | Follows from #1 |
| 3 | Implement real User + Role management | Required for all secured endpoints |
| 4 | Fix FIFO cost layers in InventoryProjection | Core cost accuracy |
| 5 | Add currency fields to ReceiveLot (originalCurrency, exchangeRate) | Cost tracking |
| 6 | Build Recipe + Product management domain | Required before production planning |
| 7 | Build Department configuration domain | Required before order routing and production planning |
| 8 | Build Order management domain | Core user-facing feature |
| 9 | Build Production Planning domain (WorkOrder, ProductionPlan) | Depends on Orders + Recipes + Departments |
| 10 | Extend Batch close with actual yield + variance | Depends on Production Planning |
| 11 | Build POS domain | Relatively independent |
| 12 | Build min-stock alert system | Extends Inventory |
| 13 | Build Period close enforcement | Extends existing Period domain |
| 14 | Build basic reporting / dashboard projections | Depends on all above |
| 15 | Wire remaining API endpoints | Wraps all command handlers |

---

## 8. Summary

| Area | Status |
|---|---|
| Event sourcing infrastructure | ✅ Solid foundation — preserve and extend |
| Core inventory movements (receive, issue, transfer) | ✅ Implemented — extend for FIFO and currency |
| Batch + period lifecycle | ✅ Implemented — extend for yield and enforced period lock |
| Financial posting rules | ✅ Good pattern — extend for new event types |
| Idempotency | ✅ Correct — apply to all new commands |
| Multi-tenancy fields | ✅ Present — needs API-layer enforcement |
| Event store persistence | 🔴 Missing — top priority |
| FIFO costing | 🔴 Wrong method currently (weighted avg) — fix in projection |
| Order management | 🔴 Not started |
| Recipe & product management | 🔴 Not started |
| Department configuration | 🔴 Not started |
| Production planning | 🔴 Not started |
| POS | 🔴 Not started |
| User & role management | 🔴 Not started |
| Reporting | 🔴 Not started |
| Customer management | 🔴 Not started |
