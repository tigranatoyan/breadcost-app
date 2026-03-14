# BreadCost App — Work Session Snapshot
**Last Updated:** 2026-03-11 (Visual alignment — globals.css, 10 page headers, card borders, emoji→lucide icons, build clean)
**Purpose:** Handoff context for continuing development in a new chat session

> **Frontend Requirements:** See `requirements/FE_REQUIREMENTS.md` — APPROVED.
> **Frontend Spec:** See `requirements/FRONTEND_SPEC.md` — frozen reference for R1/R1.5 pages.
> **Release 4 Plan:** See `jira/JIRA_R4.md` — 41 stories, 7 epics, 6 sprints.
> **Architecture:** See `architecture/ARCMAP.md` — 8 arcs, 11 actors, cross-arc dependency map.

---

## Current State Summary

| Layer | Status |
|-------|--------|
| **R1 Backend** | ✅ Done — 60 stories, all core domains |
| **R1.5 Frontend** | ✅ Done — 14 pages (login thru technologist) |
| **R2 Backend** | ✅ Done — 37 stories, 7 epics, ~130 tests, 67 endpoints across 10 controllers |
| **R2 Frontend** | ✅ Done — 7 new pages (suppliers, deliveries, invoices, report-builder, loyalty, subscriptions, customers) |
| **Phase 3a Infra** | ✅ Done — PostgreSQL, Docker, Flyway V1, TenantContext, multi-tenancy foundation |
| **R3-S1 Backend** | ✅ Done — 6 stories (exchange rate, supplier API, AI WhatsApp), V2 migration (6 tables), 283 tests |
| **R3-S2 Backend** | ✅ Done — 6 stories (AI suggestions, driver mobile), V3 migration (7 tables), 309 tests |
| **R3-S3 Backend** | ✅ Done — 3 stories (AI pricing, AI anomaly, mobile customer app), V4 migration (4 tables), 328 tests |
| **R3-FE Frontend** | ✅ Done — 7 stories, 4 new pages + 1 modified (ai-pricing, ai-whatsapp, ai-suggestions, driver, mobile-admin, exchange-rates, supplier API tab) |
| **R4-S1 BE (Security)** | ✅ Done — 4 stories (SecurityConfig, @PreAuthorize, own-data enforcement, /me endpoint), 404 tests |
| **R4-S1 FE (Design System)** | ✅ Done — 4 stories (component library, sidebar rework, top nav, 404 page) |

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
| Auth | JWT (JwtUtil + JwtAuthFilter), Spring Security, @PreAuthorize RBAC |
| Multi-tenancy | TenantContext ThreadLocal + TenantFilter (JWT tenant extraction) |

**Run backend:** `.\gradlew bootRun` (port 8080) or `java -jar build/libs/breadcost-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8085`
**Run frontend:** `cd frontend && npm run dev` (port 3000) — Node.js at `C:\Program Files\nodejs` (may need PATH prepend)
**Build frontend:** `cd frontend && npm run build` (28 routes, 0 errors)
**Note:** API_BASE in `frontend/lib/api.ts` currently set to port 8085 (match server port)

---

## File Counts

| Category | Count |
|----------|-------|
| Backend Java (src/main) | ~198 files |
| Backend Tests (src/test) | ~54 files, 404 tests passing |
| Frontend pages (app/**/page.tsx) | 28 files (27 routes + layout) |
| Frontend total (tsx/ts/css/mjs) | 43 files |
| Backend packages | 23 (api, ai, commands, customers, delivery, domain, driver, events, eventstore, finance, invoice, loyalty, masterdata, mobile, multitenancy, projections, purchaseorder, reporting, security, subscription, supplier, validation, whatsapp) |

---

## Frontend Pages (27 routes)

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

### R3-FE Pages (6 new routes + 1 modified) — Added 2026-03-09
| Route | Purpose | Key Features |
|-------|---------|-------------|
| `/exchange-rates` | Exchange rate management | Rate table, add rate, lookup, converter, fetch from API |
| `/ai-whatsapp` | AI WhatsApp dashboard | Tabs: All/Escalated. Conversation list, message threads, draft orders, resolve |
| `/ai-suggestions` | AI suggestions | Tabs: Replenishment/Forecast/Production. Generate, list, dismiss, pending filter |
| `/ai-pricing` | AI pricing & anomalies | Tabs: Pricing Suggestions/Anomaly Alerts. Generate, accept/dismiss, severity badges |
| `/driver` | Driver sessions | Tabs: Active Sessions/Packaging/Payments. Manifest view, end session, lookup |
| `/mobile-admin` | Mobile device admin | Tabs: Devices/Notifications. Device list, remove, send notification |
| `/suppliers` *(modified)* | + API Config tab | New tab: supplier API configs, add config modal, send PO via API |

