# BreadCost Frontend Inventory

> **Generated from**: `frontend/` directory  
> **Framework**: Next.js 14 (App Router, `'use client'` pages) + React 18 + Tailwind CSS 3  
> **Total Pages**: 27 `page.tsx` files under `frontend/app/`  
> **Shared Components**: 2 files (`AuthShell.tsx`, `ui.tsx`)  
> **Locales**: 2 (English `en.ts`, Armenian `hy.ts`)

---

## 1. Infrastructure

### 1.1 API Layer — `lib/api.ts`
- **`apiFetch<T>(path, opts?)`** — Generic fetch wrapper
- `API_BASE = 'http://localhost:8080'`, `TENANT_ID = 'tenant1'`
- Auto-injects `Authorization: Bearer <token>` from `localStorage('bc_token')`
- Auto-injects `Content-Type: application/json`
- 401 → redirect to `/login`
- Parses JSON error responses (`{ error, message }`)

### 1.2 Auth — `lib/auth.ts`
- **Storage**: `localStorage` keys `bc_token`, `bc_user`
- **`UserInfo`**: `{ username, roles, displayName }`
- **Roles**: `admin | floor | management | viewer | finance | warehouse | cashier | technologist`
- **Role mapping** (backend → frontend):  
  `Admin→admin`, `ProductionUser/ProductionSupervisor→floor`, `Technologist→technologist`, `FinanceUser→finance`, `Manager→management`, `Warehouse/WarehouseKeeper→warehouse`, `Cashier→cashier`, default→`viewer`
- **Exports**: `setSession()`, `clearCredentials()`, `getToken()`, `getUserInfo()`, `getUsername()`, `getRole()`, `hasRole()`, `isLoggedIn()`

### 1.3 i18n — `lib/i18n.tsx`
- Custom React context provider (`I18nProvider`)
- `useT()` hook returns `t(key, vars?)` function
- Dot-notation key resolution (`'dashboard.revenue'`)
- Variable interpolation (`{count}`, `{pct}`)
- `localStorage` persistence key: `breadcost_locale`
- 2 locales: `en` (English), `hy` (Armenian)

### 1.4 Shared Components — `components/ui.tsx`

| Component | Props | Description |
|-----------|-------|-------------|
| `Modal` | `title, onClose, children, wide?` | Overlay dialog with backdrop, optional wide mode |
| `Spinner` | — | Centered loading spinner |
| `Alert` | `msg, onClose` | Red error banner |
| `Success` | `msg, onClose` | Green success banner |
| `Badge` | `status` | Color-coded status pill (~15 statuses mapped) |
| `Field` | `label, hint?, children` | Form field wrapper with label |
| `Table` | `cols[], rows[][], empty?` | Generic table with empty state |

### 1.5 Auth Shell — `components/AuthShell.tsx`
- **Sidebar navigation**: 19 nav sections with role-based filtering
- **Locale switcher**: EN/HY toggle in sidebar
- **Auth guard**: Redirects to `/login` if not logged in
- **Role-based default routes**: `floor→/floor`, `cashier→/pos`, `warehouse→/inventory`, `technologist→/recipes`, `finance→/reports`
- **Logout** button clears credentials

---

## 2. Page Inventory

### 2.1 `/` — Root Redirect
| Attribute | Value |
|-----------|-------|
| **Route** | `/` |
| **File** | `app/page.tsx` |
| **Functionality** | Immediate redirect to `/dashboard` |

---

### 2.2 `/login` — Login Page
| Attribute | Value |
|-----------|-------|
| **Route** | `/login` |
| **File** | `app/login/page.tsx` (~120 lines) |
| **CRUD** | — (auth only) |
| **API Endpoints** | `POST /v1/auth/login` |
| **Modals/Forms** | Login form (username + password) |
| **Filters** | — |
| **RBAC** | Public (no auth required) |
| **i18n keys** | `login.*` |
| **Features** | Demo account quick-login buttons (admin, production, finance, viewer), JWT auth, stores via `setSession()` |

---

