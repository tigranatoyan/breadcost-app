# BreadCost — Release 4: Customer Portal & Visual Refresh
**Project Key:** BC | **Date:** 2026-03-11 | **Version:** 4.0.0

---

## Counterproposals (decided before planning)

| # | Topic | Trial Has | Proposal | Rationale |
|---|-------|-----------|----------|-----------|
| CP-1 | Mobile Gallery page | Static mockup with 3 iPhone frames | **SKIP** — keep as concept in FIGMA_DESIGN_PROMPT.md | Zero functional value in a web app. Real mobile apps = separate R5 initiative (React Native or native). |
| CP-2 | WhatsApp Flows page | Separate screen with 3 flow cards | **MERGE** — add as sub-tab inside existing AI WhatsApp page | Trial shows only 3 static cards. Not enough content for a standalone page. Better UX as part of WhatsApp management. |
| CP-3 | Login visual | Branded dark gradient with BreadCost logo | **REWORK** existing login page, not new page | Login already works (JWT auth, error handling). Only needs visual refresh. |
| CP-4 | Notification delivery | Template preview cards | **Template CRUD now, delivery in R5** | FCM/APNs + SMTP integration is significant infra work. Ship template management first, defer delivery layer. |
| CP-5 | Visual rework strategy | 26 page-level rewrites | **Component-first migration** | Build design system components in Sprint 1, then migrate pages to use them. Future Figma changes only update the library, not 26 files. |
| CP-6 | Subscription enforcement | Tier cards + tenant assignment table | **Wire enforcement into BE now** | SaaS tiers exist but `hasFeature()` is never called by any service. This is a logic bug that must be fixed regardless of FE work. |

---

## Release 4 — Customer Portal & Visual Refresh

### Release Acceptance Criteria

| # | Criterion | Verified By |
|---|-----------|-------------|
| R4-AC-01 | Customer can register, login, and manage profile via dedicated portal | E2E-CUST-01..03 |
| R4-AC-02 | Customer can browse catalog, add to cart, checkout, and track orders | E2E-CUST-04..07 |
| R4-AC-03 | Customer sees loyalty tier, points balance, and can redeem | E2E-CUST-08 |
| R4-AC-04 | /v2/ and /v3/ endpoints enforce authentication; customer can only access own data | E2E-SEC-01..04 |
| R4-AC-05 | All 26 back-office pages match trial visual style (rounded cards, dark sidebar, stat cards, section titles) | Visual QA |
| R4-AC-06 | Notification templates can be created, edited, and previewed per channel | E2E-NOTIF-01 |
| R4-AC-07 | Subscription tier enforcement blocks access to unpaid features | E2E-SUB-01..02 |
| R4-AC-08 | Custom branded 404/error page displayed for invalid routes | E2E-ERR-01 |

---

## Epics

| Epic | Title | Sprint(s) | Stories |
|------|-------|-----------|---------|
| BC-E24 | Security Hardening | R4-S1 | BC-2601..2604 |
| BC-E25 | Design System Refresh | R4-S1 | BC-2701..2704 |
| BC-E26 | Customer Portal Frontend | R4-S2, S3, S4 | BC-2801..2809 |
| BC-E27 | Customer Portal BE Gaps | R4-S2, S3 | BC-2901..2905 |
| BC-E28 | Notifications & Templates | R4-S4 | BC-3001..3004 |
| BC-E29 | Subscription Enforcement | R4-S5 | BC-3101..3103 |
| BC-E30 | Visual Rework | R4-S5, S6 | BC-3201..3212 |

---

## Sprint R4-S1 — Foundation (Security + Design System) ✅ DONE

**Goal:** Secure all API versions. Build reusable component library matching trial design.
**Stories:** 8 | **Estimated SP:** 34 | **Status:** ✅ Complete — 404 tests, 0 failures
**Commit:** R4-S1 (2026-03-11)

---