### Navigation Sections (AuthShell.tsx — reworked R4-S1)
| Section | Routes | Roles |
|---------|--------|-------|
| Main | dashboard | all |
| Operations | orders, production-plans | admin, management |
| Warehouse | inventory | admin, management, warehouse |
| Sales | pos | admin, management, cashier |
| Reports | reports | admin, management, viewer, finance |
| My Shift | floor | floor |
| Workshop | recipes, products | technologist |
| Analysis | technologist | technologist, admin |
| Floor | floor | admin |
| Supply Chain | suppliers, deliveries | admin, management |
| Finance | invoices, customers | admin, management, finance |
| Loyalty | loyalty | admin, management |
| Analytics | report-builder | admin, management, finance |
| AI & Automation | ai-whatsapp, ai-suggestions, ai-pricing | admin, management |
| Driver | driver | admin, management |
| Platform | subscriptions, exchange-rates, mobile-admin | admin |
| Configuration | admin, departments, products, recipes | admin |

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
- **Design system:** `components/design-system.tsx` exports: cn, Button, Card, StatCard, SectionTitle, InputField, SelectField, Progress, Badge, SidebarItem, Table (trial-matching)
- **Global CSS:** `globals.css` — btn/btn-primary/btn-secondary/btn-xs/input classes via `@apply` (rounded-md, inline-flex, gap-2). All pages inherit consistent button/input styling.
- **Lucide icons:** `lucide-react` installed — all pages use lucide icons (no emoji icons). Icons imported per page.
- **i18n:** `useI18n()` hook → `t('section.key')` dot notation, EN + HY locales (~1150 keys each)
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
| R2 | 37 | ✅ All Done |
| R3-S1 | 6 | ✅ Done (exchange rate, supplier API, AI WhatsApp) |
| R3-S2 | 6 | ✅ Done (AI suggestions, driver mobile) |
| R3-S3 | 3 | ✅ Done (AI pricing, AI anomaly, mobile customer app) |
| R3-FE | 7 | ✅ All Done (2 epics, 2 sprints, BC-247–BC-253) |
| R4-S1 | 8 | ✅ Done (security hardening: 4 BE + design system: 4 FE) |

---

## What's Next (Priority Order)

1. **R4-S2** — Customer Portal: Auth & Catalog (5 stories, 23 SP: BC-2801–2803, BC-2901–2902)
2. **R4-S3** — Customer Portal: Commerce (5 stories, 23 SP: BC-2804–2806, BC-2903–2904)
3. **R4-S4** — Customer Account + Notifications (8 stories, 31 SP: BC-2807–2809, BC-2905, BC-3001–3004)
4. **R4-S5** — Visual Rework P1 + Subscription Enforcement (8 stories, 29 SP: BC-3101–3103, BC-3201–3205)
5. **R4-S6** — Visual Rework P2 + QA (7 stories, 27 SP: BC-3206–3212)

---

## Session History

### 2026-03-11 — Visual Alignment: FIGMA_TRIAL → Live Pages

**Scope:** Align all live frontend pages with `FIGMA_TRIAL.tsx` design prototype.

**API Fix:**
- `api.ts` API_BASE changed from port 8080 → 8085
- 3 files (`report-builder`, `suppliers`, `admin`) had hardcoded `localhost:8080` URLs → replaced with `${API_BASE}`
- Installed `lucide-react` dependency

**globals.css Update (affects all 28 pages):**
- `.btn` → added `inline-flex items-center justify-center gap-2 rounded-md`
- `.btn-xs` → added `inline-flex items-center justify-center gap-1.5 rounded-md`
- `.input` → changed `rounded` → `rounded-md`, improved focus ring
- All buttons and inputs across every page now match design-system styling

**Page Header Eyebrow Pattern (10 pages):**
Added `<div class="text-xs font-semibold uppercase tracking-[0.24em] text-blue-600">EYEBROW</div>` + bold title + subtitle pattern:
- Dashboard → "Overview", Recipes → "Workshop", Orders → "Sales", POS → "Retail"
- Production Plans → "Production", Floor → "Production", Inventory → "Inventory"
- Products → "Catalog", Departments → "Operations", Reports → "Insights"

**Card Border Updates (20+ instances):**
- `border rounded-xl` → `rounded-2xl border border-gray-200` across orders, pos, production-plans, inventory, floor, reports

**Emoji → Lucide Icon Replacements:**
- Dashboard: emoji KPIs → lucide icons in blue-50 circles, emoji issues → AlertTriangle
- Recipes: ⏱→Clock3, ▲/▼→ChevronUp/Down
- Orders: 📅→Calendar, ▲/▼→ChevronUp/Down
- POS: 🍞→Package
- Floor: ⏳→Clock3, ▶️→Play, ✅→Check, ❌→X, 🏭→Factory, 📋→ClipboardList, 🔧→Wrench, ⏱→Clock3, 🌡→Thermometer
- Reports: 📥→Download, ↻→RefreshCw (animated spin), ⚡→Zap