### 2.3 `/dashboard` — Dashboard
| Attribute | Value |
|-----------|-------|
| **Route** | `/dashboard` |
| **File** | `app/dashboard/page.tsx` (~500 lines) |
| **CRUD** | Read-only |
| **API Endpoints** | `GET /v1/departments`, `GET /v1/products`, `GET /v1/orders`, `GET /v1/production-plans`, `GET /v1/inventory/positions`, `GET /v1/items`, `GET /v1/inventory/alerts`, `GET /v1/reports/revenue-summary` |
| **Modals/Forms** | — |
| **Filters** | — |
| **RBAC** | All authenticated roles |
| **i18n keys** | `dashboard.*` (~45 keys) |
| **Features** | KPI cards (Revenue, Open Orders, Today's Plans, Stock Value), Today's Orders widget, Active Plans widget, Next Event banner, Delivery Timeline (8 items), Issues Panel, Stock Alerts, Revenue Widget, Production Floor progress section. Auto-refresh 60s, tick 30s |

---

### 2.4 `/orders` — Order Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/orders` |
| **File** | `app/orders/page.tsx` (~500+ lines) |
| **CRUD** | Create, Read, Update (status transitions) |
| **API Endpoints** | `GET /v1/orders`, `GET /v1/products`, `POST /v1/orders`, `POST /v1/orders/{id}/confirm`, `POST /v1/orders/{id}/cancel`, `POST /v1/orders/{id}/status` |
| **Modals/Forms** | Create Order modal (customer, delivery time, notes, rush toggle, rush premium %, multi-line product/qty/price), Cancel dialog with reason |
| **Filters** | Status dropdown (DRAFT, CONFIRMED, IN_PRODUCTION, READY, OUT_FOR_DELIVERY, DELIVERED, CANCELLED + ALL), Customer text search |
| **RBAC** | All authenticated |
| **i18n keys** | `orders.*` (~30 keys) |
| **Features** | Status workflow: DRAFT→CONFIRMED→IN_PRODUCTION→READY→OUT_FOR_DELIVERY→DELIVERED, CANCELLED. Expandable row detail showing order lines with lead time conflict detection. Rush order support |

---

### 2.5 `/products` — Product Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/products` |
| **File** | `app/products/page.tsx` (~280 lines) |
| **CRUD** | Create, Read, Update |
| **API Endpoints** | `GET /v1/products`, `GET /v1/departments`, `POST /v1/products`, `PUT /v1/products/{id}` |
| **Modals/Forms** | Create modal (department, name, description, sale unit, base UoM, price, VAT), Edit modal (same + status) |
| **Filters** | Text search by name, Department dropdown (BC-1706) |
| **RBAC** | All authenticated |
| **i18n keys** | `products.*` (~20 keys) |
| **Features** | Table: Name, Department, Sale Unit, Base UoM, Price, VAT%, Status, Actions. Status: ACTIVE/INACTIVE |

---

### 2.6 `/recipes` — Recipe Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/recipes` |
| **File** | `app/recipes/page.tsx` (~500+ lines) |
| **CRUD** | Create, Read, Update, Delete (technology steps) |
| **API Endpoints** | `GET /v1/products`, `GET /v1/departments`, `GET /v1/recipes?productId=`, `POST /v1/recipes`, `POST /v1/recipes/{id}/activate`, `PUT /v1/recipes/{id}/ingredients`, `GET/POST/PUT/DELETE /v1/technology-steps` |
| **Modals/Forms** | Create recipe modal (batch size/UoM, yield/UoM, production notes, lead time hours, ingredients list) |
| **Filters** | Department dropdown (BC-1707), Product dropdown selector |
| **RBAC** | All authenticated |
| **i18n keys** | `recipes.*` (~35 keys) |
| **Features** | Recipe list per product with expandable cards. Tabs: Ingredients (view + inline edit), Technology Steps (full CRUD). Activate recipe action |

---

### 2.7 `/departments` — Department Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/departments` |
| **File** | `app/departments/page.tsx` (~200 lines) |
| **CRUD** | Create, Read, Update |
| **API Endpoints** | `GET /v1/departments`, `POST /v1/departments`, `PUT /v1/departments/{id}` |
| **Modals/Forms** | Create modal (name, lead time hours, warehouse mode), Edit modal (BC-1705: same + status) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `departments.*` (~15 keys) |
| **Features** | Table: Name, Lead Time, Warehouse Mode (ISOLATED/SHARED), Status (ACTIVE/INACTIVE), Actions |

---

### 2.8 `/production-plans` — Production Plan Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/production-plans` |
| **File** | `app/production-plans/page.tsx` (~500+ lines) |
| **CRUD** | Create, Read, Update (status + work order actions) |
| **API Endpoints** | `GET /v1/production-plans`, `GET /v1/production-plans/{id}`, `POST /v1/production-plans`, `POST /v1/production-plans/{id}/generate`, `POST /v1/production-plans/{id}/approve`, `POST /v1/production-plans/{id}/start`, `POST /v1/production-plans/{id}/complete`, `POST /v1/production-plans/work-orders/{woId}/start\|complete\|cancel`, `GET /v1/production-plans/{id}/material-requirements`, `GET /v1/production-plans/{id}/schedule`, `PATCH /v1/production-plans/work-orders/{woId}/schedule` |
| **Modals/Forms** | Create plan modal (date, shift MORNING/AFTERNOON/NIGHT, notes) |
| **Filters** | Status dropdown (DRAFT, GENERATED, APPROVED, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED), Date picker |
| **RBAC** | All authenticated |
| **i18n keys** | `productionPlans.*` (~25 keys) |
| **Features** | Plan status workflow: DRAFT→GENERATED→APPROVED→PUBLISHED→IN_PROGRESS→COMPLETED. Expandable plan cards with work orders table. Work order actions (start/complete/cancel). Actual yield input on completion. Material requirements panel. Schedule/Gantt timeline with lead time display. Inline schedule editing (start offset, duration hours) |

---

### 2.9 `/floor` — Production Floor
| Attribute | Value |
|-----------|-------|
| **Route** | `/floor` |
| **File** | `app/floor/page.tsx` (~500+ lines) |
| **CRUD** | Read, Update (work order status) |
| **API Endpoints** | `GET /v1/production-plans`, `GET /v1/production-plans/{id}`, `GET /v1/recipes/active?productId=`, `GET /v1/technology-steps?recipeId=`, `POST /v1/production-plans/work-orders/{woId}/start\|complete\|cancel` |
| **Modals/Forms** | WO Detail side-panel with Tabs: Technology Steps (checklist), Recipe/Ingredients view |
| **Filters** | Automatic: shows today's plans + any IN_PROGRESS plans |
| **RBAC** | `floor` role default route |
| **i18n keys** | `floor.*` (~25 keys) |
| **Features** | Today's production overview. Shift indicator with color-coded badge (MORNING/AFTERNOON/NIGHT). Plan cards with progress bar (done/total work orders). Work order cards with status icon. Slide-out detail panel: technology step checklist (localStorage-persisted), recipe ingredient view with per-batch/total calculations. WO actions: start, complete, cancel. Recipe + steps caching. 60s auto-refresh |

---

### 2.10 `/inventory` — Inventory Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/inventory` |
| **File** | `app/inventory/page.tsx` (~500+ lines) |
| **CRUD** | Create (items + stock receipt), Read, Update (items + adjustments + transfers) |
| **API Endpoints** | `GET /v1/inventory/positions`, `GET /v1/items`, `GET /v1/departments`, `POST /v1/inventory/receipts`, `POST /v1/inventory/transfers`, `POST /v1/inventory/adjust`, `POST /v1/items`, `PUT /v1/items/{id}` |
| **Modals/Forms** | Receive Stock modal (item, qty, unit cost, supplier ref), Transfer modal (item, lot, qty, from/to location), Adjust modal (BC-1501: item, adjustment qty, reason code, notes), Item create/edit modal (name, type, base UoM, description, min threshold) |
| **Filters** | Type filter (INGREDIENT, PACKAGING, FG, BYPRODUCT, WIP), Department/Location, Text search, Below-threshold-only checkbox |
| **RBAC** | Role check: `admin`/`warehouse` for adjustments; `warehouse` default route |
| **i18n keys** | `inventory.*` (~50 keys) |
| **Features** | Two tabs: Stock Levels, Items. Alert count badge for low stock. Lot expand (BC-1502). Type-coded badges. Valuation display. Stock position table with expandable rows. Reason codes: WASTE, SPOILAGE, COUNT_CORRECTION, OTHER. Idempotency keys on receipts |

---

### 2.11 `/pos` — Point of Sale
| Attribute | Value |
|-----------|-------|
| **Route** | `/pos` |
| **File** | `app/pos/page.tsx` (~500+ lines) |
| **CRUD** | Create (sales) |
| **API Endpoints** | `GET /v1/products`, `GET /v1/departments`, `POST /v1/pos/sales`, `POST /v1/pos/reconcile` |
| **Modals/Forms** | Quick-add popover (qty, unit price), Receipt modal (BC-1601), EOD reconciliation modal (BC-1603) |
| **Filters** | Product search, Department dropdown |
| **RBAC** | `cashier` default route |
| **i18n keys** | `pos.*` (~30 keys) |
| **Features** | Split-pane: product grid + cart panel. Product card grid with quick-add overlay. Cart with line qty ±, remove, clear. Payment: CASH (with cash received + change calculation) or CARD (with card reference). Receipt modal with print support (opens print window). EOD reconciliation: total transactions, cash/card totals, refunds, net sales, expected drawer amount. Print EOD report |

---

### 2.12 `/reports` — Reports & Analytics
| Attribute | Value |
|-----------|-------|
| **Route** | `/reports` |
| **File** | `app/reports/page.tsx` (~500+ lines) |
| **CRUD** | Read-only |
| **API Endpoints** | `GET /v1/orders`, `GET /v1/inventory/positions`, `GET /v1/items`, `GET /v1/production-plans`, `GET /v1/departments`, `GET /v1/recipes/active?productId=`, `GET /v1/reports/revenue-summary`, `GET /v1/reports/top-products` |
| **Modals/Forms** | — |
| **Filters** | Report tab selector (Orders, Inventory, Production, Revenue, Costs), Date range (from/to) |
| **RBAC** | `finance` default route |
| **i18n keys** | `reports.*` (~40 keys) |
| **Features** | 5 sub-reports: **Orders** (KPIs, revenue by status, top customers, order listing), **Inventory** (KPIs, valuation by type, low stock alerts, full positions table), **Production** (plan/WO KPIs, plan status breakdown with bar charts, shift distribution, recent plans), **Revenue** (revenue summary, top products), **Costs** (batch cost analysis via recipes). CSV export helper. Money formatting helpers |

---

### 2.13 `/admin` — Administration
| Attribute | Value |
|-----------|-------|
| **Route** | `/admin` |
| **File** | `app/admin/page.tsx` (~500+ lines) |
| **CRUD** | Create, Read, Update (users + config) |
| **API Endpoints** | `GET /v1/users`, `POST /v1/users`, `PUT /v1/users/{id}`, `POST /v1/users/{id}/reset-password`, `GET /v1/config`, `PUT /v1/config`, `GET /v1/departments` |
| **Modals/Forms** | Create user form (username, password, display name, role), Edit user modal (BC-1701: displayName, role, department, active), Password reset modal (BC-1702), Config edit form (BC-1703: order cutoff time, rush premium %, main currency) |
| **Filters** | — |
| **RBAC** | Admin-only section |
| **i18n keys** | `admin.*` (~40 keys) |
| **Features** | Master Data links (Departments, Products, Recipes, Inventory). Two tabs: Users, System. User management with role assignment (7 roles). Demo accounts fallback display. System info panel (app version, stack, API base, tenant ID). Operational settings (cutoff time, rush premium, currency) |

---

### 2.14 `/technologist` — Technologist Dashboard
| Attribute | Value |
|-----------|-------|
| **Route** | `/technologist` |
| **File** | `app/technologist/page.tsx` (~400+ lines) |
| **CRUD** | Read-only (links to /recipes for editing) |
| **API Endpoints** | `GET /v1/products`, `GET /v1/production-plans`, `GET /v1/departments`, `GET /v1/recipes/active?productId=` |
| **Modals/Forms** | — |
| **Filters** | Department dropdown |
| **RBAC** | `technologist` default route |
| **i18n keys** | `technologist.*` (~20 keys) |
| **Features** | KPI row (with active recipe, missing recipe, with lead time, avg lead time). Recipe health table: expandable product rows showing active recipe details (batch, yield, lead time, ingredients, production notes). Yield Variance Analysis (BC-1805). Production frequency bar chart. Recent plans timeline. Links to /recipes for management |

---

### 2.15 `/suppliers` — Supplier Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/suppliers` |
| **File** | `app/suppliers/page.tsx` (~500+ lines) |
| **CRUD** | Full CRUD (suppliers, catalog, POs, API configs) |
| **API Endpoints** | `GET/POST /v2/suppliers`, `PUT/DELETE /v2/suppliers/{id}`, `GET/POST /v2/suppliers/{id}/catalog`, `GET/POST /v2/purchase-orders`, `PUT /v2/purchase-orders/{id}/approve`, `GET /v2/purchase-orders/{id}/export`, `GET /v2/purchase-orders/{id}`, `POST /v2/purchase-orders/suggest`, `GET/POST /v3/supplier-api/configs`, `POST /v3/supplier-api/send-po` |
| **Modals/Forms** | Create/Edit Supplier modal (name, email, phone, notes), Catalog modal (view + add items), Create PO modal (supplier, notes, FX rate/currency, multi-line items), PO Detail modal, API Config modal |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `suppliers.*`, `supplierApi.*` (~35 keys) |
| **Features** | Three tabs: Suppliers, Purchase Orders, API Config. Supplier catalog management with ingredient pricing. Multi-line PO creation with FX rate support. PO approval workflow. PO export to XLSX. Auto-suggest POs. Supplier API integration config (URL, API key, format JSON/XML). Send PO via API |

---

### 2.16 `/deliveries` — Delivery Run Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/deliveries` |
| **File** | `app/deliveries/page.tsx` (~250 lines) |
| **CRUD** | Create, Read, Update (status + assign orders) |
| **API Endpoints** | `GET/POST /v2/delivery-runs`, `GET /v2/delivery-runs/{id}/orders`, `GET /v2/delivery-runs/{id}/manifest`, `POST /v2/delivery-runs/{id}/assign`, `PUT /v2/delivery-runs/{id}/complete`, `PUT /v2/delivery-runs/{id}/fail`, `POST /v2/delivery-runs/{id}/redeliver`, `POST /v2/delivery-runs/{id}/waive` |
| **Modals/Forms** | Create Run modal (driver name, vehicle plate, scheduled date, notes), Assign Orders modal (comma-separated order IDs), Run Detail modal (orders + manifest) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `deliveries.*` (~20 keys) |
| **Features** | Delivery run table. Assign orders to runs. Complete runs. Per-order actions: fail (with reason prompt), re-deliver, waive. Manifest view with product items |

---

### 2.17 `/invoices` — Invoice & Billing
| Attribute | Value |
|-----------|-------|
| **Route** | `/invoices` |
| **File** | `app/invoices/page.tsx` (~350 lines) |
| **CRUD** | Read, Update (payments, void, credit limit, discount rules) |
| **API Endpoints** | `GET /v2/invoices`, `GET /v2/invoices/{id}`, `POST /v2/invoices/{id}/payments`, `PUT /v2/invoices/{id}/void`, `GET /v2/customers/{id}/credit-check`, `PUT /v2/customers/{id}/credit-limit`, `GET/POST /v2/customers/{id}/discount-rules` |
| **Modals/Forms** | Invoice detail modal, Record Payment modal, Credit Check modal, Set Credit Limit modal, Add Discount Rule modal |
| **Filters** | Status dropdown (ISSUED, PARTIALLY_PAID, PAID, OVERDUE, VOIDED) |
| **RBAC** | All authenticated |
| **i18n keys** | `invoices.*` (~30 keys) |
| **Features** | Two tabs: Invoices, Discounts. Invoice list with status filter. Payment recording. Void invoices. Customer credit check (limit, balance, available). Set credit limits. Discount rule management per customer (product, category, %, min qty, date range) |

---

### 2.18 `/customers` — Customer Portal Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/customers` |
| **File** | `app/customers/page.tsx` (~300 lines) |
| **CRUD** | Create (customers + orders), Read |
| **API Endpoints** | `GET/POST /v2/customers`, `POST /v2/customers/register`, `GET /v2/products`, `GET/POST /v2/orders`, `GET /v2/orders/{id}` |
| **Modals/Forms** | Register Customer modal (name, email, phone), Create Order modal (customer select, multi-line product/qty), Order Detail modal |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `customers.*` (~15 keys) |
| **Features** | Three tabs: Customers, Catalog (read-only product list), Orders. Customer registration. Customer order creation with product selection. Order detail view with line items |

---

### 2.19 `/loyalty` — Loyalty Program
| Attribute | Value |
|-----------|-------|
| **Route** | `/loyalty` |
| **File** | `app/loyalty/page.tsx` (~300 lines) |
| **CRUD** | Full CRUD (tiers), Create (award/redeem), Read (balances, history) |
| **API Endpoints** | `GET/POST /v2/loyalty/tiers`, `PUT/DELETE /v2/loyalty/tiers/{id}`, `GET /v2/loyalty/balance/{custId}`, `POST /v2/loyalty/award`, `POST /v2/loyalty/redeem`, `GET /v2/loyalty/history/{custId}` |
| **Modals/Forms** | Create/Edit Tier modal (name, min points, discount %, perks), Award Points modal (customer, points, reason), Redeem Points modal |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `loyalty.*` (~20 keys) |
| **Features** | Three tabs: Tiers, Balances, History. Loyalty tier CRUD (name, min points, discount %, perks). Customer balance lookup (current points, lifetime points, tier). Award and redeem points. Transaction history per customer |

---

### 2.20 `/report-builder` — Custom Report Builder
| Attribute | Value |
|-----------|-------|
| **Route** | `/report-builder` |
| **File** | `app/report-builder/page.tsx` (~300 lines) |
| **CRUD** | Full CRUD (custom reports) |
| **API Endpoints** | `GET /v2/reports/kpi-blocks`, `GET/POST /v2/reports/custom`, `PUT/DELETE /v2/reports/custom/{id}`, `POST /v2/reports/custom/{id}/run`, `GET /v2/reports/custom/{id}/export` |
| **Modals/Forms** | Create Report modal (name, description, block picker), Edit Report modal, Result modal (dynamic table) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `reportBuilder.*` (~15 keys) |
| **Features** | Two tabs: Reports, Catalog (KPI blocks). KPI block catalog with categories. Custom report creation from block selection (checkbox picker). Run report (displays dynamic data table). Export report to XLSX. Edit/delete reports |

---

### 2.21 `/subscriptions` — Subscription & Feature Gating
| Attribute | Value |
|-----------|-------|
| **Route** | `/subscriptions` |
| **File** | `app/subscriptions/page.tsx` (~200 lines) |
| **CRUD** | Read, Create (assign subscription) |
| **API Endpoints** | `GET /v2/subscriptions/tiers`, `GET /v2/subscriptions/current`, `POST /v2/subscriptions/assign`, `GET /v2/subscriptions/features/{key}` |
| **Modals/Forms** | Assign Tier modal (tier selector) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `subscriptions.*` (~15 keys) |
| **Features** | Two tabs: Tiers (pricing card grid with features list), Assignment (current tier status, change tier). Feature check tool: enter feature key → allowed/denied response |

---

### 2.22 `/exchange-rates` — Exchange Rate Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/exchange-rates` |
| **File** | `app/exchange-rates/page.tsx` (~200 lines) |
| **CRUD** | Create, Read |
| **API Endpoints** | `GET/POST /v3/exchange-rates`, `GET /v3/exchange-rates/lookup`, `GET /v3/exchange-rates/convert`, `POST /v3/exchange-rates/fetch` |
| **Modals/Forms** | Add Rate modal (base currency, target, rate, date), Fetch from API modal (base, targets) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `exchangeRates.*` (~15 keys) |
| **Features** | Rate table (base, target, rate, date). Lookup section: get rate by currency+date. Convert section: convert amount between currencies on a date. Fetch from external API: bulk import rates by base + target currencies |

---

### 2.23 `/ai-pricing` — AI Pricing & Anomaly Detection
| Attribute | Value |
|-----------|-------|
| **Route** | `/ai-pricing` |
| **File** | `app/ai-pricing/page.tsx` (~200 lines) |
| **CRUD** | Read, Update (accept/dismiss/acknowledge) |
| **API Endpoints** | `GET /v3/ai/pricing`, `GET /v3/ai/pricing/pending`, `POST /v3/ai/pricing/generate`, `POST /v3/ai/pricing/{id}/accept`, `POST /v3/ai/pricing/{id}/dismiss`, `GET /v3/ai/anomalies`, `GET /v3/ai/anomalies/active`, `POST /v3/ai/anomalies/generate`, `POST /v3/ai/anomalies/{id}/acknowledge`, `POST /v3/ai/anomalies/{id}/dismiss` |
| **Modals/Forms** | — |
| **Filters** | Pending-only checkbox (pricing), Active-only checkbox (anomalies) |
| **RBAC** | All authenticated |
| **i18n keys** | `aiPricing.*` |
| **Features** | Two tabs: Pricing Suggestions, Anomaly Alerts. Generate pricing suggestions (product, current/suggested price, reasoning). Accept/dismiss suggestions. Anomaly detection with severity levels (LOW/MEDIUM/HIGH/CRITICAL with color coding). Deviation %, explanation, suggested action. Acknowledge/dismiss alerts |

---

### 2.24 `/ai-whatsapp` — AI WhatsApp Order Bot
| Attribute | Value |
|-----------|-------|
| **Route** | `/ai-whatsapp` |
| **File** | `app/ai-whatsapp/page.tsx` (~150 lines) |
| **CRUD** | Read, Update (resolve escalations) |
| **API Endpoints** | `GET /v3/ai/whatsapp/conversations`, `GET /v3/ai/whatsapp/conversations/escalated`, `GET /v3/ai/whatsapp/conversations/{id}/messages`, `GET /v3/ai/whatsapp/conversations/{id}/drafts`, `POST /v3/ai/whatsapp/conversations/{id}/resolve` |
| **Modals/Forms** | Conversation detail modal (chat-style message thread + draft orders) |
| **Filters** | Tab: All vs Escalated conversations |
| **RBAC** | All authenticated |
| **i18n keys** | `aiWhatsapp.*` |
| **Features** | Two tabs: All Conversations, Escalated. Conversation list (phone, status, date). Chat-style message thread (BOT vs customer messages with color differentiation). Draft orders extracted from conversation. Resolve escalation (with/without closing) |

---

### 2.25 `/ai-suggestions` — AI Suggestions (Replenishment, Forecast, Production)
| Attribute | Value |
|-----------|-------|
| **Route** | `/ai-suggestions` |
| **File** | `app/ai-suggestions/page.tsx` (~200 lines) |
| **CRUD** | Read, Create (generate), Update (dismiss) |
| **API Endpoints** | `GET /v3/ai/suggestions/replenishment`, `GET /v3/ai/suggestions/replenishment/pending`, `POST /v3/ai/suggestions/replenishment/generate`, `POST /v3/ai/suggestions/replenishment/{id}/dismiss`, `GET /v3/ai/suggestions/forecast`, `POST /v3/ai/suggestions/forecast/generate`, `GET /v3/ai/suggestions/production`, `POST /v3/ai/suggestions/production/generate` |
| **Modals/Forms** | — |
| **Filters** | Pending-only checkbox (replenishment), Period (WEEKLY/MONTHLY), Forecast days (7/14/30), Plan date |
| **RBAC** | All authenticated |
| **i18n keys** | `aiSuggestions.*` |
| **Features** | Three tabs: Replenishment (product, suggested qty, current stock, days left, reason, dismiss), Demand Forecast (product, forecast days, predicted qty, confidence %), Production Suggestions (product, suggested qty, plan date, reason). Period and horizon selectors |

---

### 2.26 `/driver` — Driver Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/driver` |
| **File** | `app/driver/page.tsx` (~200 lines) |
| **CRUD** | Read, Update (end session) |
| **API Endpoints** | `GET /v3/driver/sessions/active`, `GET /v3/driver/sessions/{id}/manifest`, `POST /v3/driver/sessions/{id}/end`, `GET /v3/driver/packaging/{runId}`, `GET /v3/driver/payments/session/{sessionId}` |
| **Modals/Forms** | Manifest modal (orders list) |
| **Filters** | — |
| **RBAC** | All authenticated |
| **i18n keys** | `driver.*` |
| **Features** | Three tabs: Active Sessions (driver, run, status, time, GPS location, manifest, end session), Packaging (lookup by run ID: confirmation status, discrepancies), Payments (lookup by session ID: order, amount, method, reference) |

---

### 2.27 `/mobile-admin` — Mobile Device & Push Notification Management
| Attribute | Value |
|-----------|-------|
| **Route** | `/mobile-admin` |
| **File** | `app/mobile-admin/page.tsx` (~170 lines) |
| **CRUD** | Read, Delete (devices), Create (notifications) |
| **API Endpoints** | `GET /v3/mobile/devices`, `DELETE /v3/mobile/devices/{id}`, `GET /v3/mobile/notifications`, `POST /v3/mobile/notifications` |
| **Modals/Forms** | Send Notification modal (customer ID, title, body, type, reference ID) |
| **Filters** | Customer ID search (devices), Customer ID search (notifications) |
| **RBAC** | All authenticated |
| **i18n keys** | `mobileAdmin.*` |
| **Features** | Two tabs: Devices (list registered devices by customer, remove devices), Notifications (list sent notifications by customer, send new push notification with type: GENERAL/custom) |

---

## 3. API Version Summary

| API Version | Pages Using |
|-------------|------------|
| `/v1/*` | dashboard, orders, products, recipes, departments, production-plans, floor, inventory, pos, reports, admin, technologist |
| `/v2/*` | suppliers, deliveries, invoices, customers, loyalty, report-builder, subscriptions |
| `/v3/*` | exchange-rates, ai-pricing, ai-whatsapp, ai-suggestions, driver, mobile-admin, suppliers (api-config) |

---

## 4. Role-Based Access Summary

| Role | Default Route | Accessible Sections (via AuthShell sidebar filtering) |
|------|---------------|------------------------------------------------------|
| `admin` | `/dashboard` | All sections |
| `floor` | `/floor` | Dashboard, Floor, Production Plans |
| `management` | `/dashboard` | Dashboard, Orders, Reports, Production Plans |
| `viewer` | `/dashboard` | Dashboard (read-only) |
| `finance` | `/reports` | Dashboard, Reports, Invoices, Exchange Rates |
| `warehouse` | `/inventory` | Dashboard, Inventory |
| `cashier` | `/pos` | Dashboard, POS |
| `technologist` | `/recipes` | Dashboard, Recipes, Technologist |

---

## 5. i18n Coverage

| Locale File | Sections | Estimated Keys |
|-------------|----------|----------------|
| `locales/en.ts` | common, login, nav, roles, dashboard, orders, products, recipes, departments, floor, inventory, pos, productionPlans, reports, admin, technologist, suppliers, supplierApi, deliveries, invoices, reportBuilder, loyalty, customers, subscriptions, exchangeRates, aiPricing, aiWhatsapp, aiSuggestions, driver, mobileAdmin | ~600+ |
| `locales/hy.ts` | Same structure as en.ts | ~600+ (Armenian translations) |

---

## 6. Complete API Endpoint Inventory

### v1 Endpoints
| Method | Endpoint | Used By |
|--------|----------|---------|
| POST | `/v1/auth/login` | login |
| GET | `/v1/departments` | dashboard, products, recipes, production-plans, inventory, pos, reports, admin, technologist |
| POST | `/v1/departments` | departments |
| PUT | `/v1/departments/{id}` | departments |
| GET | `/v1/products` | dashboard, orders, products, recipes, pos, reports, technologist |
| POST | `/v1/products` | products |
| PUT | `/v1/products/{id}` | products |
| GET | `/v1/orders` | dashboard, orders, reports |
| POST | `/v1/orders` | orders |
| POST | `/v1/orders/{id}/confirm` | orders |
| POST | `/v1/orders/{id}/cancel` | orders |
| POST | `/v1/orders/{id}/status` | orders |
| GET | `/v1/recipes` | recipes |
| POST | `/v1/recipes` | recipes |
| GET | `/v1/recipes/active` | floor, technologist, reports |
| POST | `/v1/recipes/{id}/activate` | recipes |
| PUT | `/v1/recipes/{id}/ingredients` | recipes |
| GET/POST/PUT/DELETE | `/v1/technology-steps` | recipes, floor |
| GET | `/v1/production-plans` | dashboard, production-plans, floor, reports, technologist |
| GET | `/v1/production-plans/{id}` | production-plans, floor |
| POST | `/v1/production-plans` | production-plans |
| POST | `/v1/production-plans/{id}/generate\|approve\|start\|complete` | production-plans |
| POST | `/v1/production-plans/work-orders/{woId}/start\|complete\|cancel` | production-plans, floor |
| GET | `/v1/production-plans/{id}/material-requirements` | production-plans |
| GET | `/v1/production-plans/{id}/schedule` | production-plans |
| PATCH | `/v1/production-plans/work-orders/{woId}/schedule` | production-plans |
| GET | `/v1/inventory/positions` | dashboard, inventory, reports |
| GET | `/v1/items` | dashboard, inventory, reports |
| POST | `/v1/items` | inventory |
| PUT | `/v1/items/{id}` | inventory |
| GET | `/v1/inventory/alerts` | dashboard |
| POST | `/v1/inventory/receipts` | inventory |
| POST | `/v1/inventory/transfers` | inventory |
| POST | `/v1/inventory/adjust` | inventory |
| POST | `/v1/pos/sales` | pos |
| POST | `/v1/pos/reconcile` | pos |
| GET | `/v1/reports/revenue-summary` | dashboard, reports |
| GET | `/v1/reports/top-products` | reports |
| GET | `/v1/users` | admin |
| POST | `/v1/users` | admin |
| PUT | `/v1/users/{id}` | admin |
| POST | `/v1/users/{id}/reset-password` | admin |
| GET | `/v1/config` | admin |
| PUT | `/v1/config` | admin |

### v2 Endpoints
| Method | Endpoint | Used By |
|--------|----------|---------|
| GET/POST | `/v2/suppliers` | suppliers |
| PUT/DELETE | `/v2/suppliers/{id}` | suppliers |
| GET/POST | `/v2/suppliers/{id}/catalog` | suppliers |
| GET/POST | `/v2/purchase-orders` | suppliers |
| GET | `/v2/purchase-orders/{id}` | suppliers |
| PUT | `/v2/purchase-orders/{id}/approve` | suppliers |
| GET | `/v2/purchase-orders/{id}/export` | suppliers |
| POST | `/v2/purchase-orders/suggest` | suppliers |
| GET/POST | `/v2/delivery-runs` | deliveries |
| GET | `/v2/delivery-runs/{id}/orders` | deliveries |
| GET | `/v2/delivery-runs/{id}/manifest` | deliveries |
| POST | `/v2/delivery-runs/{id}/assign` | deliveries |
| PUT | `/v2/delivery-runs/{id}/complete` | deliveries |
| PUT | `/v2/delivery-runs/{id}/fail` | deliveries |
| POST | `/v2/delivery-runs/{id}/redeliver` | deliveries |
| POST | `/v2/delivery-runs/{id}/waive` | deliveries |
| GET | `/v2/invoices` | invoices |
| GET | `/v2/invoices/{id}` | invoices |
| POST | `/v2/invoices/{id}/payments` | invoices |
| PUT | `/v2/invoices/{id}/void` | invoices |
| GET | `/v2/customers/{id}/credit-check` | invoices |
| PUT | `/v2/customers/{id}/credit-limit` | invoices |
| GET/POST | `/v2/customers/{id}/discount-rules` | invoices |
| GET | `/v2/customers` | customers |
| POST | `/v2/customers/register` | customers |
| GET | `/v2/products` | customers |
| GET/POST | `/v2/orders` | customers |
| GET | `/v2/orders/{id}` | customers |
| GET/POST | `/v2/loyalty/tiers` | loyalty |
| PUT/DELETE | `/v2/loyalty/tiers/{id}` | loyalty |
| GET | `/v2/loyalty/balance/{custId}` | loyalty |
| POST | `/v2/loyalty/award` | loyalty |
| POST | `/v2/loyalty/redeem` | loyalty |
| GET | `/v2/loyalty/history/{custId}` | loyalty |
| GET | `/v2/reports/kpi-blocks` | report-builder |
| GET/POST | `/v2/reports/custom` | report-builder |
| PUT/DELETE | `/v2/reports/custom/{id}` | report-builder |
| POST | `/v2/reports/custom/{id}/run` | report-builder |
| GET | `/v2/reports/custom/{id}/export` | report-builder |
| GET | `/v2/subscriptions/tiers` | subscriptions |
| GET | `/v2/subscriptions/current` | subscriptions |
| POST | `/v2/subscriptions/assign` | subscriptions |
| GET | `/v2/subscriptions/features/{key}` | subscriptions |

### v3 Endpoints
| Method | Endpoint | Used By |
|--------|----------|---------|
| GET/POST | `/v3/exchange-rates` | exchange-rates |
| GET | `/v3/exchange-rates/lookup` | exchange-rates |
| GET | `/v3/exchange-rates/convert` | exchange-rates |
| POST | `/v3/exchange-rates/fetch` | exchange-rates |
| GET/POST | `/v3/supplier-api/configs` | suppliers |
| POST | `/v3/supplier-api/send-po` | suppliers |
| GET | `/v3/ai/pricing` | ai-pricing |
| GET | `/v3/ai/pricing/pending` | ai-pricing |
| POST | `/v3/ai/pricing/generate` | ai-pricing |
| POST | `/v3/ai/pricing/{id}/accept\|dismiss` | ai-pricing |
| GET | `/v3/ai/anomalies` | ai-pricing |
| GET | `/v3/ai/anomalies/active` | ai-pricing |
| POST | `/v3/ai/anomalies/generate` | ai-pricing |
| POST | `/v3/ai/anomalies/{id}/acknowledge\|dismiss` | ai-pricing |
| GET | `/v3/ai/whatsapp/conversations` | ai-whatsapp |
| GET | `/v3/ai/whatsapp/conversations/escalated` | ai-whatsapp |
| GET | `/v3/ai/whatsapp/conversations/{id}/messages` | ai-whatsapp |
| GET | `/v3/ai/whatsapp/conversations/{id}/drafts` | ai-whatsapp |
| POST | `/v3/ai/whatsapp/conversations/{id}/resolve` | ai-whatsapp |
| GET | `/v3/ai/suggestions/replenishment` | ai-suggestions |
| GET | `/v3/ai/suggestions/replenishment/pending` | ai-suggestions |
| POST | `/v3/ai/suggestions/replenishment/generate` | ai-suggestions |
| POST | `/v3/ai/suggestions/replenishment/{id}/dismiss` | ai-suggestions |
| GET | `/v3/ai/suggestions/forecast` | ai-suggestions |
| POST | `/v3/ai/suggestions/forecast/generate` | ai-suggestions |
| GET | `/v3/ai/suggestions/production` | ai-suggestions |
| POST | `/v3/ai/suggestions/production/generate` | ai-suggestions |
| GET | `/v3/driver/sessions/active` | driver |
| GET | `/v3/driver/sessions/{id}/manifest` | driver |
| POST | `/v3/driver/sessions/{id}/end` | driver |
| GET | `/v3/driver/packaging/{runId}` | driver |
| GET | `/v3/driver/payments/session/{id}` | driver |
| GET | `/v3/mobile/devices` | mobile-admin |
| DELETE | `/v3/mobile/devices/{id}` | mobile-admin |
| GET/POST | `/v3/mobile/notifications` | mobile-admin |
