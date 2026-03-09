# BreadCost App — Work Session Snapshot
**Last Updated:** 2026-03-09 (R3 complete — all 15 stories Done, Jira synced, pushed to main)
**Purpose:** Handoff context for continuing development in a new chat session

> **Frontend Requirements:** See `work/120-task/FE_REQUIREMENTS.md` — APPROVED.
> **Frontend Spec:** See `work/120-task/FRONTEND_SPEC.md` — frozen reference for R1/R1.5 pages.

---

## Current State Summary

| Layer | Status |
|-------|--------|
| **R1 Backend** | ✅ Done — 60 stories, all core domains |
| **R1.5 Frontend** | ✅ Done — 14 pages (login thru technologist) |
| **R2 Backend** | ✅ Done — 33 stories, 7 epics, ~130 tests, 67 endpoints across 10 controllers |
| **R2 Frontend** | ✅ Done — 7 new pages (suppliers, deliveries, invoices, report-builder, loyalty, subscriptions, customers) |
| **Phase 3a Infra** | ✅ Done — PostgreSQL, Docker, Flyway V1, TenantContext, multi-tenancy foundation |
| **R3-S1 Backend** | ✅ Done — 6 stories (exchange rate, supplier API, AI WhatsApp), V2 migration (6 tables), 283 tests |
| **R3-S2 Backend** | ✅ Done — 6 stories (AI suggestions, driver mobile), V3 migration (7 tables), 309 tests |
| **R3-S3 Backend** | ✅ Done — 3 stories (AI pricing, AI anomaly, mobile customer app), V4 migration (4 tables), 328 tests |

**Repo:** `https://github.com/tigranatoyan/breadcost-app.git`

---

## Tech Stack

| Component | Version / Detail |
|-----------|-----------------|
| Java | 21 (Spring Boot 3.4.2) |
| Build | Gradle 8.11 (build.gradle.kts) |
| DB (dev) | H2 file-based (`./data/breadcost`) |
| DB (prod) | PostgreSQL 16 via Docker Compose |
| Migration | Flyway V1 (37 tables), V2 (6 tables), V3 (7 tables), V4 (4 tables) |
| Frontend | Next.js 14.2.18, React 18, TypeScript 5, Tailwind CSS 3.4 |
| i18n | Custom React Context (EN + HY, ~750 keys each) |
| Auth | Spring Security basic auth (admin/admin) |
| Multi-tenancy | TenantContext ThreadLocal + TenantFilter (JWT tenant extraction) |

**Run backend:** `.\gradlew bootRun` (port 8080)
**Run frontend:** `cd frontend && npm run dev` (port 3000)
**Build frontend:** `cd frontend && npm run build` (24 routes, 0 errors)

---

## File Counts

| Category | Count |
|----------|-------|
| Backend Java (src/main) | ~195 files |
| Backend Tests (src/test) | ~51 files, 328 tests passing |
| Frontend pages (app/**/page.tsx) | 22 files (21 routes + layout) |
| Frontend total (tsx/ts/css/mjs) | 34 files |
| Backend packages | 23 (api, ai, commands, customers, delivery, domain, driver, events, eventstore, finance, invoice, loyalty, masterdata, mobile, multitenancy, projections, purchaseorder, reporting, security, subscription, supplier, validation, whatsapp) |

---

## Frontend Pages (21 routes)

### R1/R1.5 Pages (14)
| Route | Purpose |
|-------|---------|
| `/login` | Auth login form |
| `/dashboard` | KPI widgets, stock alerts, production snapshot |
| `/orders` | Order CRUD, status flow, cancel w/ reason |
| `/products` | Product CRUD, pricing, VAT |
| `/recipes` | Recipe versioning, ingredients, technology steps |
| `/departments` | Department CRUD, lead times, warehouse mode |
| `/floor` | Floor worker shift plan, WO panel, tech step checkboxes |
| `/inventory` | Positions, adjustments, lot detail, receive lot, FIFO |
| `/pos` | Point of sale, receipt modal, EOD reconciliation |
| `/production-plans` | Plan CRUD, work orders, auto-generate, status flow |
| `/reports` | Revenue, COGS, waste, margins, work order analytics |
| `/admin` | User CRUD, config editor, password reset |
| `/technologist` | Recipe overview, cost analysis, production stats |

