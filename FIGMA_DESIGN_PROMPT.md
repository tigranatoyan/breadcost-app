# BreadCost — Figma Design System & Screen Specifications

> **Purpose**: Complete design prompt for building all BreadCost UI screens in Figma.
> **Product**: Multi-tenant bakery/confectionery ERP — production costing, order management, inventory, POS, delivery, customer portal, AI-powered features, and WhatsApp order intake.
> **Markets**: Armenia (primary), B2B wholesale + B2C retail bakeries.
> **Languages**: English, Armenian (Հայերեն) — all screens must support RTL-ready bilingual layout.

---

## 1. DESIGN SYSTEM FOUNDATIONS

### 1.1 Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `primary-600` | `#2563EB` | Primary buttons, active nav, links, focus rings |
| `primary-700` | `#1D4ED8` | Primary button hover |
| `primary-50` | `#EFF6FF` | Primary tint backgrounds |
| `gray-50` | `#F9FAFB` | Page background |
| `gray-100` | `#F3F4F6` | Secondary button bg, card borders |
| `gray-200` | `#E5E7EB` | Dividers, input borders |
| `gray-500` | `#6B7280` | Muted text, placeholders |
| `gray-700` | `#374151` | Secondary text |
| `gray-900` | `#111827` | Primary text |
| `slate-900` | `#0F172A` | Sidebar background |
| `slate-800` | `#1E293B` | Sidebar hover |
| `green-600` | `#16A34A` | Success buttons, ACTIVE/COMPLETED badges |
| `green-100` | `#DCFCE7` | Success badge bg |
| `red-600` | `#DC2626` | Danger buttons, error states |
| `red-100` | `#FEE2E2` | Error badge bg |
| `yellow-600` | `#CA8A04` | Warning states, PENDING badges |
| `yellow-100` | `#FEF9C3` | Warning badge bg |
| `blue-100` | `#DBEAFE` | Info badge bg |
| `amber-100` | `#FEF3C7` | IN_PRODUCTION / IN_PROGRESS badge bg |
| `purple-100` | `#F3E8FF` | APPROVED badge bg |
| `orange-100` | `#FFEDD5` | GENERATED badge bg |
| `teal-100` | `#CCFBF1` | READY / PUBLISHED badge bg |

### 1.2 Typography

| Style | Font | Weight | Size | Line Height |
|-------|------|--------|------|-------------|
| H1 Page Title | System (Inter / Apple System) | Bold (700) | 24px / `text-2xl` | 32px |
| H2 Section | System | Semibold (600) | 20px / `text-xl` | 28px |
| H3 Card Title | System | Semibold (600) | 18px / `text-lg` | 28px |
| Body | System | Normal (400) | 14px / `text-sm` | 20px |
| Small / Label | System | Medium (500) | 12px / `text-xs` | 16px |
| Table Header | System | Medium (500) | 12px / `text-xs` uppercase | 16px |
| Monospace | System mono | Normal | 12px / `text-xs` | 16px |

### 1.3 Spacing & Grid

- **Base unit**: 4px
- **Page padding**: 24px (`p-6`)
- **Card padding**: 16px (`p-4`)
- **Section gap**: 24px (`space-y-6`)
- **Form field gap**: 16px (`space-y-4`)
- **Sidebar width**: 256px (`w-64`)
- **Content max-width**: Fluid (full width minus sidebar)
- **Breakpoints**: sm 640px, md 768px, lg 1024px, xl 1280px, 2xl 1536px

### 1.4 Component Library

#### Buttons
| Variant | Background | Text | Border | Hover |
|---------|-----------|------|--------|-------|
| Primary | blue-600 | white | none | blue-700 |
| Secondary | gray-100 | gray-700 | none | gray-200 |
| Danger | red-600 | white | none | red-700 |
| Success | green-600 | white | none | green-700 |
| XS | same variants | same | none | same, smaller padding |
- All: `px-4 py-2 rounded font-medium`, disabled → `opacity-50 cursor-not-allowed`

#### Form Inputs
- Full-width, `border border-gray-300 rounded px-3 py-2`
- Focus: `ring-2 ring-blue-500 border-blue-500`
- Label above: `text-sm font-medium text-gray-700 mb-1`
- Hint text below: `text-xs text-gray-500 mt-1`

#### Status Badges
| Status | BG | Text |
|--------|-----|------|
| DRAFT | gray-100 | gray-800 |
| ACTIVE | green-100 | green-800 |
| CONFIRMED | blue-100 | blue-800 |
| IN_PRODUCTION | amber-100 | amber-800 |
| IN_PROGRESS | amber-100 | amber-800 |
| COMPLETED | green-100 | green-800 |
| CANCELLED | red-100 | red-800 |
| READY | teal-100 | teal-800 |
| DELIVERED | green-100 | green-800 |
| OUT_FOR_DELIVERY | blue-100 | blue-800 |
| PUBLISHED | teal-100 | teal-800 |
| APPROVED | purple-100 | purple-800 |
| GENERATED | orange-100 | orange-800 |
| OVERDUE | red-100 | red-800 |
| PENDING | yellow-100 | yellow-800 |
- Shape: `inline-flex px-2 py-0.5 rounded-full text-xs font-medium`

#### Modal
- Overlay: `bg-black/50 fixed inset-0 z-50`
- Panel: `bg-white rounded-lg shadow-xl max-w-lg mx-auto mt-20 p-6`
- Wide variant: `max-w-4xl`
- Title bar with close (×) button top-right
- Footer: right-aligned buttons `gap-2`

#### Table
- `min-w-full divide-y divide-gray-200`
- Header: `bg-gray-50 text-xs text-gray-500 uppercase tracking-wider`
- Row: `hover:bg-gray-50` with `divide-y`
- Empty state: centered gray text with icon

#### Cards
- `bg-white rounded-lg shadow-sm border border-gray-200 p-4`
- Stat card: icon left, value large, label small below

#### Alert / Success Banners
- Alert: `bg-red-50 border-l-4 border-red-400 text-red-800 p-4` with dismiss ×
- Success: `bg-green-50 border-l-4 border-green-400 text-green-800 p-4` with dismiss ×

---

## 2. LAYOUT STRUCTURE

### 2.1 App Shell (Authenticated)