### Epic: BC-E24 — Security Hardening

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2601 | **Secure /v2/ and /v3/ endpoints in SecurityConfig** | 1. `SecurityConfig` updated: `/v2/**` and `/v3/**` require authentication (except `POST /v2/customers/register` and `POST /v2/customers/login`) 2. Unauthenticated request to `/v2/orders` → 401 3. Unauthenticated request to `/v3/driver/sessions` → 401 4. Public endpoints still work without token 5. All existing tests pass | P0 | 3 | ✅ Done |
| BC-2602 | **Add @PreAuthorize to all v2/v3 controllers** | 1. `CustomerOrderController`: Customer role can only access own orders (customerId from JWT must match) 2. `CustomerController` profile endpoints: Customer can only access own profile 3. `SubscriptionController`: Admin-only for CRUD, tenant-scoped for read 4. `DriverController`: Admin+Management+Driver roles 5. `MobileAppController`: Customer role for own devices, Admin for all 6. `AiConversationController`: Admin+Management roles 7. Unit tests verify 403 for wrong roles | P0 | 5 | ✅ Done |
| BC-2603 | **Customer own-data-only access enforcement** | 1. Create `CustomerSecurityUtil.assertOwner(principal, customerId)` helper 2. Customer JWT contains customerId as subject 3. All `/v2/customers/{id}/*` endpoints verify JWT subject matches path `{id}` 4. Mismatch → 403 5. Admin role bypasses check 6. Tests: customer A cannot read customer B's orders → 403 | P0 | 5 | ✅ Done |
| BC-2604 | **Customer "me" endpoint** | 1. `GET /v2/customers/me` returns authenticated customer's profile (reads customerId from JWT) 2. No path parameter needed 3. Returns same DTO as `GET /v2/customers/{id}/profile` 4. 401 if no token 5. Test: login → /me → correct profile | P1 | 2 | ✅ Done |

---

### Epic: BC-E25 — Design System Refresh

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2701 | **Create trial-matching component library** | 1. New file `components/design-system.tsx` with all components from trial: `Button` (5 variants: primary/secondary/ghost/success/danger × 3 sizes: xs/sm/lg), `Card` (rounded-[28px], optional title + action), `StatCard` (icon + label + value + hint), `SectionTitle` (eyebrow + title + subtitle + action slot), `Input` (label + placeholder + optional rightIcon), `SelectField` (label + options), `Progress` bar, `Badge` (16 status variants matching trial colors), `MiniBarChart`, `MiniLineChart`, `DonutSummary` 2. `cn()` utility for class merging 3. Tailwind classes match trial exactly: `rounded-[28px]` cards, `bg-slate-900` dark sidebar, `bg-blue-600` primary accent 4. All components export from index, type-safe with TypeScript props 5. Storybook-like demo page at `/design-system` (dev only) showing all components | P0 | 8 | ✅ Done |
| BC-2702 | **Rework AuthShell sidebar to match trial** | 1. Sidebar background: `bg-slate-900` with rounded-[28px] container 2. Brand block: BreadCost logo in blue-600 rounded-2xl icon + name + "ERP" subtitle 3. Navigation items: `SidebarItem` component with icon + label + active state (blue-600 bg, white text) 4. Section grouping with subtle dividers and section titles 5. Collapse/expand on mobile (hamburger menu) 6. Selected item highlights correctly 7. Language switcher and logout in sidebar footer | P0 | 5 | ✅ Done |
| BC-2703 | **Rework top navigation bar** | 1. Header: white bg, border-bottom, max-w-[1800px] centered 2. Left: BreadCost logo + name 3. Right: screen quick-jump dropdown (all 37 screens), language toggle (EN/HY), notification bell button, user avatar/name, logout 4. Responsive: collapses to hamburger below lg breakpoint 5. Back-office / Portal mode toggle (if user has customer portal access) | P1 | 3 | ✅ Done |
| BC-2704 | **Custom 404/error page** | 1. New `app/not-found.tsx` with branded design matching trial: large "404" text, "This tray came out empty" headline, description text, "Back to Dashboard" + "Open Catalog" CTA buttons 2. Armenian translation for all text 3. Next.js `not-found.tsx` convention auto-handles invalid routes 4. Style matches trial: centered layout, slate-900 big number, gray-500 description | P1 | 2 | ✅ Done |