### R2 Pages (7) — Added 2026-03-09
| Route | Purpose | Key Features |
|-------|---------|-------------|
| `/suppliers` | Supplier CRUD + Purchase Orders | Tabs: Suppliers / POs. Catalog viewer, PO line items, approve, export XLSX, auto-suggest |
| `/deliveries` | Delivery run management | Create runs, assign orders, manifest view, complete/fail/redeliver/waive |
| `/invoices` | B2B invoicing + discount rules | Tabs: Invoices / Discount Rules. Status filter, payment recording, credit check/limit, void |
| `/report-builder` | KPI block catalog + custom reports | Tabs: My Reports / KPI Catalog. Block picker, run reports, dynamic result table, export |
| `/loyalty` | Loyalty program admin | Tabs: Tiers / Balance / History. Award/redeem points, transaction history |
| `/subscriptions` | Subscription tier management | Tabs: Plans / Current. Card grid, assign/change plan, feature access checker |
| `/customers` | Customer portal | Tabs: Customers / Catalog / Orders. Registration, order creation with product selection |

### Navigation Sections (AuthShell.tsx)
| Section | Routes | Roles |
|---------|--------|-------|
| Main | dashboard | all |
| Operations | orders, production-plans, floor, technologist | varies |
| Catalog | products, recipes, departments, inventory | varies |
| Sales | pos | admin, management, cashier |
| Supply Chain | suppliers, deliveries | admin, management |
| Finance | invoices, customers | admin, management, finance |
| Loyalty | loyalty | admin, management |
| Analytics | reports, report-builder | admin, management, finance |
| Platform | subscriptions | admin |
| Configuration | admin | admin |

---

## R2 Backend Endpoints (67 total)

| Controller | Prefix | Endpoints |
|------------|--------|-----------|
| SupplierController | `/v2/suppliers` | CRUD + catalog items |
| PurchaseOrderController | `/v2/purchase-orders` | CRUD + approve + suggest + export |
| DeliveryRunController | `/v2/delivery-runs` | CRUD + assign + complete + fail + redeliver + waive + manifest |
| InvoiceController | `/v2/invoices` | List + detail + payments + void |
| CustomerController | `/v2/customers` | CRUD + credit-check + credit-limit + discount-rules |
| LoyaltyController | `/v2/loyalty` | Tiers CRUD + balance + award + redeem + history |
| SubscriptionController | `/v2/subscriptions` | Tiers + tenant assignment + feature check |
| ReportController | `/v2/reports` | KPI blocks + custom reports CRUD + run + export |
| ProductController | `/v2/products` | Product catalog (customer-facing) |
| OrderController | `/v2/orders` | Customer order CRUD |

## R3 Backend Endpoints

| Controller | Prefix | Endpoints |
|------------|--------|-----------|
| ExchangeRateController | `/v3/exchange-rates` | CRUD + latest rate |
| SupplierApiConfigController | `/v3/supplier-api-configs` | CRUD |
| AiWhatsAppController | `/v3/ai/whatsapp` | Conversations CRUD + messages + resolve |
| AiSuggestionController | `/v3/ai/suggestions` | Replenishment generate/list/dismiss + forecast generate/list + production generate/list |
| AiPricingAnomalyController | `/v3/ai` | Pricing generate/list/dismiss/accept + anomaly generate/list/acknowledge/dismiss |
| DriverController | `/v3/driver` | Sessions start/end/location + manifest + stop updates + packaging confirm + payment collect |
| MobileAppController | `/v3/mobile` | Device registration + push notifications + order status notifications |

---

## Database Schema

**Flyway V1** — 37 tables covering: tenants, users, roles, departments, products, recipes, recipe_ingredients, technology_steps, orders, order_lines, inventory_positions, inventory_adjustments, lots, batches, work_orders, production_plans, pos_sessions, pos_transactions, suppliers, supplier_catalog_items, purchase_orders, purchase_order_lines, delivery_runs, delivery_run_orders, invoices, invoice_lines, invoice_payments, customers, customer_discount_rules, loyalty_tiers, loyalty_balances, loyalty_transactions, subscription_tiers, tenant_subscriptions, kpi_blocks, custom_reports, custom_report_blocks.

**Flyway V2** (R3-S1) — 6 tables: exchange_rates, supplier_api_configs, ai_whatsapp_conversations, ai_whatsapp_messages, ai_whatsapp_order_intents, ai_whatsapp_templates.

**Flyway V3** (R3-S2) — 7 tables: ai_replenishment_hints, ai_demand_forecasts, ai_production_suggestions, driver_sessions, driver_stop_updates, packaging_confirmations, driver_payments.

**Flyway V4** (R3-S3) — 4 tables: ai_pricing_suggestions, ai_anomaly_alerts, mobile_device_registrations, push_notifications.

---

## Frontend Patterns & Conventions