```
┌──────────────────────────────────────────────────────────────────┐
│ ┌──────────┐ ┌──────────────────────────────────────────────────┐│
│ │           │ │  Top Bar: [breadcrumb]     [EN|HY] [User] [Out]││
│ │  SIDEBAR  │ ├──────────────────────────────────────────────────┤│
│ │  w-64     │ │                                                  ││
│ │  bg-slate │ │             PAGE CONTENT                         ││
│ │  -900     │ │             bg-gray-50                            ││
│ │           │ │             p-6                                   ││
│ │  Logo     │ │                                                  ││
│ │  Nav      │ │                                                  ││
│ │  Groups   │ │                                                  ││
│ │           │ │                                                  ││
│ └──────────┘ └──────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 Sidebar Navigation Groups (role-filtered)

| Group | Items | Roles |
|-------|-------|-------|
| **Overview** | Dashboard | All |
| **Sales** | Orders, POS, Customers, Invoices, Loyalty | Admin, Manager, Cashier, Finance |
| **Production** | Production Plans, Floor View, Recipes, Products, Departments | Admin, Manager, Production, Technologist |
| **Inventory** | Inventory, Suppliers | Admin, Manager, Warehouse |
| **Delivery** | Deliveries, Driver | Admin, Manager |
| **Finance** | Reports, Report Builder, Exchange Rates | Admin, Manager, Finance |
| **AI Tools** | AI WhatsApp, AI Suggestions, AI Pricing | Admin, Manager |
| **System** | Admin, Mobile Admin, Subscriptions | Admin |

### 2.3 Login Page (Standalone — no sidebar)

- Full-screen dark gradient background (`slate-900` → `slate-800`)
- Centered card (max-w-md), white, rounded-xl, shadow-2xl
- Logo/brand top, heading "BreadCost", subtitle "Bakery Management System"
- Fields: Username, Password
- "Sign In" primary button full-width
- Demo credentials hint at bottom (gray-400 text)
- Error alert below form on failure

---

## 3. SCREEN SPECIFICATIONS — BACK-OFFICE (Staff)

### Screen 01: Dashboard (`/dashboard`)
**Roles**: All authenticated users
**Purpose**: Operations overview with real-time KPIs

**Layout**:
```
┌─ Page Title: "Dashboard" ─────────────────────────────────────────┐
│                                                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │ 💰Revenue│ │ 📦 Open  │ │ 🏭Plans  │ │ 📊 Stock │             │
│  │ Today    │ │ Orders   │ │ Today    │ │ Value    │             │
│  │ ֏125,000 │ │ 12       │ │ 3        │ │ ֏890,000 │             │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘             │
│                                                                    │
│  ┌─ Delivery Timeline ──────────┐  ┌─ Stock Alerts ─────────────┐│
│  │ Gantt-style bar chart         │  │ ⚠ Flour — below reorder    ││
│  │ showing today's delivery      │  │ ⚠ Sugar — 3 days supply    ││
│  │ windows with status colors    │  │ ⚠ Butter — needs restock   ││
│  └──────────────────────────────┘  └─────────────────────────────┘│
│                                                                    │
│  ┌─ Production Floor ───────────┐  ┌─ Issues Detector ──────────┐│
│  │ Department cards with WO      │  │ Orders without plans        ││
│  │ progress bars                 │  │ Plans without active recipe ││
│  │ (completed/total counts)      │  │ Low stock items              ││
│  └──────────────────────────────┘  └─────────────────────────────┘│
│                                                                    │
│  ┌─ Getting Started Wizard (new tenants only) ──────────────────┐│
│  │ ✅ Create department  ✅ Add products  ◻ Add recipes  ◻ ...    ││
│  └──────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```
- **Auto-refresh**: 60-second polling
- **APIs**: `/v1/reports/revenue-summary`, `/v1/reports/orders-summary`, `/v1/reports/production-summary`, `/v1/inventory/alerts`

---

### Screen 02: Orders (`/orders`)
**Roles**: Admin, Manager, ProductionUser, FinanceUser, Viewer
**Purpose**: Full order lifecycle management

**Layout**:
- **Top bar**: Page title + "New Order" primary button
- **Filters row**: Status dropdown, Date range, Customer search input
- **Table columns**: Order #, Customer, Phone, Order Date, Delivery Date, Status (badge), Total (formatted ֏), Actions
- **Row actions**: Confirm, Start Production, Mark Ready, Deliver, Cancel (with reason modal)
- **Create Order modal**:
  - Customer name, phone, delivery date
  - Rush toggle (adds premium %)
  - Order lines table: Product dropdown, Qty, Unit Price — add/remove rows
  - Notes textarea
  - Total calculation at bottom

**Status flow**: DRAFT → CONFIRMED → IN_PRODUCTION → READY → OUT_FOR_DELIVERY → DELIVERED (or CANCELLED at any point)

---

### Screen 03: POS — Point of Sale (`/pos`)
**Roles**: Cashier, Admin
**Purpose**: Retail counter sales

**Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  POS                                    [Department filter] │
│                                                             │
│  ┌─ Product Grid (left 60%) ──┐  ┌─ Cart (right 40%) ────┐│
│  │ ┌─────┐ ┌─────┐ ┌─────┐   │  │ Item         Qty  Amt  ││
│  │ │Bread│ │Cake │ │Roll │   │  │ Baguette     ×2   ֏800 ││
│  │ │֏400 │ │֏1200│ │֏300 │   │  │ Croissant    ×1   ֏500 ││
│  │ └─────┘ └─────┘ └─────┘   │  │ ─────────────────────  ││
│  │ ┌─────┐ ┌─────┐ ┌─────┐   │  │ Subtotal:      ֏2,100 ││
│  │ │Crois│ │Donut│ │Tart │   │  │ VAT:             ֏420  ││
│  │ │֏500 │ │֏350 │ │֏800 │   │  │ Total:         ֏2,520  ││
│  │ └─────┘ └─────┘ └─────┘   │  │                         ││
│  └────────────────────────────┘  │ [💵 Cash] [💳 Card]     ││
│                                  └─────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```
- Product cards: name, price, click to add to cart
- Cart: line items with ±qty, remove (×), running total
- **Payment modal**: Amount tendered, change calculation (cash), receipt preview
- **Receipt modal**: Print-ready format (business name, items, totals, date, transaction #)
- **EOD Reconciliation**: Cash counted vs expected, card subtotal, overage/shortage

---

### Screen 04: Production Plans (`/production-plans`)
**Roles**: Admin, Manager, ProductionUser
**Purpose**: Plan production runs and manage work orders

**Layout**:
- **Top bar**: Title + "New Plan" button
- **Table**: Plan name, Date, Shift, Department, Status (badge), # Work Orders, Actions
- **Create modal**: Plan name, date picker, shift (morning/afternoon/night), notes
- **Plan detail expandable panel**:
  - Work orders list (product, qty, status, scheduled time)
  - Material requirements breakdown
  - Gantt-style schedule view (horizontal timeline bars)
  - WO time slot editing (drag or form)

**Status flow**: DRAFT → GENERATED → APPROVED → PUBLISHED → IN_PROGRESS → COMPLETED

**Action buttons per status**:
| Status | Actions |
|--------|---------|
| DRAFT | Generate |
| GENERATED | Approve (Admin/Manager only) |
| APPROVED | Publish |
| PUBLISHED | Start |
| IN_PROGRESS | Complete |

---

### Screen 05: Floor View (`/floor`)
**Roles**: ProductionUser (primary), Admin
**Purpose**: Shop floor worker's daily view — simplified, task-focused

**Layout**:
```
┌─ Today's Production Plan ────────────────────────────────────┐
│  Plan: "Morning Batch — March 10"    Status: PUBLISHED       │
│                                                               │
│  ┌─ WO Card ─────────────┐  ┌─ WO Card ─────────────┐       │
│  │ Baguette ×50           │  │ Croissant ×30          │       │
│  │ Status: IN_PROGRESS    │  │ Status: NOT_STARTED    │       │
│  │ [Start] [Complete]     │  │ [Start]                │       │
│  └────────────────────────┘  └────────────────────────┘       │
│                                                               │
│  ┌─ Side Panel (when WO selected) ──────────────────────────┐│
│  │  Tabs: [Technology Steps] [Recipe]                        ││
│  │                                                           ││
│  │  Technology Steps:                                        ││
│  │  ☑ 1. Mix dough (15 min)                                 ││
│  │  ☑ 2. First proof (40 min)                               ││
│  │  ☐ 3. Shape loaves (20 min)                              ││
│  │  ☐ 4. Second proof (30 min)                              ││
│  │  ☐ 5. Bake at 220°C (25 min)                             ││
│  │                                                           ││
│  │  Recipe:                                                  ││
│  │  Flour     — 12.5 kg/batch                                ││
│  │  Water     — 8.0 L/batch                                  ││
│  │  Yeast     — 0.25 kg/batch                                ││
│  │  Salt      — 0.2 kg/batch                                 ││
│  └───────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```
- Large touch-friendly cards (optimized for tablet on shop floor)
- Checkbox persistence for technology steps
- Color-coded WO status

---

### Screen 06: Recipes (`/recipes`)
**Roles**: Admin, Technologist
**Purpose**: Manage product recipes with versioning, ingredients, and technology steps

**Layout**:
- **Filters**: Department dropdown, Product search
- **Table** grouped by product: Recipe version, Status, Created date
- **Expandable row** with tabs:
  - **Ingredients tab**: Inline-editable table (Item, Qty, UoM, Waste %, Mode), add row
  - **Technology Steps tab**: Ordered steps (step #, name, duration, temp, instructions), add/edit modal
- **Create Recipe modal**: Select product, add initial ingredients
- **"Activate" button** on recipe row → sets this version as active

---

### Screen 07: Products (`/products`)
**Roles**: Admin, Manager, Technologist
**Purpose**: Product catalog management

**Layout**:
- **Top bar**: Title + search input + department filter + "New Product" button
- **Table**: Name, Department, Sale Unit, UoM, Price (֏), VAT %, Status, Actions
- **Create/Edit modal**: Name, department dropdown, sale unit, base UoM, price (minor units), VAT rate

---

### Screen 08: Departments (`/departments`)
**Roles**: Admin
**Purpose**: Manage bakery departments

**Layout**:
- **Top bar**: Title + "New Department" button
- **Table**: Name, Lead Time (hours), Warehouse Mode (SHARED/ISOLATED), Status, Actions
- **Create/Edit modal**: Name, lead time hours, warehouse mode dropdown

---

### Screen 09: Inventory (`/inventory`)
**Roles**: Admin, Manager, Warehouse
**Purpose**: Full inventory management with FIFO lot tracking

**Layout (tabs)**:

**Tab 1 — Stock Levels**:
- Table: Item, Location, Current Qty, UoM, Lot #, Reorder Point, Status
- **Receive Stock modal**: Item, qty, unit cost, currency (AMD/USD/EUR), FX rate, lot #, supplier
- **Transfer modal**: Item, from location, to location, qty
- **Adjust modal**: Item, qty (+/-), reason (WASTE/SPOILAGE/COUNT_CORRECTION/OTHER), notes
- **Lot detail expandable**: FIFO layers with dates, costs, remaining qty

**Tab 2 — Items**:
- Item master: Name, Type (INGREDIENT/PACKAGING/FG/BYPRODUCT/WIP), UoM, reorder point
- Create/Edit item modal

**Alerts panel** (sidebar or top banner): Items below reorder point with severity indicators

---

### Screen 10: Reports (`/reports`)
**Roles**: Admin, Manager, Finance
**Purpose**: Management reporting widgets

**Layout** (multi-widget dashboard):
- **Revenue Summary**: Today / This Week / This Month cards with ֏ values
- **Top Products**: Bar chart or ranked list with qty + revenue
- **Production Summary**: Plans completed, WOs finished, avg lead time
- **Orders Summary**: By status breakdown (pie or stacked bar)

---

### Screen 11: Technologist View (`/technologist`)
**Roles**: Technologist, Admin
**Purpose**: Recipe health overview and production analysis

**Layout**:
- **Department filter** at top
- **Product list** with health badges:
  - 🟢 Active recipe (version X)
  - 🔴 Missing recipe
  - 🟡 Draft only (no active version)
- **Expandable detail**: Ingredient list, yield info, cost per unit
- **Production plan analysis**: Which plans use which recipes, material overlap

---

### Screen 12: Admin Panel (`/admin`)
**Roles**: Admin
**Purpose**: System administration

**Layout (tabs)**:

**Tab 1 — Users**:
- User table: Username, Display Name, Role, Department, Status, Actions
- Create user modal: username, display name, password, role dropdown, department
- Reset password action
- Edit role/department inline

**Tab 2 — System**:
- Tenant Config form:
  - Order cutoff time (HH:mm)
  - Rush premium percentage
  - Main currency (AMD/USD/EUR)
- Master data links: Quick nav to Departments, Products, Items
- Demo accounts display (for development)

---

### Screen 13: Suppliers (`/suppliers`)
**Roles**: Admin, Manager, Warehouse
**Purpose**: Supplier management and procurement

**Layout (tabs)**:

**Tab 1 — Suppliers**:
- Supplier table: Name, Contact, Phone, Email, Status, Actions
- Create/Edit modal: Name, contact person, phone, email, address, notes
- **Catalog sub-panel** per supplier: Item, SKU, unit cost, lead days, min qty

**Tab 2 — Purchase Orders**:
- PO table: PO #, Supplier, Date, Status, Total, Actions
- Create PO: Select supplier, add lines (item from catalog, qty, price), notes
- **Approve PO** action (Manager+)
- **Receive Delivery** modal: Select PO lines, enter received qty, lot #
- **Export PO** button (Excel download)

**Tab 3 — API Config**:
- Supplier API integration: Endpoint URL, auth token, format, auto-send toggle
- Per-supplier configuration

---

### Screen 14: Deliveries (`/deliveries`)
**Roles**: Admin, Manager
**Purpose**: Manage delivery logistics

**Layout**:
- **Top bar**: Title + "New Delivery Run" button
- **Runs table**: Run #, Driver, Vehicle, Date, # Orders, Status, Actions
- **Create Run modal**: Driver name, vehicle plate, delivery date
- **Run detail panel**:
  - Assigned orders list with delivery status per order
  - **Assign Orders** modal: Select from confirmed/ready orders
  - **Manifest view**: Printable delivery manifest (customer, address, items, notes)
  - Per-order actions: Complete ✓, Fail ✗, Redeliver ↻, Waive charge

---

### Screen 15: Invoices (`/invoices`)
**Roles**: Admin, Finance
**Purpose**: B2B invoicing and credit management

**Layout (tabs)**:

**Tab 1 — Invoices**:
- Invoice table: Invoice #, Customer, Issue Date, Due Date, Amount, Status, Actions
- Status filter: DRAFT / ISSUED / PAID / OVERDUE
- **Invoice detail panel**: Line items (product, qty, unit price, total), payment history
- Actions: Issue, Record Payment (amount, method, date), Mark Overdue

**Tab 2 — Discounts**:
- Customer discount rules table: Customer, Product/Category, Discount %, Valid period
- Create discount rule modal
- **Credit management**: Per-customer credit limit, current balance, credit check result

---

### Screen 16: Customers (`/customers`)
**Roles**: Admin, Manager
**Purpose**: B2B customer management (admin side)

**Layout (tabs)**:

**Tab 1 — Customers**:
- Customer table: Name, Contact, Phone, Email, Type (Retail/Wholesale/HoReCa), Status
- Register customer modal: Business name, contact person, phone, email, address, type
- View/edit customer profile

**Tab 2 — Catalog**:
- Customer-facing product catalog preview (read-only view of `/v2/products`)
- Shows what customers see: name, description, price, availability

**Tab 3 — Orders**:
- Customer orders placed via portal: Order #, Customer, Date, Status, Total
- View order detail (lines placed through customer portal)

---

### Screen 17: Loyalty Program (`/loyalty`)
**Roles**: Admin, Manager
**Purpose**: Customer loyalty points management

**Layout (tabs)**:

**Tab 1 — Tiers**:
- Tier table: Name (Bronze/Silver/Gold/Platinum), Min Points, Discount %, Perks
- Create/Edit tier modal

**Tab 2 — Balances**:
- Customer search + balance lookup
- Customer loyalty card: Current tier, points balance, tier progress bar, next tier threshold

**Tab 3 — History**:
- Transaction log: Customer, Type (AWARD/REDEEM), Points, Reference, Date
- **Award Points modal**: Select customer, points amount, reason
- **Redeem Points modal**: Select customer, points to redeem, apply discount

---

### Screen 18: Report Builder (`/report-builder`)
**Roles**: Admin, Finance
**Purpose**: Custom report composition from KPI blocks

**Layout (tabs)**:

**Tab 1 — Reports**:
- Custom report list: Name, Created, # Blocks, Actions
- Create report: Name, description, select KPI blocks from catalog
- **Run report**: Rendered results panel with selected KPI data
- Export to PDF/Excel

**Tab 2 — Catalog**:
- KPI block catalog grid: Block name, category, description, preview
- Categories: Revenue, Production, Inventory, Orders, Customers
- Each block: clickable to preview sample output

---

### Screen 19: Subscriptions (`/subscriptions`)
**Roles**: Admin (platform admin / super-admin)
**Purpose**: SaaS subscription tier management

**Layout (tabs)**:

**Tab 1 — Tiers**:
- Tier cards or table: Free / Starter / Professional / Enterprise
- Per tier: Monthly price, max users, max departments, feature list
- Feature flags: AI features, WhatsApp, Custom reports, API access, Multi-location

**Tab 2 — Assignment**:
- Tenant table: Tenant ID, Current tier, Assigned date
- Assign/change tier modal
- Feature check: Verify if tenant has access to specific feature key

---

### Screen 20: Exchange Rates (`/exchange-rates`)
**Roles**: Admin, Finance
**Purpose**: Currency management for multi-currency procurement

**Layout**:
- **Add Rate**: Base currency, target currency, rate, effective date
- **Rates table**: Base, Target, Rate, Date, Source (manual/API)
- **Lookup**: Currency code + date → historical rate
- **Converter**: Amount + from currency + to currency → converted value
- **Fetch External** button: Pull latest rates from central bank API

---

## 4. SCREEN SPECIFICATIONS — AI & AUTOMATION

### Screen 21: AI WhatsApp Conversations (`/ai-whatsapp`)
**Roles**: Admin, Manager
**Purpose**: Monitor AI-powered WhatsApp order intake

**Layout**:
```
┌─ AI WhatsApp ────────────────────────────────────────────────────┐
│  Tabs: [All Conversations] [Escalated]                           │
│                                                                   │
│  ┌─ Conversation List (left 35%) ───┐ ┌─ Chat Detail (right) ──┐│
│  │ 🟢 Armen's Bakery   10:32 AM    │ │ Customer: Armen's       ││
│  │    "I need 50 baguettes for..."  │ │ Phone: +374-94-123456   ││
│  │ 🟡 Café Central      09:15 AM   │ │ Status: 🟢 Active       ││
│  │    "Can I change tomorrow's..."  │ │                          ││
│  │ 🔴 Hotel Grand       08:45 AM   │ │ ┌─ Messages ──────────┐ ││
│  │    ⚠ ESCALATED                   │ │ │ 🤖 AI: "Hello! I    │ ││
│  │                                   │ │ │ can help you place  │ ││
│  │                                   │ │ │ an order..."        │ ││
│  │                                   │ │ │                      │ ││
│  │                                   │ │ │ 👤 Customer:        │ ││
│  │                                   │ │ │ "50 baguettes and   │ ││
│  │                                   │ │ │ 20 croissants for   │ ││
│  │                                   │ │ │ tomorrow morning"   │ ││
│  │                                   │ │ │                      │ ││
│  │                                   │ │ │ 🤖 AI: "I've noted  │ ││
│  │                                   │ │ │ your order..."      │ ││
│  │                                   │ │ └────────────────────┘ ││
│  │                                   │ │                          ││
│  │                                   │ │ ┌─ Draft Order ───────┐ ││
│  │                                   │ │ │ Baguette    ×50     │ ││
│  │                                   │ │ │ Croissant   ×20     │ ││
│  │                                   │ │ │ Delivery: Mar 11    │ ││
│  │                                   │ │ │ [Confirm] [Edit]    │ ││
│  │                                   │ │ └─────────────────────┘ ││
│  │                                   │ │                          ││
│  │                                   │ │ [Resolve Escalation]    ││
│  └───────────────────────────────────┘ └─────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

**Key interactions**:
- Conversation list with status indicators (🟢 active, 🟡 pending, 🔴 escalated)
- Message thread: alternating AI bot messages and customer messages (WhatsApp-style bubbles)
- AI-extracted draft orders displayed below chat with line items
- "Confirm as Order" converts draft → real order in `/v1/orders`
- "Resolve Escalation" for human-intervened conversations
- Unread count badges on escalated tab

---

### Screen 22: AI Suggestions (`/ai-suggestions`)
**Roles**: Admin, Manager
**Purpose**: AI-powered inventory and production recommendations

**Layout (tabs)**:

**Tab 1 — Replenishment**:
- Suggestion cards: Item, Current stock, Recommended qty, Estimated cost, Confidence %
- Confidence bar (green/yellow/red)
- Actions: "Create PO" (auto-fills purchase order), "Dismiss"
- "Generate New Suggestions" button

**Tab 2 — Demand Forecast**:
- Product forecast table: Product, Predicted demand (units), Confidence %, Period
- Trend chart: Historical vs predicted (line chart placeholder)
- Date range selector

**Tab 3 — Production**:
- Suggested production quantities per product/department
- Based on: open orders + forecast demand - current stock
- Actions: "Create Plan" (auto-fills production plan), "Dismiss"

---

### Screen 23: AI Pricing & Anomalies (`/ai-pricing`)
**Roles**: Admin, Manager, Finance
**Purpose**: AI-driven price optimization and anomaly detection

**Layout (tabs)**:

**Tab 1 — Pricing Suggestions**:
- Table: Product, Current Price, Suggested Price, Change %, Reason, Confidence
- Color-coded: green (price increase), red (price decrease)
- Actions per row: Accept (updates product price), Dismiss
- Filter: Pending / Accepted / Dismissed

**Tab 2 — Anomalies**:
- Alert cards with severity badge: 🔴 Critical, 🟡 Warning, 🔵 Info
- Alert detail: Type (cost spike, revenue drop, margin erosion), Description, Metric value, Expected range
- Actions: Acknowledge, Dismiss
- Timeline: when anomaly was detected

---

## 5. SCREEN SPECIFICATIONS — DRIVER & MOBILE

### Screen 24: Driver Management (`/driver`)
**Roles**: Admin, Manager
**Purpose**: Monitor driver sessions and delivery execution

**Layout (tabs)**:

**Tab 1 — Active Sessions**:
- Session cards: Driver name, Vehicle, Start time, Current location (lat/lng), # Stops remaining
- Session detail: Live manifest with stop-by-stop status
  - Each stop: Customer, Address, Status (PENDING/ARRIVED/COMPLETED/FAILED), ETA
- GPS coordinate display (map placeholder for future)
- "End Session" action

**Tab 2 — Packaging**:
- Packaging confirmation table: Run #, Order #, Expected items, Confirmed items, Discrepancy
- Discrepancy highlighting (red when confirmed ≠ expected)
- Driver notes on discrepancies

**Tab 3 — Payments**:
- Payment log: Order #, Session, Amount, Method (CASH/CARD/TRANSFER), Status, Time
- Totals by session: Cash collected, Card total, Outstanding amount
- Per-order payment detail

---

### Screen 25: Mobile Admin (`/mobile-admin`)
**Roles**: Admin
**Purpose**: Manage registered mobile devices and push notifications

**Layout (tabs)**:

**Tab 1 — Devices**:
- Device table: Customer, Device name, Platform (iOS/Android), Token (masked), Registered date
- Remove device action (with confirmation)
- Device count per customer

**Tab 2 — Notifications**:
- Notification history table: Customer, Type, Title, Sent time, Status (SENT/FAILED)
- **Send Notification modal**: Customer selector, notification type dropdown (ORDER_STATUS/PROMOTION/SYSTEM), title, message body, reference ID
- Push notification types:
  - Order status update (auto-triggered)
  - Promotional message (manual)
  - System announcement (manual)

---

## 6. SCREEN SPECIFICATIONS — CUSTOMER PORTAL (Separate App)

> **Note**: The customer portal is a **separate front-end application** accessed by B2B/B2C customers (not staff). It should have its own simpler layout, branding, and authentication flow.

### 6.1 Customer Portal Layout

```
┌──────────────────────────────────────────────────────────────┐
│  ┌─ Header ───────────────────────────────────────────────┐  │
│  │ [Logo: BreadCost]    [🛒 Cart (3)]  [👤 Profile] [Out]│  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌─ Nav Bar ──────────────────────────────────────────────┐  │
│  │ [Catalog]  [My Orders]  [Loyalty]  [Contact]          │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌─ Content ──────────────────────────────────────────────┐  │
│  │                                                         │  │
│  │              PAGE CONTENT                               │  │
│  │              max-w-6xl mx-auto                          │  │
│  │                                                         │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─ Footer ───────────────────────────────────────────────┐  │
│  │ © 2026 BreadCost  |  Contact  |  Terms  |  Privacy     │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Screen CP-01: Customer Login / Register
**Purpose**: Customer authentication (separate from staff login)

**Login form**:
- Phone number (Armenian format: +374-XX-XXXXXX)
- Password
- "Sign In" button
- "Register" link → registration form
- "Forgot Password" link

**Register form**:
- Business Name
- Contact Person Name
- Phone Number (with country code)
- Email
- Password + Confirm Password
- Business Type: Retail Shop / Restaurant / Hotel / Café / Other
- Delivery Address
- "Register" button
- WhatsApp opt-in checkbox: "Receive order updates via WhatsApp"

---

### Screen CP-02: Product Catalog (Customer-Facing)
**Purpose**: Browse and order products

**Layout**:
```
┌─ Our Products ──────────────────────────────────────────────────┐
│  [Search: "bread..."]  [Category: All ▾]  [Sort: Popular ▾]    │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ 🖼 Photo │  │ 🖼 Photo │  │ 🖼 Photo │  │ 🖼 Photo │       │
│  │ Baguette │  │ Sourdough│  │ Croissant│  │ Danish   │       │
│  │ ֏400/pc  │  │ ֏600/pc  │  │ ֏500/pc  │  │ ֏450/pc  │       │
│  │ [Add🛒]  │  │ [Add🛒]  │  │ [Add🛒]  │  │ [Add🛒]  │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ 🖼 Photo │  │ 🖼 Photo │  │ 🖼 Photo │  │ 🖼 Photo │       │
│  │ Lavash   │  │ Eclair   │  │ Matnakash│  │ Gata     │       │
│  │ ֏200/pc  │  │ ֏700/pc  │  │ ֏350/pc  │  │ ֏800/pc  │       │
│  │ [Add🛒]  │  │ [Add🛒]  │  │ [Add🛒]  │  │ [Add🛒]  │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                  │
│  ← 1 2 3 ... →                                                  │
└─────────────────────────────────────────────────────────────────┘
```

- Product cards: Image placeholder, name, price, quick-add button
- Product detail view (click card): Full description, ingredients, allergens, nutrition, add to cart with qty
- Category/department filter
- Customer-specific pricing (if discount rules apply)

---

### Screen CP-03: Shopping Cart & Checkout
**Purpose**: Review cart and place order

**Layout**:
```
┌─ Your Cart ─────────────────────────────────────────────────────┐
│                                                                  │
│  ┌──────────────────────────────────────────────────────┐       │
│  │ 🖼 Baguette        ×50    ֏400   = ֏20,000  [−][+][🗑] │       │
│  │ 🖼 Croissant       ×30    ֏500   = ֏15,000  [−][+][🗑] │       │
│  │ 🖼 Sourdough Loaf  ×20    ֏600   = ֏12,000  [−][+][🗑] │       │
│  └──────────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌─ Order Details ──────────┐  ┌─ Summary ──────────────┐      │
│  │ Delivery Date: [picker]  │  │ Subtotal:    ֏47,000   │      │
│  │ Special Notes: [text]    │  │ Discount:    -֏2,350   │      │
│  │ Rush Delivery: [toggle]  │  │ (5% wholesale)          │      │
│  │ ⚡ +15% rush premium    │  │ Rush:        +֏6,698   │      │
│  │                          │  │ ───────────────────     │      │
│  │ Delivery Address:        │  │ Total:       ֏51,348   │      │
│  │ [saved address ▾]        │  │                         │      │
│  │                          │  │ [Place Order →]         │      │
│  └──────────────────────────┘  └─────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

- Qty adjustment (±) with subtotal recalculation
- Delivery date picker (respects order cutoff time)
- Rush delivery toggle with premium display
- Customer-specific discount auto-applied
- Delivery address from profile or editable
- Order notes / special instructions
- Loyalty points redemption option (if available)
- "Place Order" → confirmation screen

---

### Screen CP-04: Order Confirmation
**Purpose**: Post-checkout confirmation

**Layout**:
- ✅ Large success checkmark
- Order # displayed prominently
- Summary: items, delivery date, total
- Estimated delivery time
- "You will receive updates via WhatsApp at +374-XX-XXXXXX"
- [Track Order] [Continue Shopping] buttons

---

### Screen CP-05: My Orders
**Purpose**: Order history and tracking

**Layout**:
- **Tabs**: Active Orders | Order History
- Order cards:
  - Order #, date, delivery date, status badge, total
  - Expandable: line items, delivery driver (if assigned), tracking info
  - Status timeline: Placed → Confirmed → In Production → Ready → Out for Delivery → Delivered
  - Visual progress bar with current step highlighted
- **Reorder button**: Re-add all items from a past order to cart

---

### Screen CP-06: Loyalty Dashboard (Customer)
**Purpose**: Customer's loyalty program view

**Layout**:
```
┌─ My Loyalty ─────────────────────────────────────────────────────┐
│                                                                   │
│  ┌─ Tier Status ────────────────────────────────────────────────┐│
│  │      🥈 SILVER MEMBER                                        ││
│  │      Points: 2,450 / 5,000 for Gold                         ││
│  │      ████████████░░░░░░░░░ 49%                               ││
│  │      Discount: 5%  |  Perks: Free delivery on orders >֏50K  ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                   │
│  ┌─ Points History ─────────────────────────────────────────────┐│
│  │  +250 pts   Order #1042    Mar 8, 2026                       ││
│  │  -500 pts   Redeemed       Mar 5, 2026                       ││
│  │  +180 pts   Order #1038    Mar 3, 2026                       ││
│  │  +320 pts   Order #1035    Feb 28, 2026                      ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                   │
│  ┌─ Available Rewards ──┐  ┌─ Tier Benefits ───────────────────┐│
│  │ Redeem 1000 pts for  │  │ 🥉 Bronze: 2% discount            ││
│  │ ֏5,000 discount      │  │ 🥈 Silver: 5% + free delivery     ││
│  │ [Redeem at Checkout] │  │ 🥇 Gold: 8% + priority production  ││
│  └──────────────────────┘  │ 💎 Platinum: 12% + dedicated rep   ││
│                             └───────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

---

### Screen CP-07: Customer Profile
**Purpose**: Customer account settings

**Layout**:
- **Profile section**: Business name, contact person, phone, email (editable)
- **Delivery addresses**: List of saved addresses, add/edit/set default
- **Communication preferences**:
  - WhatsApp notifications: ON/OFF
  - Order confirmations: ON/OFF
  - Delivery updates: ON/OFF
  - Promotional messages: ON/OFF
- **Password change**: Current + new + confirm
- **Order preferences**: Default delivery time, preferred payment method

---

## 7. WHATSAPP COMMUNICATION FLOWS

### 7.1 WhatsApp Order Intake Flow

Design these as **chat mockup screens** showing the WhatsApp conversation UI:

**Flow 1 — New Order via WhatsApp**:
```
CUSTOMER → "Hi, I need 50 baguettes for tomorrow"
BOT      → "Hello! I'd be happy to help. Let me check availability.
            📋 Here's what I've prepared:
            • Baguette × 50 — ֏20,000
            Delivery: March 11, Morning
            Total: ֏20,000
            
            Would you like to confirm this order?"
CUSTOMER → "Yes, and add 20 croissants"
BOT      → "Updated! Your order:
            • Baguette × 50 — ֏20,000
            • Croissant × 20 — ֏10,000
            Delivery: March 11, Morning
            Total: ֏30,000
            
            ✅ Confirm  |  ✏️ Edit  |  ❌ Cancel"
CUSTOMER → "Confirm"
BOT      → "✅ Order #1052 confirmed!
            You'll receive updates here.
            Thank you, Armen's Bakery! 🍞"
```

**Flow 2 — Order Status Update (Automated)**:
```
BOT → "📦 Order #1052 Update:
        Status: OUT FOR DELIVERY
        Driver: Hovhannes
        ETA: 10:30 AM
        
        Track: [link]"
```

**Flow 3 — Escalation to Human**:
```
CUSTOMER → "I want to change my order from yesterday but
            the items are custom and I can't explain to the bot"
BOT      → "I understand you need help with a custom request.
            Let me connect you with our team.
            ⏳ A team member will respond shortly."
            
[ADMIN DASHBOARD shows: 🔴 ESCALATED conversation]
```

### 7.2 WhatsApp Notification Templates

Design template preview cards for:
1. **Order Confirmation**: "✅ Order #{number} confirmed. {items_summary}. Delivery: {date}."
2. **Production Started**: "🏭 Your order #{number} is now in production."
3. **Ready for Delivery**: "📦 Order #{number} is packed and ready for delivery."
4. **Out for Delivery**: "🚚 Order #{number} is on the way! Driver: {driver_name}, ETA: {time}."
5. **Delivered**: "✅ Order #{number} has been delivered. Thank you!"
6. **Payment Reminder**: "💳 Invoice #{number} for ֏{amount} is due on {date}."
7. **Stock Alert** (to owner): "⚠️ {item_name} stock is below reorder point. Current: {qty} {uom}."
8. **Promotional**: "🍞 New this week: {product_name}! Order now: [link]"

---

## 8. MOBILE APP SCREENS (Native iOS/Android)

> **Note**: These are for **future native mobile apps**. Design should prioritize touch-friendly interactions, large tap targets, and offline capability.

### Mobile 01: Customer Mobile App — Login
- Phone number + OTP verification (or password)
- Biometric authentication option (Face ID / fingerprint)
- Brand splash screen

### Mobile 02: Customer Mobile App — Catalog & Cart
- Vertical scrolling product cards (large images)
- Bottom tab bar: Home | Catalog | Cart (badge) | Orders | Profile
- Floating cart button with item count
- Product detail as bottom sheet

### Mobile 03: Customer Mobile App — Order Tracking
- Real-time map with driver location pin (when out for delivery)
- Status timeline (vertical stepper)
- Push notification deep-link into this screen
- Call driver button

### Mobile 04: Floor Worker App — Shift View
- Today's work orders as swipeable cards
- Large checkboxes for technology steps (finger-friendly)
- Timer per step
- "Complete WO" large button at bottom
- Minimal UI — optimized for flour-covered hands

### Mobile 05: Driver App — Delivery Session
- Stop-by-stop route list
- Current stop highlighted with "Navigate" (opens map app)
- Swipe to mark: Delivered ✓ / Failed ✗
- Photo capture for proof of delivery
- Cash/card payment collection form
- Packaging confirmation checklist

### Mobile 06: POS Tablet App
- Full-screen product grid (optimized for tablet landscape)
- Right-side cart panel
- Payment screen with numpad for cash amount
- Receipt display (show to customer or print via Bluetooth)
- Offline mode indicator + sync queue status

---

## 9. RESPONSIVE BREAKPOINTS

All back-office screens should be designed at these breakpoints:

| Breakpoint | Width | Layout |
|-----------|-------|--------|
| **Desktop** | 1280px+ | Full sidebar + content |
| **Laptop** | 1024px | Sidebar collapsible to icons, full content |
| **Tablet** | 768px | Sidebar hidden (hamburger menu), full-width content |
| **Mobile** | 375px | Stack all columns, hide non-essential columns in tables |

Customer portal breakpoints:
| Breakpoint | Width | Layout |
|-----------|-------|--------|
| **Desktop** | 1280px+ | Max-width container, 4-column product grid |
| **Tablet** | 768px | 2-column product grid, stacking cart on checkout |
| **Mobile** | 375px | Single column, bottom sheet cart, simplified nav |

---

## 10. INTERACTION PATTERNS

### Modals
- Create/Edit entities: centered modal with form, Cancel + Save buttons
- Confirmation dialogs: small centered modal, descriptive text, Cancel + Confirm
- Destructive actions: red Confirm button with warning text

### Tables
- Sortable columns (click header to toggle ↑↓)
- Hover row highlight (gray-50)
- Row click → expand detail panel or navigate
- Empty state: centered icon + "No {items} found" + CTA button
- Loading state: Spinner component centered

### Filters
- Inline filter bar above tables (dropdowns + search input)
- "Clear Filters" text button
- Active filter count badge

### Toast / Feedback
- Success: green banner at top of page (auto-dismiss 3s)
- Error: red banner at top (stays until dismissed)
- Loading: spinner overlay on buttons during API calls

### Navigation
- Sidebar items: icon + label, active item highlighted (slate-700 bg, white text)
- Breadcrumb path in top bar (e.g., "Inventory > Stock Levels")
- Role-based filtering: items invisible to unauthorized roles (not grayed out)

---

## 11. ICONOGRAPHY

Use a consistent icon set (recommend: Heroicons or Lucide). Key icons needed:

| Context | Icon |
|---------|------|
| Dashboard | `LayoutDashboard` |
| Orders | `ShoppingCart` |
| POS | `CreditCard` / `Receipt` |
| Production | `Factory` / `Wrench` |
| Floor | `HardHat` / `Clipboard` |
| Recipes | `BookOpen` / `ChefHat` |
| Products | `Package` |
| Departments | `Building2` |
| Inventory | `Warehouse` / `BoxesStacked` |
| Suppliers | `Truck` |
| Delivery | `TruckDelivery` |
| Invoices | `FileText` / `Receipt` |
| Customers | `Users` |
| Loyalty | `Star` / `Gift` |
| Reports | `BarChart3` |
| Admin | `Settings` / `Shield` |
| AI / WhatsApp | `MessageCircle` / `Bot` |
| AI Suggestions | `Sparkles` / `Lightbulb` |
| AI Pricing | `TrendingUp` / `DollarSign` |
| Driver | `MapPin` / `Navigation` |
| Mobile | `Smartphone` |
| Exchange Rates | `ArrowLeftRight` / `Currency` |
| Subscriptions | `CreditCard` / `Crown` |
| Notifications | `Bell` |
| Search | `Search` |
| Filter | `Filter` |
| Add | `Plus` |
| Edit | `Pencil` |
| Delete | `Trash2` |
| Close | `X` |
| Check | `Check` |
| Warning | `AlertTriangle` |
| Info | `Info` |
| User | `User` |
| Sign Out | `LogOut` |
| Language | `Globe` |

---

## 12. DATA VISUALIZATION

Charts needed across screens (use consistent chart style):

| Chart | Screen | Data |
|-------|--------|------|
| **KPI Cards** | Dashboard | Revenue, orders, plans, stock value |
| **Bar Chart** | Reports/Top Products | Product name × revenue/qty |
| **Line Chart** | AI Forecast | Historical vs predicted demand over time |
| **Pie/Donut** | Orders Summary | Orders by status distribution |
| **Progress Bar** | Dashboard/Floor | WO completion per department |
| **Gantt** | Production Plans | Work order timeline bars |
| **Timeline** | Dashboard/Deliveries | Delivery windows with status |
| **Tier Progress** | Customer Loyalty | Points toward next tier |
| **Sparkline** | AI Anomalies | Metric trend mini-chart |

Chart style: clean, minimal, blue-600 primary color, gray gridlines, no 3D effects.

---

## 13. ARMENIAN (ՀԱՅԵՐԵՆ) LOCALIZATION NOTES

- All UI text has Armenian translations (see `frontend/locales/hy.ts`)
- Armenian script: Մուտգործել (Login), Պատվdelays (Orders), Արադdelays (Products), etc.
- Currency symbol: ֏ (Armenian Dram) — appears before amounts: ֏125,000
- Date format: DD.MM.YYYY (Armenian standard)
- Phone format: +374-XX-XXXXXX
- Reading direction: LTR (Armenian is left-to-right)
- Ensure text containers can accommodate ~30% longer Armenian text vs English
- Language switcher: [EN] [HY] toggle in top bar

---

## 14. SCREEN INVENTORY CHECKLIST

### Back-Office Staff App (27 screens)
- [ ] Login
- [ ] Dashboard
- [ ] Orders (list + create modal + detail)
- [ ] POS (product grid + cart + payment + receipt + EOD)
- [ ] Production Plans (list + create + detail with Gantt)
- [ ] Floor View (WO cards + side panel)
- [ ] Recipes (grouped table + ingredients + tech steps)
- [ ] Products (list + CRUD modal)
- [ ] Departments (list + CRUD modal)
- [ ] Inventory (stock levels + items tabs + receive/transfer/adjust modals)
- [ ] Reports (multi-widget dashboard)
- [ ] Technologist View (health badges + detail)
- [ ] Admin (users + system config tabs)
- [ ] Suppliers (suppliers + PO + API config tabs)
- [ ] Deliveries (runs + manifest + per-order actions)
- [ ] Invoices (list + detail + payment + discounts)
- [ ] Customers (list + catalog preview + portal orders)
- [ ] Loyalty (tiers + balances + history)
- [ ] Report Builder (custom reports + KPI catalog)
- [ ] Subscriptions (tiers + assignment)
- [ ] Exchange Rates (rates + converter)
- [ ] AI WhatsApp (conversation list + chat + draft orders)
- [ ] AI Suggestions (replenishment + forecast + production)
- [ ] AI Pricing (pricing suggestions + anomalies)
- [ ] Driver Management (sessions + packaging + payments)
- [ ] Mobile Admin (devices + notifications)
- [ ] Error / 404 page

### Customer Portal (7 screens)
- [ ] Customer Login / Register
- [ ] Product Catalog (grid + detail)
- [ ] Shopping Cart & Checkout
- [ ] Order Confirmation
- [ ] My Orders (active + history + tracking)
- [ ] Loyalty Dashboard
- [ ] Customer Profile & Settings

### WhatsApp Mockups (3 flows)
- [ ] New Order Conversation
- [ ] Status Update Notifications
- [ ] Escalation to Human

### Notification Templates (8 templates)
- [ ] Order Confirmation
- [ ] Production Started
- [ ] Ready for Delivery
- [ ] Out for Delivery
- [ ] Delivered
- [ ] Payment Reminder
- [ ] Stock Alert
- [ ] Promotional

### Mobile App Screens (6 screens)
- [ ] Customer App — Login (OTP + biometric)
- [ ] Customer App — Catalog & Cart
- [ ] Customer App — Order Tracking (with map)
- [ ] Floor Worker App — Shift View
- [ ] Driver App — Delivery Session
- [ ] POS Tablet App

### Total: ~51 unique screen designs + 8 notification templates + 3 WhatsApp flows

---

## 15. FIGMA ORGANIZATION

### Suggested Page Structure
```
📁 BreadCost Design System
├── 🎨 Foundations (Colors, Typography, Spacing, Grid)
├── 🧱 Components (Buttons, Inputs, Tables, Modals, Badges, Cards, Charts)
├── 📐 Layouts (App Shell, Sidebar, Customer Portal Shell, Mobile Shell)
├── 📱 Back-Office Screens (27 frames, Desktop + Tablet)
├── 🛒 Customer Portal (7 frames, Desktop + Mobile)
├── 💬 WhatsApp Flows (3 conversation mockups)
├── 📲 Mobile App (6 frames, iPhone + Android)
├── 📨 Notification Templates (8 template cards)
├── 🔄 Prototyping Flows (login → dashboard, order lifecycle, customer checkout)
└── 📋 Handoff Specs (spacing, API mapping, interaction notes)
```

### Naming Convention
- Frames: `[Section] / [Screen Name] / [Breakpoint]`
  - Example: `Back-Office / Orders / Desktop`
  - Example: `Customer Portal / Catalog / Mobile`
- Components: `[Category] / [Component] / [Variant]`
  - Example: `Button / Primary / Default`
  - Example: `Badge / Status / CONFIRMED`