---

## Sprint R4-S2 — Customer Portal: Auth & Catalog

**Goal:** Customer registration, login, and catalog browsing functional end-to-end.
**Stories:** 5 | **Estimated SP:** 23

---

### Epic: BC-E26 — Customer Portal Frontend

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2801 | **FE Customer Login / Register page** | 1. New page at `app/customer/login/page.tsx` 2. Dual-card layout matching trial: "Sign In" card (phone + password + submit + forgot-password link) and "Register" card (business name, contact, phone in +374 format, email, password, confirm password, Create Account button) 3. Calls `POST /v2/customers/login` and `POST /v2/customers/register` 4. On successful login: stores customer JWT + redirects to `/customer/catalog` 5. On successful register: auto-login + redirect 6. Error handling: invalid credentials, duplicate email, validation errors 7. Full Armenian translation 8. Phone format hint: +374-XX-XXXXXX | P0 | 5 |
| BC-2802 | **FE Customer Portal Catalog page** | 1. New page at `app/customer/catalog/page.tsx` 2. Product grid layout (4 cols on xl, 3 on lg, 2 on md, 1 on sm) matching trial 3. Each card: image placeholder (gray rounded box), product name, price in ֏, "Add to cart" button 4. Search bar with real-time filter 5. Category dropdown filter (departments) 6. Sort options: name A-Z, price low-high, price high-low 7. Calls `GET /v2/products` 8. Cart state persisted in localStorage 9. Cart badge count in header 10. Full Armenian translation | P0 | 5 |
| BC-2803 | **FE Customer Portal routing + navigation shell** | 1. New layout at `app/customer/layout.tsx` — separate from back-office AuthShell 2. Portal sidebar: Catalog, Checkout, My Orders, Loyalty, Profile — icons matching trial 3. Header: BreadCost logo, customer name, language toggle, logout 4. Protected route: redirect to `/customer/login` if no customer JWT 5. Different JWT storage key from staff (avoid collision) 6. Responsive sidebar: collapsible on mobile | P0 | 5 |

### Epic: BC-E27 — Customer Portal BE Gaps

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2901 | **BE: Catalog search, filter, pagination** | 1. `GET /v2/products?search=&departmentId=&page=0&size=20&sort=name,asc` 2. `search` does case-insensitive LIKE on product name 3. `departmentId` filters by department 4. Pagination with Spring `Pageable` returning `Page<ProductCatalogDto>` with totalElements, totalPages 5. Only active products with active recipe included 6. Sort options: name, price 7. Tests: search returns filtered set, pagination works, empty page returns empty content | P0 | 5 |
| BC-2902 | **BE: Customer password reset flow** | 1. `POST /v2/customers/forgot-password` accepts email, generates reset token (UUID), stores in DB with 1h expiry 2. `POST /v2/customers/reset-password` accepts token + newPassword, validates token, updates passwordHash, invalidates token 3. Expired token → 400 "Token expired" 4. Invalid token → 404 5. Token single-use (second attempt → 404) 6. Add `PasswordResetTokenEntity` (id, customerId, token, expiresAt, used) 7. Tests: full reset flow, expired token, reuse token | P1 | 3 |

---

## Sprint R4-S3 — Customer Portal: Commerce

**Goal:** Customer can checkout, see order confirmation, and track orders.
**Stories:** 5 | **Estimated SP:** 23

---

