# BreadCost App — Frontend Requirements Document
**Version:** 1.0
**Date:** 2026-03-04
**Status:** Draft — Pending Approval
**Companion:** `REQUIREMENTS.md` (BE), `FRONTEND_SPEC.md` (frozen implementation reference)

---

## Table of Contents
1. [Principles & Constraints](#principles)
2. [User Roles & Access](#roles)
3. [Global Shell & Navigation](#shell)
4. [Screen: Login](#login)
5. [Screen: Dashboard](#dashboard)
6. [Screen: Orders](#orders)
7. [Screen: Production Plans](#production-plans)
8. [Screen: Floor (Shift View)](#floor)
9. [Screen: Recipes & Technology Steps](#recipes)
10. [Screen: Products](#products)
11. [Screen: Departments](#departments)
12. [Screen: Inventory](#inventory)
13. [Screen: POS](#pos)
14. [Screen: Reports & Technologist View](#reports)
15. [Screen: Admin Panel (Config & Users)](#admin)
16. [Global Non-Functional Requirements (FE)](#nfr)
17. [R1 Coverage Analysis](#r1-coverage)

---

## 1. Principles & Constraints <a name="principles"></a>

| # | Principle |
|---|-----------|
| P-1 | **No full-page re-renders.** Data mutations shall update only the affected component state, not reload the whole page. Use `useRef` for caches, `useState` only for display state. |
| P-2 | **Role-enforced UI.** Elements the user cannot act on shall be hidden or disabled at the component level, not just at the route level. Server-side enforcement is the security boundary; UI hiding is for UX only. |
| P-3 | **Tenant-scoped API calls.** Every API call shall include `tenantId` (from auth context). No API call shall fire without a resolved `tenantId`. |
| P-4 | **Stable screen identity.** Each screen has one canonical URL. Navigation shall use Next.js `<Link>` and `router.push()` — never full page reloads. |
| P-5 | **Loading/error states.** Every async data fetch shall have: loading skeleton or spinner, error message with retry option, and empty-state message. No screen shall render with undefined/null data silently. |
| P-6 | **Optimistic UI where safe.** Status badge updates and list item removals may be optimistic. Writes that affect financial or production state shall wait for server confirmation before updating UI. |
| P-7 | **Frozen behaviour via spec.** Once a screen is approved and frozen in `FRONTEND_SPEC.md`, its behaviour shall not be changed without an explicit change request. Redesigns start from that document. |
| P-8 | **Tailwind only.** No inline styles, no CSS modules, no additional UI libraries (MUI, Chakra, etc.) unless explicitly approved. Shared primitives live in `frontend/components/ui.tsx`. |
| P-9 | **TypeScript strict mode.** All components shall be fully typed. No `any` types on data interfaces. API response shapes correspond to BE DTOs. |

---

## 2. User Roles & Access <a name="roles"></a>

| Role | Code | Capabilities |
|------|------|-------------|
| Admin | `admin` | Full access to all screens and all actions |
| Management | `management` | Orders, Production Plans, Dashboard, Reports. No Configuration write access. |
| Technologist | `technologist` | Recipes (full CRUD including tech steps), Products (read), Reports (technologist view). |
| Floor Worker | `production` | Floor shift view only. Cannot access orders, recipes, config, or reports. |
| Finance | `finance` | Bookkeeping, Inventory (read), Reports. No production write access. |
| Warehouse | `warehouse` | Inventory (receive, adjust, transfer). No orders or production write access. |
| Cashier | `cashier` | POS screen only. |

### Default Route Per Role
| Role | Default Route on Login |
|------|------------------------|
| `admin` | `/dashboard` |
| `management` | `/dashboard` |
| `technologist` | `/recipes` |
| `production` | `/floor` |
| `finance` | `/reports` |
| `warehouse` | `/inventory` |
| `cashier` | `/pos` |

---

## 3. Global Shell & Navigation <a name="shell"></a>

### FE-SHELL-1 — Auth Shell (`AuthShell.tsx`)
- Wraps all authenticated pages
- Renders a persistent side-nav and top bar
- Handles: JWT token read from localStorage, token expiry check, redirect to `/login` if unauthenticated
- Loading state: shows a full-screen centered spinner — **never returns null** (avoids login page flash)
- Floor worker (`production` role without `admin`/`management`): auto-redirect from `/dashboard` → `/floor`

### FE-SHELL-2 — Side Navigation Structure
Navigation sections are built from the role set of the logged-in user:

```
[Overview]
  Dashboard                   → /dashboard       (all roles except production)

[My Shift]                    ← shown FIRST for production role
  My Shift Plan               → /floor

[Operations]
  Orders                      → /orders           (admin, management)
  Production Plans            → /production-plans (admin, management, production)

[Workshop]
  Recipes & Steps             → /recipes          (admin, technologist)
  Products                    → /products         (admin, technologist, management)
  Departments                 → /departments      (admin only)

[Warehouse]
  Inventory                   → /inventory        (admin, management, warehouse)

[Finance]
  POS                         → /pos              (admin, cashier)
  Reports                     → /reports          (admin, management, finance, technologist)

[Analysis]
  Technologist View           → /technologist     (admin, technologist)

[Administration]
  Admin Panel                 → /admin            (admin only)
```

### FE-SHELL-3 — Top Bar
- Shows: App name (BreadCost), current tenant name, logged-in username + role badge, logout button
- No breadcrumb (screens are flat, not hierarchical in R1)

### FE-SHELL-4 — Active Route Highlight
- Active nav item shown with a left border indicator and background highlight
- Active route determined by `usePathname()`

---

## 4. Screen: Login (`/login`) <a name="login"></a>

### FE-LOGIN-1 — Layout
- Standalone page (not wrapped in AuthShell)
- Centred card on a neutral background
- App logo / name at top of card

### FE-LOGIN-2 — Fields
| Field | Type | Validation |
|-------|------|-----------|
| Username | text input | required |
| Password | password input | required |

### FE-LOGIN-3 — Behaviour
- Submit calls `POST /v1/auth/login` with `{ username, password }`
- On success: store JWT in localStorage, store user object (username, roles, tenantId) in AuthContext, redirect to default route for the user's primary role
- On failure: show inline error "Invalid username or password"
- Button shows spinner while request is in flight; form is disabled

### FE-LOGIN-4 — Already Authenticated
- If AuthContext resolves a valid token on mount, redirect immediately to the default route without showing the login form

---

## 5. Screen: Dashboard (`/dashboard`) <a name="dashboard"></a>

### FE-DASH-1 — Role Access
Visible to: admin, management, finance. Floor workers are auto-redirected away.

### FE-DASH-2 — Content (R1 Scope)
| Widget | Data Source | Roles |
|--------|------------|-------|
| Today's Orders (count + total value) | `GET /v1/orders?tenantId=&date=today` | admin, management |
| Active Production Plans (count, status breakdown) | `GET /v1/production-plans?tenantId=` | admin, management |
| Stock Alert Count (items below min threshold) | `GET /v1/inventory/alerts?tenantId=` | admin, management, warehouse |
| Revenue — Today / Week / Month | `GET /v1/reports/revenue-summary?tenantId=` | admin, management, finance |
| Top 5 Products by Quantity Ordered (this week) | `GET /v1/reports/top-products?tenantId=` | admin, management |

### FE-DASH-3 — Refresh
- Dashboard auto-refreshes every 60 seconds (not on every render)
- A manual "Refresh" button is available

### FE-DASH-4 — R2+ Widgets (out of R1)
- WhatsApp order activity feed
- Delivery status map
- Customer balance alerts

---

## 6. Screen: Orders (`/orders`) <a name="orders"></a>

### FE-ORD-1 — Role Access
admin, management. Read-only for technologist and finance (R2).

### FE-ORD-2 — Order List View
- Table columns: Order #, Customer, Date, Delivery Date, Status badge, Total Value, Actions
- Filters: status dropdown (All / Draft / Confirmed / In Production / Ready / Delivered / Cancelled), date range picker, customer search
- Pagination: 20 rows per page
- Status badge colours:
  - Draft: grey
  - Confirmed: blue
  - In Production: yellow
  - Ready: teal
  - Delivered: green
  - Cancelled: red

### FE-ORD-3 — Create Order (modal or drawer)
Fields:
| Field | Type | Validation |
|-------|------|-----------|
| Customer | searchable select (from customer list) | required |
| Delivery Date | date picker | required, must be future |
| Order Lines | repeating rows: Product (select), Qty (number), Unit Price (auto-filled, editable), Notes | at least 1 line |
| Rush Order toggle | checkbox | requires management role to activate if after cutoff |
| Rush Premium % | number | auto-filled from config, editable if authorized |

- Cutoff enforcement: if current time is past today's cutoff and rush toggle is off, show warning and disable Confirm button
- On save as Draft: `POST /v1/orders?tenantId=` → status = DRAFT
- On confirm: `POST /v1/orders/{id}/confirm?tenantId=`

### FE-ORD-4 — Order Detail / Edit
- Opening an order shows a detail panel (right drawer or separate page)
- Draft orders: all fields editable
- Confirmed orders: only notes and rush override editable (within modification window)
- Status transition buttons (role-gated):
  - Confirm (management/admin on DRAFT)
  - Cancel (management/admin on DRAFT or CONFIRMED)
  - Mark Ready (management/admin)

### FE-ORD-5 — Status Transitions
Follow BE state machine. UI shall only show valid next-state buttons. Invalid transitions are not rendered.

---

## 7. Screen: Production Plans (`/production-plans`) <a name="production-plans"></a>

### FE-PP-1 — Role Access
admin, management for full CRUD. `production` role: read-only view of approved plans.

### FE-PP-2 — Plan List View
- Table: Plan ID (short), Plan Date, Shift, Status badge, Batch Count, Work Orders (count), Actions
- Filter by status (DRAFT / GENERATED / APPROVED / IN_PROGRESS / COMPLETED)
- Status badge colours:
  - DRAFT: grey
  - GENERATED: blue
  - APPROVED: teal
  - IN_PROGRESS: yellow
  - COMPLETED: green

### FE-PP-3 — Create Plan
Inline form above table or modal:
| Field | Type |
|-------|------|
| Plan Date | date picker (default: tomorrow) |
| Shift | select: MORNING / AFTERNOON / NIGHT |
| Batch Count | number (default 1) |

On create: `POST /v1/production-plans?tenantId=`

### FE-PP-4 — Generate Work Orders
- Button "Generate Work Orders" on a DRAFT plan
- Calls `POST /v1/production-plans/{id}/generate?tenantId=`
- On success: plan moves to GENERATED, work order list appears in plan expand
- On empty result: show message "No confirmed orders found — work orders generated from all available confirmed orders" (reflect fallback behaviour)

### FE-PP-5 — Plan Detail (expand row)
Work order table within expanded row:
- Columns: Product Name, Target Qty, Unit, Status badge, Department, Actions
- Actions: Start (PLANNED → IN_PROGRESS), Complete (IN_PROGRESS → COMPLETED), Cancel

### FE-PP-6 — Approve Plan
- Button "Approve Plan" visible to management/admin on GENERATED plan
- Calls `POST /v1/production-plans/{id}/approve?tenantId=`
- Confirmation dialog before action

### FE-PP-7 — No Re-Render
- Plan list shall not re-fetch on every action
- After generate/approve: update the single mutated plan in local state (do not reload whole list)

---

## 8. Screen: Floor — Shift View (`/floor`) <a name="floor"></a>

### FE-FLOOR-1 — Purpose
Active screen for `production` role. Shows the production plan for the current shift. Primary interaction surface for floor workers.

### FE-FLOOR-2 — Plan Resolution
- On mount: fetch today's plan for the current shift → `GET /v1/production-plans?tenantId=&date=today&shift=current`
- If no plan found for current shift: show informational empty state "No plan found for this shift"
- Display plan header: Date, Shift badge (colour-coded), Status, Batch Count

### FE-FLOOR-3 — Work Order Cards
- Grid of cards, one per work order in the plan
- Each card shows:
  - Product name (prominent)
  - Status badge (colour-coded with icon)
  - Target qty + unit
- Card is clickable → opens WOPanel

### FE-FLOOR-4 — WO Panel (side panel, `WOPanel` component)
- Fixed position: right side, full height, width = 384px (`w-96`)
- Semi-transparent dark overlay behind panel
- Header: product name + X close button
- Two tabs: **Technology Steps** | **Recipe**
- Footer: action buttons based on WO status

#### Technology Steps Tab
- List of steps fetched from `GET /v1/technology-steps?tenantId=&recipeId=`
- Each step shows: number circle, name, activities description, instruments badge, duration badge, temperature badge
- Each step has a checkbox
- Checkbox state persisted to `localStorage` at key `step_{workOrderId}_{stepNumber}` (`'1'` / `'0'`)
- Checkboxes are independent per work order (different WOs for same product have separate checkbox state)
- Steps loaded once and cached in `stepsCache.current[recipeId]` — no re-fetch on panel close/reopen

#### Recipe Tab
- List of ingredients: name, `{perBatch × batchCount}` qty, unit
- Fetched from product's active recipe via `GET /v1/recipes?tenantId=&productId=`
- Cached in `recipeCache.current[productId]`

#### Footer Action Buttons
| WO Status | Available Actions |
|-----------|------------------|
| PLANNED | [▶ Start Production] → PUT status to IN_PROGRESS |
| IN_PROGRESS | [✓ Complete] + [✕ Cancel] |
| COMPLETED / CANCELLED | No action buttons, read-only |

- After action: refresh plan from server, repoint `selectedWo` to updated WO object, do not close panel

### FE-FLOOR-5 — Caching Rules
```
recipeCache = useRef<Record<productId, RecipeDetail>>
stepsCache  = useRef<Record<recipeId, TechnologyStep[]>>
```
Cache is populated on first WO open. Subsequent opens of the same WO skip the network call.
Cache lives for the lifetime of the page component (reset on full page reload only).

### FE-FLOOR-6 — Step Confirmation Persistence
Step checkbox state must survive panel close, WO card click elsewhere, and page refresh.
Use `localStorage` (not React state) as the persistence layer for checkbox values.

---

## 9. Screen: Recipes & Technology Steps (`/recipes`) <a name="recipes"></a>

### FE-REC-1 — Role Access
Admin, Technologist: full CRUD. Others: no access (hidden from nav).

### FE-REC-2 — Recipe List
- Table: Version, Product, Department, Status badge, Ingredient count, Step count, Actions
- Status badge: DRAFT (grey), ACTIVE (green), ARCHIVED (red)
- Filters: product search, status filter

### FE-REC-3 — Expandable Recipe Row
Click row to expand. Two tabs:

**Ingredients Tab**
- Table: Item Name, Qty, Unit, Unit Mode, Waste %, Purchase Unit, Conversion Ratio
- Production Notes panel below table
- Add Ingredient button (admin/technologist)

**Technology Steps Tab**
- Ordered list of process steps (stepNumber ascending)
- Each step: number circle (blue), name, activities text, instruments badge, duration badge (minutes), temperature badge (°C)
- Edit (✏) button + Delete (×) button per step (admin/technologist only)
- "Add Step" button at bottom of list

### FE-REC-4 — Recipe Header
Collapsed row header shows:
`{name} v{version} · {STATUS} · {X} ingr · {Y} steps`

### FE-REC-5 — Create Recipe (modal)
| Field | Type | Validation |
|-------|------|-----------|
| Product | select from product list | required |
| Department | select | required |
| Batch Size | number | required, > 0 |
| Expected Yield | number | required |
| Yield Unit | text | required |
| Production Notes | textarea | optional |

On submit: `POST /v1/recipes?tenantId=` → creates DRAFT

### FE-REC-6 — Activate Recipe (button on DRAFT recipe)
- "Activate" button visible on DRAFT recipes (admin/technologist)
- Confirmation dialog: "Activating this version will archive the current active recipe for this product."
- Calls `POST /v1/recipes/{id}/activate?tenantId=`

### FE-REC-7 — Technology Step Modal (create / edit)
| Field | Type | Validation |
|-------|------|-----------|
| Step Number | number | required, unique within recipe |
| Step Name | text (max 200) | required |
| Activities | textarea (max 2000) | optional |
| Instruments | text (max 500) | optional |
| Duration (min) | number | optional |
| Temperature (°C) | number | optional |

Create: `POST /v1/technology-steps?tenantId=` with `{recipeId, stepNumber, name, activities, instruments, durationMinutes, temperatureCelsius}`
Edit: `PUT /v1/technology-steps/{stepId}?tenantId=`
Delete: `DELETE /v1/technology-steps/{stepId}?tenantId=` with confirmation

### FE-REC-8 — Steps Caching
Steps cached in `stepsRef.current[recipeId]`. After create/edit/delete: call `loadSteps(recipeId, force=true)` to invalidate and reload.

---

## 10. Screen: Products (`/products`) <a name="products"></a>

### FE-PROD-1 — Role Access
Admin, Technologist: full CRUD. Management: read-only.

### FE-PROD-2 — Product List
- Table: Name, Department, Sale Unit (PIECE/WEIGHT/BOTH), Price, Status (ACTIVE/INACTIVE), Active Recipe Version, Actions
- Filter: department, status

### FE-PROD-3 — Create / Edit Product (modal)
| Field | Type | Validation |
|-------|------|-----------|
| Name | text | required |
| Department | select | required |
| Sale Unit | select: PIECE / WEIGHT / BOTH | required |
| Price per Unit | decimal | required, > 0 |
| VAT Rate % | number | optional |
| Description | textarea | optional |

Create: `POST /v1/products?tenantId=`
Edit: `PUT /v1/products/{id}?tenantId=`

### FE-PROD-4 — Active Recipe Link
- Product row shows the active recipe version (if any)
- Clicking "View Recipe" links to `/recipes` with the product pre-filtered

---

## 11. Screen: Departments (`/departments`) <a name="departments"></a>

### FE-DEPT-1 — Role Access
Admin only.

### FE-DEPT-2 — Department List
- Table: Name, Lead Time (hours), Warehouse Mode (SHARED/ISOLATED), Product Types, Staff Count, Actions

### FE-DEPT-3 — Create / Edit Department (modal)
| Field | Type | Validation |
|-------|------|-----------|
| Name | text | required |
| Lead Time (hours) | number | required, ≥ 0 |
| Warehouse Mode | select: SHARED / ISOLATED | required |
| Description | textarea | optional |

Create: `POST /v1/departments?tenantId=`
Edit: `PUT /v1/departments/{id}?tenantId=`

---

## 12. Screen: Inventory (`/inventory`) <a name="inventory"></a>
**R1 scope — this screen is NOT yet implemented. Defined here for planning.**

### FE-INV-1 — Role Access
Admin, Management (read), Warehouse (full).

### FE-INV-2 — Stock Level View
- Table: Item Name, Location/Department, On-Hand Qty, Unit, Last Receipt Date, FIFO Cost per Unit, Total Value, Alert (if below min threshold)
- Filter: department/site, item search, alert-only toggle

### FE-INV-3 — Receive Lot (modal)
Triggered from "Receive Stock" button (warehouse role):
| Field | Type | Validation |
|-------|------|-----------|
| Item | searchable select | required |
| Quantity | number | required, > 0 |
| Unit | text (auto-filled from item) | |
| Cost per Unit | decimal | required |
| Currency | select | required |
| Exchange Rate to Main Currency | decimal | required if currency ≠ main |
| Supplier Reference | text | optional |
| Lot Notes | textarea | optional |

On submit: `POST /v1/inventory/receive?tenantId=` → `ReceiveLotCommand`

### FE-INV-4 — Inventory Adjustment (modal)
| Field | Type |
|-------|------|
| Item | searchable select |
| Adjustment Qty (positive = add, negative = reduce) | number |
| Reason Code | select (WASTE / SPOILAGE / COUNT_CORRECTION / OTHER) |
| Notes | textarea |

On submit: `POST /v1/inventory/adjust?tenantId=`

### FE-INV-5 — Transfer (modal)
| Field | Type |
|-------|------|
| Item | searchable select |
| From Location | select |
| To Location | select |
| Quantity | number |

On submit: `POST /v1/inventory/transfer?tenantId=`

### FE-INV-6 — Lot Detail (expand row)
Show FIFO cost layers per lot: lotId, received date, original qty, remaining qty, unit cost.

---

## 13. Screen: POS (`/pos`) <a name="pos"></a>
**R1 scope — NOT yet implemented.**

### FE-POS-1 — Role Access
Admin, Cashier.

### FE-POS-2 — POS Session Layout
- Left panel: product grid (searchable, filterable by department)
- Right panel: current transaction (items added, qty controls, price, subtotal)
- Bottom of right panel: Payment section

### FE-POS-3 — Add to Transaction
- Click product card → adds 1 unit to transaction
- Qty +/- buttons in transaction panel
- Remove line × button

### FE-POS-4 — Payment
- Select payment method: CASH / CARD
- Cash: enter received amount → system shows change
- Card: enter terminal confirmation (reference number)
- Apply loyalty redemption (R2 — not in R1)
- "Complete Sale" → `POST /v1/pos/sales?tenantId=`

### FE-POS-5 — Receipt
- After sale: show receipt modal (product lines, total, payment method, cashier, timestamp)
- Print button (browser print)
- "New Sale" resets transaction panel

### FE-POS-6 — End of Day Reconciliation
- Button: "End of Day" → shows summary: total cash sales, total card sales, total refunds, net, expected cash in drawer
- Calls `POST /v1/pos/reconcile?tenantId=`

---

## 14. Screen: Reports & Technologist View (`/reports`, `/technologist`) <a name="reports"></a>
**R1 scope — basic reports. Full constructor is R2.**

### FE-RPT-1 — Role Access
Admin, Management, Finance: `/reports`. Admin, Technologist: `/technologist`.

### FE-RPT-2 — R1 Standard Reports (`/reports`)
Available reports (tabbed or dropdown):
| Report | Description | FRs covered |
|--------|-------------|-------------|
| Production Summary | Plan vs actual by product by period | FR-3.7 |
| Material Consumption | Planned vs actual ingredients per batch | FR-9.4 |
| Inventory Valuation | On-hand stock at FIFO cost | FR-9.5 |
| Revenue Summary | Orders total by day/week/month | FR-10.1 |
| Cost per Batch | FIFO material cost per closed batch | FR-9.2, FR-9.3 |

Common controls per report: date range picker, department filter, Export CSV button.

### FE-RPT-3 — Technologist View (`/technologist`)
- Per-product: active recipe version, ingredient list with costs (from last received lots), yield, waste analysis
- Planned vs actual variance panel per recent batch
- Read-only. No edit actions.

---

## 15. Screen: Admin Panel (`/admin`) <a name="admin"></a>
**R1 scope — user management + system config.**

### FE-ADMIN-1 — Role Access
Admin only.

### FE-ADMIN-2 — Tabs
1. **Users** — manage staff accounts
2. **Roles & Permissions** — view role definitions (edit in R2)
3. **System Config** — order cutoff time, rush premium %, tenant settings

### FE-ADMIN-3 — User Management
- Table: Username, Display Name, Roles, Status (ACTIVE/INACTIVE), Last Login, Actions
- Create User modal:
  | Field | Type |
  |-------|------|
  | Username | text, unique |
  | Display Name | text |
  | Password | password + confirm |
  | Role(s) | multi-select |
  | Department | select (optional) |
- Edit User: change roles, reset password, deactivate
- Calls `POST/PUT /v1/users?tenantId=`

### FE-ADMIN-4 — System Config
| Setting | Type | Default |
|---------|------|---------|
| Order Cutoff Time | time picker | 22:00 |
| Rush Order Premium % | decimal | 15 |
| Main Currency | select (ISO codes) | UZS |
| Tenant Display Name | text | — |

On save: `PUT /v1/config?tenantId=`

---

## 16. Global Non-Functional Requirements (FE) <a name="nfr"></a>

### Performance

| ID | Requirement |
|----|-------------|
| NFR-FE-1 | Initial page load (after login) shall reach interactive state within 3 seconds on a standard broadband connection. |
| NFR-FE-2 | List views with up to 200 records shall render within 1 second of data receipt. |
| NFR-FE-3 | No full-page re-renders on data mutation. Only the mutated component subtree re-renders. |
| NFR-FE-4 | API calls for the same resource within the same page session shall be deduplicated using useRef caches. |

### Usability

| ID | Requirement |
|----|-------------|
| NFR-FE-5 | Every async action has a visible loading state (spinner or disabled button). |
| NFR-FE-6 | Every async failure has a visible error message. Error messages shall be human-readable (not raw API error strings). |
| NFR-FE-7 | Destructive actions (delete, cancel, close period) require a confirmation dialog before API call. |
| NFR-FE-8 | Form submissions disable the submit button during the API call to prevent double-submit. |
| NFR-FE-9 | All tables support at minimum text-based search and status filtering. Pagination for lists that may exceed 50 rows. |
| NFR-FE-10 | The application shall be usable on screens ≥ 1024px wide. Tablet (768px+) support is a secondary goal for R1. Mobile layout is R2. |

### Security

| ID | Requirement |
|----|-------------|
| NFR-FE-11 | JWT token shall be stored in localStorage. Token shall be included in every API request via Authorization: Bearer header. |
| NFR-FE-12 | On 401 response from any API call (token expired): clear localStorage, clear AuthContext, redirect to `/login`. |
| NFR-FE-13 | Role-restricted nav items and action buttons shall not render in the DOM for users without the required role (not merely hidden with CSS). |
| NFR-FE-14 | No sensitive data (passwords, full tokens) shall appear in the browser console or URL parameters. |

### Maintainability

| ID | Requirement |
|----|-------------|
| NFR-FE-15 | All data interfaces (API request/response shapes) shall be defined as TypeScript `interface` types in a shared `frontend/lib/types.ts` file (or per-domain type files). No inline object typing for API shapes. |
| NFR-FE-16 | All API calls shall go through `frontend/lib/api.ts` (Axios wrapper). No raw `fetch()` calls in page components. |
| NFR-FE-17 | Reusable UI primitives (Button, Modal, Badge, Table, Input, Select, Spinner) shall live in `frontend/components/ui.tsx`. Page components shall use these primitives, not rebuild them. |
| NFR-FE-18 | No component shall exceed 600 lines. Extract sub-components and hook files when approaching this limit. |

---

## 17. R1 Coverage Analysis <a name="r1-coverage"></a>

### Mapping: BE R1 FRs → Required FE Screens

| BE Domain | Key R1 FRs | Required FE Screen | Status |
|-----------|-----------|-------------------|--------|
| Order Management | FR-1.1, FR-1.6–FR-1.17 | `/orders` — list, create, confirm, cancel, status transitions | ✅ Complete (status filter, customer search, rush order, cancel with reason, full advance flow) |
| Production Planning | FR-3.1–FR-3.10 | `/production-plans` — create plan, generate WOs, approve, work order management | ⚠️ Partially built (create + generate + list WOs exist; approve + full transitions missing) |
| Recipe & Product | FR-4.1–FR-4.9 | `/recipes` — CRUD recipes + ingredients + tech steps. `/products` — CRUD products | ⚠️ Recipes page exists with ingredients + steps. Products page exists. Missing: ingredient CRUD in UI, recipe activate button, unit mode selection. |
| Inventory | FR-5.1–FR-5.11 | `/inventory` — receive stock, view stock, adjust, transfer | ❌ Not implemented |
| Bookkeeping / Cost | FR-9.1–FR-9.7 | `/reports` — cost per batch, inventory valuation. No separate bookkeeping UI in R1. | ❌ Not implemented |
| POS | FR-8.1–FR-8.6 | `/pos` — sale screen, receipt, end-of-day reconciliation | ❌ Not implemented |
| Reporting & Dashboard | FR-10.1, FR-10.4–FR-10.7 | `/dashboard` — widgets. `/reports` — standard reports | ⚠️ Dashboard page exists (shell only, no real data widgets). Reports: `/technologist` exists (basic). Standard reports not built. |
| Config & Access | FR-11.1–FR-11.9 | `/admin` — user management, system config. `/departments` — CRUD. | ⚠️ Departments page exists. Admin page is a stub. User management not built. |
| Floor Worker View | FR-3.5, FR-3.6 (production execution) | `/floor` — shift plan, step-by-step WO execution | ✅ Implemented (plan for shift, WOPanel with tech steps + recipe, WO actions, localStorage step confirmations) |
| Authentication | NFR-G.4, FR-11.4 | `/login` | ✅ Implemented (login page, AuthShell, role-based redirect) |
| Role-based Nav | FR-11.3 | `AuthShell.tsx` nav sections | ✅ Implemented (role-gated nav sections, floor auto-redirect) |

---

### Summary Coverage Table

| Screen | Built? | Completeness | Blocking R1? |
|--------|--------|-------------|-------------|
| Login `/login` | ✅ Yes | ~90% (missing: auto-redirect on token refresh, 401 handler) | No (works for dev) |
| AuthShell nav + roles | ✅ Yes | ~80% (missing: finance/warehouse/cashier roles, full role set) | No |
| Dashboard `/dashboard` | ⚠️ Shell only | ~15% (no real data widgets) | Yes |
| Orders `/orders` | ✅ Yes | ~95% (status filter, rush order, cancel+reason, full status advance; missing: date-range filter, real customer entity) | No |
| Production Plans `/production-plans` | ⚠️ Partial | ~55% (create/generate/list WOs work; approve/transitions incomplete; WO actions incomplete) | Yes |
| Floor `/floor` | ✅ Yes | ~85% (core flow complete; missing: actual yield input on complete, real API for plan fetch by shift) | No for floor workers |
| Recipes `/recipes` | ⚠️ Partial | ~65% (view + expand + tech steps work; missing: ingredient CRUD from UI, activate button) | No |
| Products `/products` | ⚠️ Partial | ~50% (list + create/edit exist; missing: active recipe link, sale unit selector) | No |
| Departments `/departments` | ⚠️ Partial | ~60% (list + create/edit exist; missing: lead time, warehouse mode fields) | No |
| Inventory `/inventory` | ❌ No | 0% | Yes — stock receipt needed for production |
| POS `/pos` | ❌ No | 0% | Yes — R1 requires POS |
| Reports `/reports` | ❌ No | 0% (technologist view ~20%) | Yes |
| Admin Panel `/admin` | ⚠️ Stub | ~10% (page exists, no user management or config) | Yes |

---

### R1 Frontend Completion: ~40%

**Fully blocking R1 delivery (must build):**
1. `/inventory` — receive stock (required before production planning is useful)
2. `/pos` — POS sale + receipt + end-of-day
3. `/reports` — at minimum: production summary, inventory valuation, cost per batch
4. `/admin` — user management (needed to create real user accounts)
5. `/dashboard` — real data widgets (currently shell only)

**Partially built — needs completion:**
7. `/production-plans` — approve plan action, WO status transitions, yield recording on complete
8. `/recipes` — ingredient CRUD from UI, activate button
9. `/products` — sale unit + pricing fields
10. `/departments` — lead time + warehouse mode fields in create/edit form

**Solid — can ship as-is:**
- Login + Auth Shell
- Floor shift view (`/floor`)
- Orders (`/orders`)

---

*End of FE Requirements Document v1.0 — Pending Approval*