- **SFC pattern:** `'use client'` → imports → `TENANT_ID='tenant1'` → component function → `apiFetch` calls → `useState` only → JSX with Tailwind
- **UI kit:** `components/ui.tsx` exports: Modal, Table, Spinner, Alert, Badge, Field, Success
- **i18n:** `useT()` hook → `t('section.key')` dot notation, EN + HY locales
- **API:** `lib/api.ts` → `apiFetch(url)` with auth header, all calls include `?tenantId=TENANT_ID`
- **Auth:** `lib/auth.ts` → role guard, `AuthShell` sidebar with role-based nav filtering
- **Tabs:** Multi-tab pages use `useState<string>` tab selector with button group
- **No external state:** No Redux/Zustand — all local useState

---

## Jira State

| Release | Stories | Status |
|---------|---------|--------|
| R1 | 60 | ✅ All Done |
| R1.5 | 23 | ✅ All Done (4 epics, Sprints 4-7) |
| R2 | 33 + FE | ✅ Backend Done, FE Done |
| R3-S1 | 6 | ✅ Done (exchange rate, supplier API, AI WhatsApp) |
| R3-S2 | 6 | ✅ Done (AI suggestions, driver mobile) |
| R3-S3 | 3 | ✅ Done (AI pricing, AI anomaly, mobile customer app) |

---

## What's Next (Priority Order)

1. **R3 Frontend** — Build R3 frontend pages for AI dashboards, driver mobile, customer mobile
2. **Testing** — End-to-end integration tests, load testing
3. **Deployment** — Production Docker setup, CI/CD pipeline

---

## Session History

### 2026-03-09 — R3-S3 Backend (AI Pricing, Anomaly, Mobile App)

**Stories:** BC-2001, BC-2002, BC-2301
**V4 migration:** 4 new tables (ai_pricing_suggestions, ai_anomaly_alerts, mobile_device_registrations, push_notifications)

**AI Pricing (BC-2001 / FR-12.5):**
- `AiPricingSuggestionEntity` — per-product price adjustment suggestions with reasoning
- `AiPricingAnomalyService.generatePricingSuggestions()` — analyzes 90-day order history, suggests volume discounts (high demand) or markups (low demand)
- Endpoints: generate, list, pending, dismiss, accept

**AI Anomaly (BC-2002 / FR-12.6):**
- `AiAnomalyAlertEntity` — anomaly alerts with severity, deviation %, explanation, suggested action
- `AiPricingAnomalyService.generateAnomalyAlerts()` — compares recent week vs 4-week baseline ‒ detects revenue drops/spikes, order volume changes, AOV shifts
- Endpoints: generate, list, active, acknowledge, dismiss

**Mobile Customer App (BC-2301 / FR-2.1):**
- `MobileDeviceRegistrationEntity` — iOS/Android device token registration
- `PushNotificationEntity` — push notification queue with order status, loyalty, promo types
- `MobileAppService` — device register/unregister, send notifications, order status change handler
- `MobileAppController` — 6 endpoints at `/v3/mobile/**`

**Tests:** 19 new tests (12 pricing/anomaly + 7 mobile). Total: 328 tests, 0 failures.
**R3 COMPLETE** — all 15 stories Done across 3 sprints.

### 2026-03-09 — R3-S2 Backend (AI Suggestions + Driver Mobile)

**Stories:** BC-1901, BC-1902, BC-1903, BC-2101, BC-2102, BC-2103
**V3 migration:** 7 new tables (ai_replenishment_hints, ai_demand_forecasts, ai_production_suggestions, driver_sessions, driver_stop_updates, packaging_confirmations, driver_payments)

**AI Suggestions (BC-1901/1902/1903):**
- `AiReplenishmentHintEntity` — per-item restock hints from consumption rates vs stock
- `AiDemandForecastEntity` — per-product demand forecast with confidence score
- `AiProductionSuggestionEntity` — batch count suggestions from forecasts + recipe sizes
- `AiSuggestionService` — computes from EventStore IssueToBatchEvents + InventoryProjection + order history
- `AiSuggestionController` — 8 endpoints at `/v3/ai/suggestions/**`

**Driver Mobile (BC-2101/2102/2103):**
- `DriverSessionEntity` — session tracking with GPS
- `DriverStopUpdateEntity` — per-stop delivery/fail actions
- `PackagingConfirmationEntity` — pre-departure checklist with discrepancies
- `DriverPaymentEntity` — on-spot cash/card payment collection
- `DriverService` — session lifecycle, stop updates, packaging, payment with auto-invoice-PAID
- `DriverController` — 11 endpoints at `/v3/driver/**`

**Tests:** 26 new tests (10 AI + 16 driver). Total: 309 tests, 0 failures.

### 2026-03-09 — R3-S1 Backend (Exchange Rate, Supplier API, AI WhatsApp)