### Epic: BC-E26 — Customer Portal Frontend (continued)

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2804 | **FE Customer Checkout page** | 1. New page at `app/customer/checkout/page.tsx` 2. Cart summary: each line shows product, ±qty buttons, delete, line total 3. Delivery options: date picker (min = tomorrow), time slot dropdown, rush toggle with premium display 4. Address selector: dropdown of saved addresses + "Add new" link to profile 5. Notes textarea 6. Price breakdown: subtotal, discount (if loyalty tier applies), rush premium, total 7. "Place Order" button → `POST /v2/orders` 8. Success → redirect to confirmation page 9. Error → inline alert 10. Empty cart → redirect to catalog 11. Full Armenian translation | P0 | 8 |
| BC-2805 | **FE Order Confirmation page** | 1. New page at `app/customer/order-confirmation/[orderId]/page.tsx` 2. Success banner: green gradient card matching trial, "Order #XXXX confirmed" headline, WhatsApp notification mention 3. Order summary: line items with qty × price 4. Total amount 5. Two CTA buttons: "Track Order" (→ My Orders) and "Back to Catalog" 6. Full Armenian translation | P1 | 3 |
| BC-2806 | **FE Customer My Orders page** | 1. New page at `app/customer/orders/page.tsx` 2. Table: order #, date, status badge, total, actions (Track / Reorder) 3. Live tracking panel: ordered timeline (confirmed → production started → out for delivery → delivered) with status dots (blue = done, gray = pending) 4. Reorder button: copies order lines into cart → redirect to checkout 5. Recent history section: last 5 completed orders 6. Calls `GET /v2/orders` and `GET /v2/orders/{id}` 7. Full Armenian translation | P0 | 5 |

### Epic: BC-E27 — Customer Portal BE Gaps (continued)

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2903 | **BE: Order tracking timeline** | 1. `GET /v2/orders/{id}/timeline` returns list of `{status, timestamp, description}` 2. Built from order status transition history (add `OrderStatusHistoryEntity` if not exists) 3. Each status change recorded: DRAFT→CONFIRMED→IN_PRODUCTION→READY→OUT_FOR_DELIVERY→DELIVERED 4. Customer can only view own orders (JWT customerId validation) 5. 404 if order belongs to different customer 6. Tests: create order → advance through statuses → timeline shows all transitions | P1 | 5 |
| BC-2904 | **BE: Customer discount rules controller** | 1. `GET /v2/customers/{id}/discounts` returns applicable discount rules for customer 2. `POST /v2/customers/{id}/discounts` creates discount rule (admin only) 3. `DELETE /v2/customers/{id}/discounts/{ruleId}` removes rule (admin only) 4. Customer can read own discounts; admin can manage any 5. Discounts applied in order total calculation (already exists in entity — wire to controller) 6. Tests: CRUD + discount applied at checkout | P2 | 2 |

---

## Sprint R4-S4 — Customer Account + Notifications

**Goal:** Customer profile management, loyalty dashboard, notification templates.
**Stories:** 8 | **Estimated SP:** 31

---