**Other:**
- `tsconfig.json` — excluded `FIGMA_TRIAL.tsx` from build (pre-existing duplicate key error)
- Build: 28 routes, 0 errors

**Files changed:** globals.css, tsconfig.json, api.ts, dashboard, recipes, orders, pos, production-plans, inventory, floor, products, departments, reports, report-builder, suppliers, admin (16 files)

### 2026-03-11 — R4-S1: Security Hardening + Design System

**Stories:** BC-2601, BC-2602, BC-2603, BC-2604, BC-2701, BC-2702, BC-2703, BC-2704
**Epics:** BC-E24 (Security Hardening), BC-E25 (Design System Refresh)
**Tests:** 404 total (76 new/fixed), 0 failures

**Security Hardening (BC-E24 — 4 stories):**

- **BC-2601 (Secure SecurityConfig):** `/v2/**` and `/v3/**` now require authentication. Public exceptions: `POST /v2/customers/register`, `POST /v2/customers/login`, `GET /v2/products/**`, `POST /v3/ai/webhook/whatsapp`.
- **BC-2602 (@PreAuthorize on v2/v3 controllers):** 14 controllers with class-level `@PreAuthorize`, 3 controllers with method-level annotations. Role combos per controller (e.g., DeliveryController = Admin+Manager, LoyaltyController = per-method roles). AiConversationController has `permitAll()` override on webhook.
- **BC-2603 (Customer own-data enforcement):** New `CustomerSecurityUtil.java` with `assertOwner(customerId)` — checks JWT principal vs customerId, Admin/Manager bypass. Wired into CustomerController (getProfile, updateProfile) and CustomerOrderController (placeOrder, getOrderStatus, getOrderHistory). JwtAuthFilter now stores tenantId in `auth.setDetails()`. 5 new tests in `CustomerOwnDataTest.java`.
- **BC-2604 (Customer /me endpoint):** `GET /v2/customers/me` reads customerId from JWT principal. `@PreAuthorize("hasRole('Customer')")`. 3 new tests in `CustomerMeEndpointTest.java`.
- **Token fixes:** 26 test files updated — empty bearer tokens → `bearer("admin1")`, `adminToken()` → `bearer("admin1")`.

**Design System Refresh (BC-E25 — 4 stories):**

- **BC-2701 (Component library):** New `frontend/components/design-system.tsx` — 11 typed components: `cn`, `Button` (5 variants × 3 sizes), `Card`, `StatCard`, `SectionTitle`, `InputField`, `SelectField`, `Progress`, `Badge` (19 status variants), `SidebarItem`, `Table`.
- **BC-2702 (Sidebar rework):** AuthShell sidebar: `bg-slate-900 rounded-[28px]`, brand block (ChefHat icon + BreadCost + ERP subtitle), `SidebarItem` components with lucide-react icons per nav item, section grouping, mobile hamburger with overlay.
- **BC-2703 (Top nav rework):** White header bar with `max-w-[1800px]`, ChefHat logo, language toggle (EN/HY), notification bell, user avatar, logout. Responsive hamburger below `lg`.
- **BC-2704 (Custom 404):** `app/not-found.tsx` — large "404" text, "This tray came out empty", dashboard + catalog CTA buttons. EN/HY translations added.

**Files changed:** 21 backend (2 security, 17 controllers, 2 new test files), 26 test token fixes, 4 frontend (design-system.tsx, AuthShell.tsx, not-found.tsx, locales).

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

## Requirements & Planning Documents

| File | Location | Contents |
|---|---|---|
| `REQUIREMENTS.md` | `requirements/` | Full FR + NFR for all 12 domains + Release mapping (R1/R2/R3) |
| `FE_REQUIREMENTS.md` | `requirements/` | Frontend requirements for all screens, roles, principles |
| `FRONTEND_SPEC.md` | `requirements/` | Frozen implementation reference for R1/R1.5 pages |
| `FIGMA_DESIGN_PROMPT.md` | `requirements/` | Figma design prompt for trial visual prototype |
| `ARCHITECTURE_REVIEW.md` | `architecture/` | Existing codebase analysis vs R1 requirements |
| `ARCMAP.md` | `architecture/` | 8 arcs, 11 actors, cross-arc dependency map |
| `JIRA.md` | `jira/` | Jira project structure, epics, stories, sprints |
| `JIRA_R4.md` | `jira/` | Release 4 plan — 41 stories, 7 epics, 6 sprints |
| `GUI_TEST_PLAN.md` | `reports/` | Manual GUI test plan |
| `MANUAL_TEST_PLAN.md` | `reports/` | End-to-end manual test scenarios |