**Stories:** BC-1801, BC-1802, BC-1803, BC-1804, BC-2201, BC-2202
**V2 migration:** 6 new tables (exchange_rates, supplier_api_configs, ai_whatsapp_conversations, ai_whatsapp_messages, ai_whatsapp_order_intents, ai_whatsapp_templates)
**New packages:** whatsapp (7 files), ai (6 files)
**Controllers:** ExchangeRateController, SupplierApiConfigController, AiWhatsAppController
**Tests:** 18 new tests. Total: 283 tests, 0 failures.

### 2026-03-09 — R2 Frontend (7 pages) + i18n + nav

**Created 7 new pages:**
- `frontend/app/suppliers/page.tsx` (~416 lines) — Suppliers CRUD + catalog + Purchase Orders with tabs
- `frontend/app/deliveries/page.tsx` (~247 lines) — Delivery runs CRUD, assign orders, manifest
- `frontend/app/invoices/page.tsx` (~335 lines) — Invoice list, payments, credit, discount rules
- `frontend/app/report-builder/page.tsx` (~283 lines) — KPI blocks + custom reports + run/export
- `frontend/app/loyalty/page.tsx` (~304 lines) — Tier CRUD, balance, award/redeem, history
- `frontend/app/subscriptions/page.tsx` (~177 lines) — Subscription plans, assignment, feature check
- `frontend/app/customers/page.tsx` (~285 lines) — Customer registration, catalog, order creation

**Updated AuthShell.tsx:** 5 new nav sections (Supply Chain, Finance, Loyalty, Analytics, Platform)
**Updated EN locale:** +11 nav keys, +200 page-level keys across 7 sections, +4 common keys
**Updated HY locale:** +11 nav keys, +200 page-level Armenian translations, +4 common keys
**Build:** 24 routes, 0 errors. Committed as `1b391f3`.

### 2026-03-08 — Phase 3a Infrastructure + R2 Backend

- PostgreSQL 16 Docker Compose + Flyway V1 migration (37 tables)
- TenantContext ThreadLocal + TenantFilter for JWT tenant extraction
- Multi-tenancy foundation for all entities
- 265 tests passing
- R2 backend: 33 stories, 7 epics, 67 endpoints, ~130 tests

### 2026-03-07 — R1.5 Frontend (Sprints 4-7)

- Sprint 4: Inventory adjustments, lot detail, department filter, currency fields, dashboard alerts
- Sprint 5: POS receipt modal, card terminal ref, EOD reconciliation, dashboard revenue widget
- Sprint 6: Admin user CRUD, config editor, catalog FE polish
- Sprint 7: Reports dashboard, production analytics, technologist page

### 2026-03-07 — i18n + AuthShell

- Custom React Context i18n system (EN + HY, ~550 keys each at that time)
- AuthShell refactored with translation keys + language switcher
- All 14 R1/R1.5 pages translated

### 2026-03-04 — Orders Screen + Gradle Migration

- Orders page: status filter, customer search, rush order, cancel w/ reason, status advance, order detail
- Migrated build from Maven to Gradle

### Earlier Sessions — R1 Backend

- Domain 1 (Orders), Domain 4 (Recipes/Products), inventory, events, projections, security

---

## Key Design Decisions

- **Recipes versioned** — editing creates new DRAFT version, activating archives previous ACTIVE
- **Order statuses** — DRAFT → CONFIRMED → IN_PRODUCTION → READY → OUT_FOR_DELIVERY → DELIVERED | CANCELLED
- **Rush orders** — detected by cutoff hour (22:00 Asia/Tashkent), configurable premium (default 15%)
- **i18n** — no external library, React Context + nested dictionaries, Armenian as Unicode escapes
- **Multi-tenancy** — ThreadLocal TenantContext, all queries filtered by tenantId
- **API versioning** — R1 endpoints at `/v1/*`, R2 at `/v2/*`, R3 at `/v3/*`

---

## Requirements Documents

All in `work/120-task/`:

| File | Contents |
|---|---|
| `REQUIREMENTS.md` | Full FR + NFR for all 12 domains + Release mapping (R1/R2/R3) |
| `ARCHITECTURE_REVIEW.md` | Existing codebase analysis vs R1 requirements |
| `FE_REQUIREMENTS.md` | Frontend requirements for all screens, roles, principles |
| `FRONTEND_SPEC.md` | Frozen implementation reference for R1/R1.5 pages |
| `JIRA.md` | Jira project structure, epics, stories, sprints |
| `GUI_TEST_PLAN.md` | Manual GUI test plan |
| `MANUAL_TEST_PLAN.md` | End-to-end manual test scenarios |