### Epic: BC-E26 — Customer Portal Frontend (continued)

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2807 | **FE Customer Loyalty Dashboard page** | 1. New page at `app/customer/loyalty/page.tsx` 2. Tier status card: dark gradient (slate-900→slate-700), customer name, points count (large), tier name, progress bar to next tier (% width) — matching trial exactly 3. Points history: list of transactions (AWARD/REDEEM, ±pts, order ref, date) 4. Tier benefits section: current tier perks listed 5. Redemption info: how many points available, redemption rate 6. Calls `GET /v2/loyalty/balance` and `GET /v2/loyalty/history` 7. Full Armenian translation | P0 | 5 |
| BC-2808 | **FE Customer Profile & Settings page** | 1. New page at `app/customer/profile/page.tsx` 2. Business profile card: editable fields (business name, contact person, phone, email) 3. Addresses section: list of saved addresses, add new, edit, delete 4. Notification preferences: toggles for WhatsApp / email / push 5. Password change: current + new + confirm fields 6. Calls `GET /v2/customers/me`, `PUT /v2/customers/{id}/profile` 7. Success toast on save 8. Full Armenian translation | P1 | 5 |
| BC-2809 | **FE WhatsApp Flows sub-tab** | 1. Add "Flows" tab to existing `app/ai-whatsapp/page.tsx` 2. Three flow cards matching trial: "New Order" (customer message → bot confirm/edit/cancel buttons), "Status Update" (automated delivery notification), "Escalation" (customer complex request → team handoff notice) 3. Each card: chat bubble mockup with example messages 4. Read-only reference view (no configuration — that's R5) 5. Full Armenian translation | P2 | 2 |

### Epic: BC-E28 — Notifications & Templates

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-3001 | **BE: Notification template entity + CRUD** | 1. New `NotificationTemplateEntity`: id, tenantId, type (ORDER_CONFIRMATION, PRODUCTION_STARTED, READY_FOR_DELIVERY, OUT_FOR_DELIVERY, DELIVERED, PAYMENT_REMINDER, STOCK_ALERT, PROMOTIONAL), channel (PUSH, EMAIL, WHATSAPP, SMS), subject, bodyTemplate (supports `{{orderNumber}}`, `{{customerName}}`, `{{status}}` vars), active, createdAt, updatedAt 2. Repository with findByTenantIdAndType 3. Flyway migration V12 | P0 | 3 |
| BC-3002 | **BE: Notification template controller** | 1. `GET /v3/notifications/templates?tenantId=` — list all templates 2. `GET /v3/notifications/templates/{id}` — get by ID 3. `POST /v3/notifications/templates` — create (Admin only) 4. `PUT /v3/notifications/templates/{id}` — update 5. `DELETE /v3/notifications/templates/{id}` — soft delete 6. `POST /v3/notifications/templates/{id}/preview` — render template with sample data, return rendered text 7. Authenticated + Admin/Management roles 8. Tests: full CRUD + preview with variable substitution | P0 | 5 |
| BC-3003 | **FE: Notification Templates management page** | 1. New page at `app/notification-templates/page.tsx` 2. Grid of 8 template type cards matching trial layout (4 cols on xl, 2 on md) 3. Each card: template type name, channel icon(s), preview body text, "Edit" button 4. Edit modal: subject, body with variable helpers (insert {{orderNumber}} etc.), channel checkboxes, active toggle 5. Preview panel: rendered sample with highlighted variables 6. Calls template CRUD API 7. Full Armenian translation 8. Add to AuthShell sidebar under Platform section (Admin/Management roles) | P0 | 5 |
| BC-3004 | **BE: Customer notification preferences** | 1. Add `notificationPreferences` embedded object to `CustomerEntity`: whatsappEnabled (default true), emailEnabled (default true), pushEnabled (default true) 2. Included in profile GET/PUT responses 3. `MobileAppService.sendNotification()` checks preferences before creating notification record 4. Flyway migration V13 adds columns 5. Tests: disable push → notification not created | P2 | 3 |

### Epic: BC-E27 — Customer Portal BE Gaps (continued)

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-2905 | **BE: Individual address CRUD** | 1. `POST /v2/customers/{id}/addresses` — add address (returns updated address list) 2. `PUT /v2/customers/{id}/addresses/{index}` — update specific address 3. `DELETE /v2/customers/{id}/addresses/{index}` — remove specific address 4. Max 5 addresses per customer 5. Each address has: label, line1, line2, city, postalCode, country 6. Customer can only manage own addresses (JWT check) 7. Tests: add/update/remove + max limit | P1 | 3 |

---

## Sprint R4-S5 — Visual Rework Phase 1 + Subscription Enforcement

**Goal:** Core back-office pages match trial visuals. Subscription tiers actually enforced.
**Stories:** 8 | **Estimated SP:** 29

> **⚠️ Visual Rework Head-Start (2026-03-11):** The visual alignment session pre-completed
> significant work across BC-3201–3208. Changes already landed (uncommitted):
> - **globals.css**: btn/input classes updated (all 28 pages benefit)
> - **Eyebrow headers**: 10 pages (Dashboard, Recipes, Orders, POS, Production Plans, Floor, Inventory, Products, Departments, Reports)
> - **Card borders**: 20+ instances migrated to `rounded-2xl border border-gray-200`
> - **Emoji → Lucide icons**: Dashboard, Recipes, Orders, POS, Floor, Reports
> - **Dashboard + Recipes**: Fully migrated to design-system components (StatCard, Badge, Progress, Button)
>
> **Per-story status:** BC-3201 ~90% done | BC-3202 ~60% | BC-3203 ~30% | BC-3204 ~40% | BC-3205 ~50% | BC-3206 ~30% | BC-3207 ~30% | BC-3208 ~60%
> **Remaining work**: deeper component migration (design-system Table, Modal, SelectField), filter bars, status flow visualizations, Gantt wrapper, chart components, modal redesigns.

---

### Epic: BC-E29 — Subscription Enforcement

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-3101 | **BE: Wire subscription enforcement into services** | 1. Create `@SubscriptionRequired("FEATURE_KEY")` annotation 2. Create `SubscriptionEnforcementAspect` (AOP) that intercepts annotated controller methods 3. Aspect calls `subscriptionService.hasFeature(tenantId, featureKey)` 4. Missing feature → 403 with errorCode `ERR_SUBSCRIPTION_REQUIRED`, message includes tier upgrade hint 5. Annotate controllers: `AiConversationController`→AI_BOT, `AiPricingAnomalyController`→AI_BOT, `DriverController`→DELIVERY, `InvoiceController`→INVOICING, `LoyaltyController`→LOYALTY, `SupplierController`→SUPPLIER 6. Tests: BASIC tier tenant calling AI endpoint → 403, ENTERPRISE tier → 200 | P0 | 5 |
| BC-3102 | **BE: Enforce maxUsers and maxProducts limits** | 1. `UserService.createUser()` checks tenant's subscription maxUsers limit 2. `ProductService.createProduct()` checks tenant's subscription maxProducts limit 3. Limit exceeded → 409 with `ERR_LIMIT_EXCEEDED` and message "Upgrade your plan to add more users/products" 4. Admin-created users count towards limit 5. Inactive users don't count 6. Tests: BASIC tier with 5 users → create 6th → 409 | P0 | 3 |
| BC-3103 | **FE: Feature-gated navigation** | 1. Fetch tenant subscription on login (new endpoint or include in auth response) 2. `AuthShell` checks feature list before rendering nav sections 3. AI section hidden for BASIC tier 4. Delivery section hidden for BASIC tier 5. Invoicing section hidden for BASIC tier 6. Supplier section hidden for BASIC/STANDARD tier 7. Gated page shows "Upgrade your plan" message if accessed directly via URL 8. Full Armenian translation for gate messages | P1 | 3 |

### Epic: BC-E30 — Visual Rework

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-3201 | **Rework Dashboard visuals** | 1. Replace inline stat boxes with `StatCard` components (icon + label + value + hint) 2. Delivery timeline section with `Progress` bars and time labels 3. Stock alerts panel with severity badges (CRITICAL=red, LOW=amber) 4. Production floor section: department progress bars with completion % 5. Issues detector callout cards 6. Getting started wizard (if new tenant) 7. All using design-system.tsx components 8. Layout: max-w-[1800px], consistent spacing (gap-6) | P0 | 5 |
| BC-3202 | **Rework Orders visuals** | 1. SectionTitle with eyebrow "Operations" 2. Filter bar: customer search input, status dropdown, date range 3. Orders table using design-system Table with trial column styling 4. Create order modal using design-system Card+Input+SelectField 5. Status flow visualization: DRAFT→CONFIRMED→…→DELIVERED horizontal badges 6. All buttons using design-system Button variants | P0 | 3 |
| BC-3203 | **Rework POS visuals** | 1. Product grid: 3-col grid with product cards (image placeholder + name + price + quick-add) 2. Cart panel: line items with ±qty, subtotal/VAT/total 3. Payment modal: amount tendered input, change calculation, Cash/Card buttons 4. Receipt section: formatted receipt preview 5. EOD reconciliation: summary cards | P0 | 3 |
| BC-3204 | **Rework Production Plans visuals** | 1. SectionTitle with eyebrow "Production" 2. Plans table with status badges 3. Expanded plan detail: WO list with status icons 4. Material requirements in Card component 5. Gantt schedule: colored bars with time axis — keep existing logic, update wrapper styling | P1 | 3 |
| BC-3205 | **Rework Floor View visuals** | 1. Plan header with shift badge 2. WO cards: touch-friendly, large enough for tablet use, status icon + product name + qty 3. Selected WO panel: technology steps with checkboxes (keep existing localStorage logic) 4. Recipe snapshot in Card component 5. Responsive: works on tablet in landscape | P1 | 4 |

---

## Sprint R4-S6 — Visual Rework Phase 2 + QA

**Goal:** All remaining pages match trial visuals. Full regression and polish.
**Stories:** 7 | **Estimated SP:** 27

> **Note:** BC-3206–3208 partially addressed in visual alignment head-start (see R4-S5 note). BC-3209–3212 not yet started.

---

### Epic: BC-E30 — Visual Rework (continued)

| Story | Title | Acceptance Criteria | Priority | SP |
|-------|-------|---------------------|----------|-----|
| BC-3206 | **Rework Inventory visuals** | 1. Tab toggle (Stock Levels / Items) using design-system buttons 2. Stock table with LOW/OK badges 3. Alerts & actions panel in Card 4. Receive/Transfer/Adjust modals using design-system Modal+Input+SelectField | P1 | 3 |
| BC-3207 | **Rework Reports + Technologist visuals** | 1. Reports: StatCard KPIs (revenue, orders, plans completed, stock alerts), top products MiniBarChart, orders DonutSummary, production summary table with Progress bars 2. Technologist: recipe health table with colored badges (READY=green, REVIEW=amber, BLOCKED=red), detail panel with alert callouts 3. All using design-system components | P1 | 5 |
| BC-3208 | **Rework Recipes + Products + Departments visuals** | 1. Recipes: SectionTitle, department+search filter bar, recipe table, ingredients tab in Card, technology steps in numbered cards with edit buttons 2. Products: SectionTitle, search+dept filter, product table, create/edit modal 3. Departments: SectionTitle, table with lead time/warehouse mode/status, create/edit modal 4. Consistent section headers and filter layouts matching trial | P1 | 5 |
| BC-3209 | **Rework Admin + Login visuals** | 1. Admin: user table, system config card (currency, date format, RBAC note), demo credentials section 2. Login: dark gradient background (slate-900→slate-800), centered white card with shadow-2xl, BreadCost logo (ChefHat icon in blue-600 rounded-2xl), form fields, demo hint area 3. Must exactly match trial LoginShowcase visual | P0 | 3 |
| BC-3210 | **Rework Extended pages visuals** | All 13 extended pages updated to use design-system components: 1. Suppliers: tables + PO list + API config note 2. Deliveries: run table + manifest preview + per-order action buttons 3. Invoices: invoice table + discount rules + credit alert 4. Customers (admin): customer table + portal catalog preview + portal orders 5. Loyalty (admin): tiers table + balances card + history 6. Report Builder: custom reports table + KPI catalog grid 7. Subscriptions: 4 tier cards + tenant assignment table 8. Exchange Rates: add rate form + converter + history table 9. Driver: active sessions + packaging check + payments 10. Mobile Admin: devices table + notification history 11. All using SectionTitle, Card, Table, Badge, Button from design-system | P1 | 5 |
| BC-3211 | **Rework AI pages visuals** | 1. AI WhatsApp: conversation list + chat bubbles (bot=blue, user=white) + draft order panel + escalation notice 2. AI Suggestions: replenishment cards with confidence %, demand forecast MiniLineChart, production suggestions 3. AI Pricing: pricing table (current/suggested/change columns), anomaly panel with severity badges (Critical=red, Warning=amber, Info=blue) 4. WhatsApp flows sub-tab (from BC-2809) visual polish | P1 | 3 |
| BC-3212 | **Final QA + cross-browser responsiveness** | 1. Test all pages at breakpoints: 375px (mobile), 768px (tablet), 1024px (laptop), 1440px (desktop), 1920px (large) 2. Chrome, Firefox, Edge verified 3. Dark sidebar collapses properly on mobile 4. All modals scrollable on small screens 5. No horizontal overflow on any page 6. All Armenian translations display correctly (no truncation) 7. Customer portal end-to-end flow verified 8. Back-office end-to-end flow verified 9. Fix any visual regression found | P0 | 3 |

---

## Summary

### Release 4 Totals

| Metric | Value |
|--------|-------|
| **Sprints** | 6 (R4-S1 through R4-S6) |
| **Epics** | 7 (BC-E24 through BC-E30) |
| **Stories** | 41 |
| **Story Points** | 167 |
| **BE stories** | 13 (security: 4, BE gaps: 4, notifications: 2, subscription: 2, migration: 1) |
| **FE stories** | 22 (portal: 9, design system: 4, visual rework: 7, notifications: 1, subscription: 1) |
| **Mixed/QA** | 6 |

### Sprint Summary

| Sprint | Theme | Stories | SP | Key Deliverable |
|--------|-------|---------|-----|-----------------|
| R4-S1 | Foundation | 8 | 34 | Secure APIs + design system + error page |
| R4-S2 | Customer Auth + Catalog | 5 | 23 | Customer login/register + catalog browsing |
| R4-S3 | Customer Commerce | 5 | 23 | Checkout + confirmation + order tracking |
| R4-S4 | Customer Account | 8 | 31 | Profile + loyalty + notifications |
| R4-S5 | Visual Rework P1 + Subscription | 8 | 29 | Core pages refreshed + tier enforcement |
| R4-S6 | Visual Rework P2 + QA | 7 | 27 | All pages refreshed + regression pass |

### Story-to-Epic Map

| Epic | Stories | Total SP |
|------|---------|----------|
| BC-E24 Security Hardening | BC-2601, 2602, 2603, 2604 | 15 |
| BC-E25 Design System Refresh | BC-2701, 2702, 2703, 2704 | 18 |
| BC-E26 Customer Portal FE | BC-2801, 2802, 2803, 2804, 2805, 2806, 2807, 2808, 2809 | 43 |
| BC-E27 Customer Portal BE Gaps | BC-2901, 2902, 2903, 2904, 2905 | 18 |
| BC-E28 Notifications & Templates | BC-3001, 3002, 3003, 3004 | 16 |
| BC-E29 Subscription Enforcement | BC-3101, 3102, 3103 | 11 |
| BC-E30 Visual Rework | BC-3201, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3210, 3211, 3212 | 45 |

### Dependencies

```
BC-2701 (design system) ──┬──→ BC-3201..3212 (all visual rework)
                          ├──→ BC-2801..2809 (all portal FE)
                          └──→ BC-3003 (notification templates FE)

BC-2601..2603 (security) ───→ BC-2801 (customer login requires secure endpoints)

BC-2901 (catalog pagination) → BC-2802 (catalog FE needs paginated API)

BC-2903 (timeline API) ────→ BC-2806 (My Orders live tracking)

BC-3001..3002 (template BE) → BC-3003 (template FE)

BC-3101 (subscription BE) ──→ BC-3103 (subscription FE gate)
```

### Version & Tagging Plan

| Version | Tag | Content |
|---------|-----|---------|
| 4.0.0-RC1 | After S4 | Security + Customer Portal (all 7 pages) + Notifications |
| 4.0.0-RC2 | After S5 | + Subscription enforcement + Core visual rework |
| 4.0.0 | After S6 | Full release: all visual rework + QA pass |

---

*End of Release 4 JIRA Plan — Generated 2026-03-11*
