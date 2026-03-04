# BreadCost R1 — GUI Test Plan (Frontend)
**Version:** 1.0 | **Date:** 2026-03-04 | **Tester:** _____________  
**Frontend URL:** http://localhost:3000 | **Backend URL:** http://localhost:8080  
**Stack:** Next.js 14 + React 18 + Tailwind CSS → Spring Boot 3.4.2 REST API

---

## Prerequisite: Starting the Application

```bash
# Terminal 1 — Backend (from project root)
mvn spring-boot:run
# Wait for "Started BreadCostApplication" message

# Terminal 2 — Frontend (from frontend/ folder)
cd frontend
npm run dev
# Wait for "Ready on http://localhost:3000"
```

Both must be running simultaneously. Backend seeds demo data on first start.

---

## Test Accounts

| Username | Password | FE Role | Default Landing Page |
|----------|----------|---------|---------------------|
| admin | admin | admin | /dashboard |
| production | production | floor | /floor |
| finance | finance | finance | /reports |
| viewer | viewer | viewer | /dashboard |
| cashier | cashier | cashier | /pos |
| warehouse | warehouse | warehouse | /inventory |
| technologist | technologist | technologist | /recipes |

**Tenant:** `tenant1` (hardcoded in frontend)

---

## How to Use This Document

Each test case describes:
- **What screen to open** in the browser
- **What buttons/fields to interact with**
- **What you should see on screen** (visual elements, data, messages)
- **Requirement mapping** to FE_REQUIREMENTS.md

Fill in the **Result** column as you go: ✅ Pass / ❌ Fail / ⏭ Skipped

---

## Suggested Execution Order

1. **Login** (Section 1) — establish sessions
2. **Admin Panel** (Section 10) — verify users, config
3. **Departments** (Section 9) — create departments needed later
4. **Products** (Section 8) — create products
5. **Recipes** (Section 7) — create recipes for products
6. **Items/Inventory** (Section 5) — receive raw materials
7. **Orders** (Section 3) — create and manage orders
8. **Production Plans** (Section 4) — plan production
9. **Production Floor** (Section 6) — execute work orders
10. **POS** (Section 11) — process sales
11. **Reports** (Section 12) — verify aggregated data
12. **Dashboard** (Section 2) — verify KPIs reflect all data
13. **Technologist** (Section 13) — verify analysis view
14. **Role Access** (Section 14) — multi-role verification

---

# Section 1: Login Screen (`/login`)

### TC-GUI-LOGIN-01 — Login Page Appearance
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-1, FE-LOGIN-2 |
| **Precondition** | Frontend running, not logged in (clear localStorage) |
| **Steps** | 1. Open http://localhost:3000/login |
| **Expected** | See: dark gradient background, 🍞 bread emoji logo, "BreadCost" title, white card with Username + Password fields (pre-filled with admin/admin), "Sign in" button, Demo Accounts quick-fill grid with 4 buttons (admin, production, finance, viewer) |
| **Result** | |

### TC-GUI-LOGIN-02 — Successful Login as Admin
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-3, FE-LOGIN-5 |
| **Precondition** | On login page |
| **Steps** | 1. Enter username: `admin`, password: `admin` 2. Click "Sign in" |
| **Expected** | Redirected to `/dashboard`. Sidebar visible with all sections: Dashboard, Orders, Production Plans, Inventory, POS, Reports, Floor, Technologist, Admin Panel, Departments, Products, Recipes |
| **Result** | |

### TC-GUI-LOGIN-03 — Demo Account Quick-Fill
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-2 |
| **Precondition** | On login page |
| **Steps** | 1. Click the "production" demo account button 2. Observe username/password fields auto-fill 3. Click "Sign in" |
| **Expected** | Fields fill with production/production. After sign in, redirected to `/floor` (floor worker default) |
| **Result** | |

### TC-GUI-LOGIN-04 — Invalid Credentials
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-4 |
| **Precondition** | On login page |
| **Steps** | 1. Enter username: `admin`, password: `wrong` 2. Click "Sign in" |
| **Expected** | Red error alert appears on the login page. NOT redirected. Username/password fields remain visible. |
| **Result** | |

### TC-GUI-LOGIN-05 — Already Logged In Redirect
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-5 |
| **Precondition** | Already logged in as admin |
| **Steps** | 1. Navigate to http://localhost:3000/login directly |
| **Expected** | Auto-redirected to `/dashboard` (not shown the login form again) |
| **Result** | |

### TC-GUI-LOGIN-06 — Unauthenticated Access Guard
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-6 |
| **Precondition** | Not logged in (open DevTools → Application → LocalStorage → delete `bc_token` and `bc_user`) |
| **Steps** | 1. Navigate to http://localhost:3000/orders |
| **Expected** | Redirected to `/login` |
| **Result** | |

### TC-GUI-LOGIN-07 — Logout
| Field | Value |
|-------|-------|
| **Requirement** | FE-LOGIN-7 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Click the logout button in the sidebar (or user area) |
| **Expected** | Redirected to `/login`. localStorage: `bc_token` and `bc_user` removed. Navigating to `/dashboard` redirects back to `/login` |
| **Result** | |

---

# Section 2: Dashboard (`/dashboard`)

### TC-GUI-DASH-01 — Dashboard Loads with KPIs
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-1, FE-DASH-2 |
| **Precondition** | Logged in as admin, some orders/plans/inventory exist |
| **Steps** | 1. Navigate to `/dashboard` |
| **Expected** | See 4 KPI cards at top: Running Revenue (number + currency), Open Orders (count), Today's Plans (count), Stock Value (number + currency). Each card is clickable and links to its section. |
| **Result** | |

### TC-GUI-DASH-02 — Getting Started Guide (Empty State)
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-3 |
| **Precondition** | Logged in as admin, zero departments exist (fresh DB or all deleted) |
| **Steps** | 1. Navigate to `/dashboard` |
| **Expected** | "Getting Started" section visible with ordered steps: create department, create product, create recipe, create order, create plan. Each step has a link to the relevant page. |
| **Result** | |

### TC-GUI-DASH-03 — Delivery Timeline
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-4 |
| **Precondition** | Logged in as admin, at least 2 orders with future delivery dates exist |
| **Steps** | 1. Navigate to `/dashboard` 2. Look at the Delivery Timeline section |
| **Expected** | Up to 8 upcoming orders sorted by delivery time. Each shows customer name, delivery countdown timer, rush badge (if rush order), conflict badge (if lead-time conflict). Data refreshes every 30 seconds. |
| **Result** | |

### TC-GUI-DASH-04 — Issues Detected Panel
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-5 |
| **Precondition** | Logged in as admin, create conditions: overdue order (past delivery), draft plan without WOs, item below min stock threshold |
| **Steps** | 1. Navigate to `/dashboard` |
| **Expected** | "Issues Detected" panel lists actionable warnings: overdue orders, lead-time conflicts, rush orders, draft plans without WOs, low stock alerts, confirmed orders without matching plan date, cancelled orders. Each issue is described with context. |
| **Result** | |

### TC-GUI-DASH-05 — Production Floor Section
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-6 |
| **Precondition** | Logged in as admin, at least 1 production plan exists |
| **Steps** | 1. Navigate to `/dashboard` 2. Scroll to "Production Floor" section |
| **Expected** | Up to 6 plan cards. Each shows plan date, shift, status badge, progress bar (WOs completed / total). |
| **Result** | |

### TC-GUI-DASH-06 — Role-Based Dashboard Access
| Field | Value |
|-------|-------|
| **Requirement** | FE-DASH-7 |
| **Precondition** | — |
| **Steps** | 1. Login as `cashier` → navigate to `/dashboard` 2. Login as `warehouse` → navigate to `/dashboard` 3. Login as `production` → navigate to `/dashboard` |
| **Expected** | Cashier is redirected to `/pos`, warehouse to `/inventory`, production(floor) to `/floor` — NOT shown the dashboard. Only admin/management/viewer/finance stay on dashboard. |
| **Result** | |

---

# Section 3: Orders (`/orders`)

### TC-GUI-ORD-01 — Orders Page Initial Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Click "Orders" in sidebar |
| **Expected** | "Orders" title, "+ New Order" button, status filter dropdown (ALL, DRAFT, CONFIRMED, etc.), customer search input, result count. Any existing orders are listed as expandable cards. |
| **Result** | |

### TC-GUI-ORD-02 — Create New Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-2, FE-ORD-3 |
| **Precondition** | At least 1 product exists. On Orders page. |
| **Steps** | 1. Click "+ New Order" 2. In modal: enter Customer Name "Test Bakery", set Requested Delivery to tomorrow 10:00, add notes "First test order" 3. In Order Lines: select a product, set Qty=10, Unit Price=5000 4. Click "+ Add Line" → add another product, Qty=5, Price=8000 5. Click "Create Order" |
| **Expected** | Modal closes. New order appears in the list with status: DRAFT, customer: "Test Bakery", 2 lines, total: 90000. Success message shown. |
| **Result** | |

### TC-GUI-ORD-03 — Create Rush Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-4 |
| **Precondition** | On Orders page, at least 1 product exists |
| **Steps** | 1. Click "+ New Order" 2. Fill customer name, delivery date 3. Check "Rush Order" checkbox 4. Enter Custom Rush Premium: 20 5. Add at least 1 order line 6. Click "Create Order" |
| **Expected** | Order created with rush badge visible on the card. When expanded, shows rush indicator. |
| **Result** | |

### TC-GUI-ORD-04 — Expand Order Details
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-5 |
| **Precondition** | At least 1 order exists |
| **Steps** | 1. Click on an order card to expand it |
| **Expected** | Expanded view shows: Order ID, placed date, delivery date + time, notes, lines table (product name, quantity, unit price, line total). Lead time conflict indicator if applicable. |
| **Result** | |

### TC-GUI-ORD-05 — Filter Orders by Status
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-6 |
| **Precondition** | Multiple orders in different statuses (DRAFT, CONFIRMED, etc.) |
| **Steps** | 1. Select "DRAFT" from the status filter dropdown 2. Observe list 3. Select "CONFIRMED" 4. Observe list 5. Select "ALL" |
| **Expected** | List filters to show only orders matching selected status. Result count updates. "ALL" shows everything. |
| **Result** | |

### TC-GUI-ORD-06 — Search Orders by Customer
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-6 |
| **Precondition** | Multiple orders with different customer names |
| **Steps** | 1. Type part of a customer name in the search input |
| **Expected** | Order list filters in real-time to show only matching customers. |
| **Result** | |

### TC-GUI-ORD-07 — Confirm Order (DRAFT → CONFIRMED)
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-7 |
| **Precondition** | A DRAFT order exists |
| **Steps** | 1. Find a DRAFT order 2. Click the "Confirm" button on the order card |
| **Expected** | Status badge changes to CONFIRMED. Confirm button disappears. Advance Status button appears. |
| **Result** | |

### TC-GUI-ORD-08 — Advance Order Status (CONFIRMED → IN_PRODUCTION → READY → OUT_FOR_DELIVERY → DELIVERED)
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-8 |
| **Precondition** | A CONFIRMED order exists |
| **Steps** | 1. Click "Advance Status" on a CONFIRMED order → should become IN_PRODUCTION 2. Click "Advance Status" again → READY 3. Again → OUT_FOR_DELIVERY 4. Again → DELIVERED |
| **Expected** | Each click advances to the next status. Badge updates. At DELIVERED, no more advance button. |
| **Result** | |

### TC-GUI-ORD-09 — Cancel Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-9 |
| **Precondition** | A DRAFT or CONFIRMED order exists |
| **Steps** | 1. Click "Cancel" button on the order card 2. In the Cancel modal: enter reason "Test cancellation" 3. Click confirm |
| **Expected** | Status changes to CANCELLED (red badge). Cancel/Confirm/Advance buttons no longer visible on this order. |
| **Result** | |

### TC-GUI-ORD-10 — Cannot Cancel a DELIVERED Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-ORD-10 |
| **Precondition** | A DELIVERED order exists |
| **Steps** | 1. Find the DELIVERED order 2. Look for Cancel button |
| **Expected** | No Cancel button shown for DELIVERED orders. |
| **Result** | |

---

# Section 4: Production Plans (`/production-plans`)

### TC-GUI-PP-01 — Production Plans Page Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Click "Production Plans" in sidebar |
| **Expected** | "Production Plans" title, "+ New Plan" button, status filter dropdown, date filter, result count. Existing plans listed as expandable cards. |
| **Result** | |

### TC-GUI-PP-02 — Create New Production Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-2 |
| **Precondition** | On Production Plans page |
| **Steps** | 1. Click "+ New Plan" 2. Set Plan Date to tomorrow 3. Select Shift: MORNING 4. Add Notes: "Test plan" 5. Click Create |
| **Expected** | Modal closes. New plan appears with status: DRAFT, tomorrow's date, MORNING shift badge. |
| **Result** | |

### TC-GUI-PP-03 — Generate Work Orders
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-3 |
| **Precondition** | DRAFT plan exists, at least 1 CONFIRMED order exists |
| **Steps** | 1. Find the DRAFT plan 2. Click "Generate Work Orders" button |
| **Expected** | Plan status changes to GENERATED (or stays DRAFT with WOs populated). Expanded view shows work orders table with product, department, quantity, status (PLANNED/PENDING). |
| **Result** | |

### TC-GUI-PP-04 — View Material Requirements
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-4 |
| **Precondition** | Plan with generated work orders exists, recipes exist for the products |
| **Steps** | 1. Expand the plan 2. Look for "Material Requirements" section |
| **Expected** | Cards showing ingredient name and quantity needed (in purchasing units). Calculated from recipe × work order quantities. |
| **Result** | |

### TC-GUI-PP-05 — View Schedule (Gantt Timeline)
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-5 |
| **Precondition** | Plan with work orders exists |
| **Steps** | 1. Expand the plan 2. Look for Schedule/Gantt section |
| **Expected** | Visual bar chart showing work order timing. Bars color-coded: critical (red), parallel (blue), sequential (green). Total lead time displayed. Legend explaining colors. |
| **Result** | |

### TC-GUI-PP-06 — Edit Work Order Schedule Offsets
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-6 |
| **Precondition** | Plan with schedule visible |
| **Steps** | 1. In the schedule view, find offset editor inputs for a work order 2. Change start offset or duration value 3. Observe timeline update |
| **Expected** | PATCH request sent. Gantt bars reposition to reflect new timing. |
| **Result** | |

### TC-GUI-PP-07 — Approve Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-7 |
| **Precondition** | A GENERATED or DRAFT plan with WOs |
| **Steps** | 1. Click "Approve" button on the plan |
| **Expected** | Plan status changes to APPROVED badge. |
| **Result** | |

### TC-GUI-PP-08 — Start Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-8 |
| **Precondition** | An APPROVED plan |
| **Steps** | 1. Click "Start" button on the plan |
| **Expected** | Plan status changes to IN_PROGRESS. |
| **Result** | |

### TC-GUI-PP-09 — Complete Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-9 |
| **Precondition** | An IN_PROGRESS plan |
| **Steps** | 1. Click "Complete" button on the plan |
| **Expected** | Plan status changes to COMPLETED. Action buttons disappear. |
| **Result** | |

### TC-GUI-PP-10 — Work Order Lifecycle (Start/Complete/Cancel)
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-10 |
| **Precondition** | Plan expanded, work orders visible |
| **Steps** | 1. Click "Start" on a PENDING/PLANNED work order → status becomes IN_PROGRESS/STARTED 2. Click "Complete" on a STARTED work order → status becomes COMPLETED 3. On another WO, click "Cancel" → status becomes CANCELLED |
| **Expected** | Each WO status updates in the table. Appropriate action buttons shown per WO status. |
| **Result** | |

### TC-GUI-PP-11 — Filter Plans by Status and Date
| Field | Value |
|-------|-------|
| **Requirement** | FE-PP-11 |
| **Precondition** | Multiple plans in different statuses |
| **Steps** | 1. Select "IN_PROGRESS" from status filter 2. Set a specific date in the date filter 3. Click "Clear Filters" |
| **Expected** | Plan list filters correctly. Count updates. Clear restores all plans. |
| **Result** | |

---

# Section 5: Inventory & Items (`/inventory`)

### TC-GUI-INV-01 — Stock Levels Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-1 |
| **Precondition** | Logged in as admin or warehouse, some inventory exists |
| **Steps** | 1. Click "Inventory" in sidebar 2. Ensure "Stock Levels" tab is active |
| **Expected** | Table with columns: Item, Type (colored badge), Location, Lot (truncated UUID), On Hand, Avg Cost, Total Value. Footer shows total valuation. Alert count badge in header if low stock items exist. |
| **Result** | |

### TC-GUI-INV-02 — Items Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-2 |
| **Precondition** | On Inventory page |
| **Steps** | 1. Click "Items" tab |
| **Expected** | Table with columns: Name, Type badge, UoM, Min Stock, Status (Active/Inactive badge), Description, Edit link. |
| **Result** | |

### TC-GUI-INV-03 — Receive Stock
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-3 |
| **Precondition** | On Stock Levels tab, at least 1 active item exists |
| **Steps** | 1. Click "Receive Stock" button 2. In modal: select Item "Flour", Quantity: 100, Cost per Unit: 5000, Supplier Reference: "SUP-001" 3. Click submit |
| **Expected** | Modal closes. Success message shown. New lot appears in stock table with qty=100, cost=5000, location=MAIN. Total valuation increases. |
| **Result** | |

### TC-GUI-INV-04 — Transfer Stock
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-4 |
| **Precondition** | At least 1 lot with on-hand qty > 0 |
| **Steps** | 1. Click "Transfer" button 2. In modal: select Item, enter From Location: "MAIN", To Location: "PROD-FLOOR", Quantity: 20 3. Click submit |
| **Expected** | Modal closes. Success message. Stock position table updates: source location qty decreases, new row appears for destination location (or qty increases if already exists). |
| **Result** | |

### TC-GUI-INV-05 — Create New Item
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-5 |
| **Precondition** | On Items tab |
| **Steps** | 1. Click "New Item" button 2. In modal: Name: "Vanilla Extract", Type: INGREDIENT, Base UoM: "ml", Min Stock Threshold: 50, Description: "Pure vanilla" 3. Click submit |
| **Expected** | Modal closes. New item appears in Items table with correct name, type, UoM, threshold, Active status. |
| **Result** | |

### TC-GUI-INV-06 — Edit Item
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-6 |
| **Precondition** | On Items tab, at least 1 item exists |
| **Steps** | 1. Click "Edit" link on an item 2. Change Min Stock Threshold to 200 3. Click Save |
| **Expected** | Modal closes. Item row updates with new threshold value. |
| **Result** | |

### TC-GUI-INV-07 — Filter by Type
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-7 |
| **Precondition** | Items of different types exist |
| **Steps** | 1. On Stock Levels tab, select "INGREDIENT" from Type dropdown 2. Observe table 3. Select "FG" 4. Clear filter |
| **Expected** | Table filters to show only items of selected type. |
| **Result** | |

### TC-GUI-INV-08 — Low Stock Highlight
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-8 |
| **Precondition** | Item has min stock threshold = 100, on hand = 20 |
| **Steps** | 1. On Stock Levels tab, check "Below threshold only" checkbox |
| **Expected** | Only items with on-hand < min threshold shown. Rows are red-highlighted. Alert badge count matches. |
| **Result** | |

### TC-GUI-INV-09 — Search Items
| Field | Value |
|-------|-------|
| **Requirement** | FE-INV-9 |
| **Precondition** | Multiple items exist |
| **Steps** | 1. Type "Flour" in the search input |
| **Expected** | Stock/Items table filters to show only items matching "Flour". |
| **Result** | |

---

# Section 6: Production Floor (`/floor`)

### TC-GUI-FLOOR-01 — Floor Page Load (Floor Worker)
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-1 |
| **Precondition** | Logged in as `production` (floor role) |
| **Steps** | 1. Auto-landed on `/floor` (or navigate there) |
| **Expected** | Shows today's production plans and any still IN_PROGRESS. Each plan card has: shift badge, date, status badge, progress bar (WOs completed/total). |
| **Result** | |

### TC-GUI-FLOOR-02 — View Work Orders on Plan
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-2 |
| **Precondition** | At least 1 plan with work orders visible on Floor page |
| **Steps** | 1. Click on a plan card to expand 2. See work order rows |
| **Expected** | Work order rows show: product name, target quantity, batch count, status badge. Each row is clickable. |
| **Result** | |

### TC-GUI-FLOOR-03 — Work Order Detail Panel with Technology Steps
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-3, FE-FLOOR-4 |
| **Precondition** | Expanded plan with WOs, recipe + technology steps exist for the WO product |
| **Steps** | 1. Click on a work order row 2. Detail panel opens (slides in from right) 3. "Technology Steps" tab should be active |
| **Expected** | Interactive checklist: each step shows number, name, activities, instruments, duration (minutes), temperature (°C). Each step has a toggle button (check/uncheck). |
| **Result** | |

### TC-GUI-FLOOR-04 — Toggle Technology Step Completion
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-5 |
| **Precondition** | WO detail panel open, Technology Steps tab visible |
| **Steps** | 1. Click the toggle on Step 1 → mark as done 2. Click toggle on Step 2 → mark as done 3. Continue until all steps are checked |
| **Expected** | Each checked step shows green check. When ALL steps are checked, a green "All steps confirmed" banner appears. Checks persist in localStorage (survive page refresh). |
| **Result** | |

### TC-GUI-FLOOR-05 — View Recipe Details for Work Order
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-6 |
| **Precondition** | WO detail panel open |
| **Steps** | 1. Click "Recipe" tab in the detail panel |
| **Expected** | Shows: batch size + UoM, expected yield + UoM, lead time (hours). Production notes displayed. Ingredients table: item name, quantity per batch × batch count, UoM, waste %. |
| **Result** | |

### TC-GUI-FLOOR-06 — Start and Complete Work Order from Floor
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-7 |
| **Precondition** | WO detail panel open, WO status=PENDING |
| **Steps** | 1. Click "Start Work Order" button in the detail panel footer 2. Observe status change to STARTED/IN_PROGRESS 3. Click "Complete Work Order" 4. Observe status change to COMPLETED |
| **Expected** | Status badge updates. Progress bar on the plan card increments. Buttons change contextually (Start→Complete→none). |
| **Result** | |

### TC-GUI-FLOOR-07 — Cancel Work Order from Floor
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-8 |
| **Precondition** | A PENDING or STARTED work order |
| **Steps** | 1. Open WO detail panel 2. Click "Cancel" button |
| **Expected** | WO status changes to CANCELLED. No more Start/Complete buttons. |
| **Result** | |

### TC-GUI-FLOOR-08 — Close Detail Panel
| Field | Value |
|-------|-------|
| **Requirement** | FE-FLOOR-9 |
| **Precondition** | WO detail panel is open |
| **Steps** | 1. Click "Close" button in the panel footer |
| **Expected** | Detail panel slides away. Back to the plan/WO list view. |
| **Result** | |

---

# Section 7: Recipes (`/recipes`)

### TC-GUI-REC-01 — Recipes Page Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-1 |
| **Precondition** | Logged in as admin or technologist, at least 1 product exists |
| **Steps** | 1. Click "Recipes" in sidebar |
| **Expected** | Product selector dropdown at top. After selecting a product, recipe list appears (empty if no recipes for that product). |
| **Result** | |

### TC-GUI-REC-02 — Create Recipe with Ingredients
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-2 |
| **Precondition** | On Recipes page, a product selected |
| **Steps** | 1. Click "Create Recipe" (or similar button) 2. In wide modal: Batch Size: 10, UoM: "kg", Expected Yield: 9.5, Yield UoM: "kg", Lead Time: 4 hours, Notes: "Standard recipe" 3. Add Ingredient: Item ID from seeded flour item, Qty/batch: 5, UoM: "kg", Waste Factor: 0.02 4. Add another ingredient (sugar, qty: 2, UoM: "kg") 5. Submit |
| **Expected** | Recipe appears in the list with version number, status DRAFT, batch size 10, yield 9.5, 2 ingredients shown. |
| **Result** | |

### TC-GUI-REC-03 — View Recipe Ingredients (Expanded)
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-3 |
| **Precondition** | Recipe with ingredients exists |
| **Steps** | 1. Click on a recipe card to expand 2. "Ingredients" tab should be active |
| **Expected** | Table with columns: Item, Qty/batch, UoM, Waste%, Purchase size, Purchase UoM. Shows all ingredient rows. |
| **Result** | |

### TC-GUI-REC-04 — Edit Recipe Ingredients
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-4 |
| **Precondition** | Expanded DRAFT recipe, Ingredients tab visible |
| **Steps** | 1. Click "Edit Ingredients" button 2. Change a quantity or add a new ingredient row 3. Click "Save" |
| **Expected** | Switches to edit mode with inline editable fields. After save, table reflects updated quantities. Success message shown. |
| **Result** | |

### TC-GUI-REC-05 — Activate Recipe
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-5 |
| **Precondition** | DRAFT recipe exists |
| **Steps** | 1. Click "Activate" button on the recipe card |
| **Expected** | Status badge changes from DRAFT to ACTIVE. Previous active version (if any) becomes archived. |
| **Result** | |

### TC-GUI-REC-06 — Technology Steps Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-6 |
| **Precondition** | Recipe expanded |
| **Steps** | 1. Click "Technology Steps" tab |
| **Expected** | List of numbered step cards (or empty state with "+ Add Step" button). Each card shows step number, name, activities, instruments, duration (min), temperature (°C). Edit (pencil) and Delete (×) buttons per step. |
| **Result** | |

### TC-GUI-REC-07 — Create Technology Step
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-7 |
| **Precondition** | On Technology Steps tab |
| **Steps** | 1. Click "+ Add Step" 2. In modal: Step Number: 1, Name: "Mix Dough", Activities: "Combine flour, water, yeast. Knead for 10 min.", Instruments: "Stand Mixer", Duration: 15 min, Temperature: 25°C 3. Submit |
| **Expected** | New step card appears numbered 1 with all entered details. |
| **Result** | |

### TC-GUI-REC-08 — Edit and Delete Technology Step
| Field | Value |
|-------|-------|
| **Requirement** | FE-REC-8 |
| **Precondition** | At least 1 technology step exists |
| **Steps** | 1. Click Edit (pencil) on a step → modal opens pre-filled 2. Change duration to 20 min → Save 3. Click Delete (×) on the step → confirm |
| **Expected** | Edit: step card updates with new duration. Delete: step disappears from list. |
| **Result** | |

---

# Section 8: Products (`/products`)

### TC-GUI-PROD-01 — Products Page Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-PROD-1 |
| **Precondition** | Logged in as admin or technologist |
| **Steps** | 1. Click "Products" in sidebar |
| **Expected** | Table with columns: Name, Department, Sale Unit, Base UoM, Price, VAT %, Status (badge), ID. "+ New Product" button. Seeded products (White Bread, Sourdough, Baguette, Croissant, Cake) visible. |
| **Result** | |

### TC-GUI-PROD-02 — Create New Product
| Field | Value |
|-------|-------|
| **Requirement** | FE-PROD-2 |
| **Precondition** | At least 1 department exists |
| **Steps** | 1. Click "+ New Product" 2. In modal: Department: select one, Name: "Rye Bread", Sale Unit: PIECE, Base UoM: "piece", Sale Price: 12000, VAT Rate: 12 3. Submit |
| **Expected** | Modal closes. New product "Rye Bread" appears in table with ACTIVE status, correct price and VAT. |
| **Result** | |

---

# Section 9: Departments (`/departments`)

### TC-GUI-DEPT-01 — Departments Page Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-DEPT-1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Click "Departments" in sidebar |
| **Expected** | Table with columns: Name, Lead Time (hours), Warehouse Mode, Status (badge), ID. "+ New Department" button. Seeded departments (DEPT-BREAD, DEPT-PASTRY, DEPT-CONFECT) visible. |
| **Result** | |

### TC-GUI-DEPT-02 — Create New Department
| Field | Value |
|-------|-------|
| **Requirement** | FE-DEPT-2 |
| **Precondition** | On Departments page |
| **Steps** | 1. Click "+ New Department" 2. In modal: Name: "Packaging", Fallback Lead Time: 2 hours, Warehouse Mode: SHARED 3. Submit |
| **Expected** | Modal closes. New department "Packaging" appears in table with 2h lead time, SHARED mode, ACTIVE status. |
| **Result** | |

---

# Section 10: Admin Panel (`/admin`)

### TC-GUI-ADMIN-01 — Admin Panel Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Click "Admin Panel" in sidebar |
| **Expected** | See sections: Master Data (4 link cards: Departments, Products, Recipes, Items/Raw Materials), Users & Roles, Operational Settings, System Info. |
| **Result** | |

### TC-GUI-ADMIN-02 — Master Data Navigation Links
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-2 |
| **Precondition** | On Admin Panel |
| **Steps** | 1. Click "Departments" card → verify navigates to `/departments` 2. Go back. Click "Products" → `/products` 3. Go back. Click "Recipes" → `/recipes` 4. Go back. Click "Items" → `/inventory` (Items tab) |
| **Expected** | Each card navigates to the correct page. |
| **Result** | |

### TC-GUI-ADMIN-03 — View Users Table
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-3 |
| **Precondition** | On Admin Panel, DB users exist (seeded) |
| **Steps** | 1. Scroll to "Users & Roles" section |
| **Expected** | Table showing: Username, Display Name, Role (colored badge), Status (Active/Inactive). 7 seeded users visible. Deactivate/Activate toggle per user. |
| **Result** | |

### TC-GUI-ADMIN-04 — Create New User
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-4 |
| **Precondition** | On Admin Panel |
| **Steps** | 1. Click "+ New User" (or find the inline form) 2. Enter Username: "testuser", Password: "testpass", Display Name: "Test User", Role: Cashier 3. Submit |
| **Expected** | New user "testuser" appears in the table with Cashier role, Active status. |
| **Result** | |

### TC-GUI-ADMIN-05 — New User Can Login
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-5 |
| **Precondition** | "testuser" just created |
| **Steps** | 1. Logout 2. Login as testuser / testpass |
| **Expected** | Login succeeds. Redirected to `/pos` (cashier default landing). |
| **Result** | |

### TC-GUI-ADMIN-06 — Deactivate User
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-6 |
| **Precondition** | On Admin Panel, "testuser" is Active |
| **Steps** | 1. Click "Deactivate" toggle next to testuser |
| **Expected** | Status changes to Inactive. |
| **Result** | |

### TC-GUI-ADMIN-07 — Deactivated User Cannot Login
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-7 |
| **Precondition** | "testuser" is deactivated |
| **Steps** | 1. Logout 2. Try to login as testuser / testpass |
| **Expected** | Login fails with error message (401). Remains on login page. |
| **Result** | |

### TC-GUI-ADMIN-08 — View Operational Settings
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-8 |
| **Precondition** | On Admin Panel |
| **Steps** | 1. Scroll to "Operational Settings" section |
| **Expected** | Shows: Order Cut-off Time (22:00), Rush Order Premium % (15), Main Currency (UZS). Values match seeded TenantConfig. |
| **Result** | |

### TC-GUI-ADMIN-09 — System Info Section
| Field | Value |
|-------|-------|
| **Requirement** | FE-ADMIN-9 |
| **Precondition** | On Admin Panel |
| **Steps** | 1. Scroll to "System Info" section |
| **Expected** | Shows: app version, backend tech (Spring Boot), frontend tech (Next.js + React), API base (http://localhost:8080), tenant ID (tenant1), site ID (MAIN). |
| **Result** | |

---

# Section 11: POS (`/pos`)

### TC-GUI-POS-01 — POS Page Layout
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-1 |
| **Precondition** | Logged in as admin or cashier, at least 1 product exists |
| **Steps** | 1. Navigate to `/pos` |
| **Expected** | Split layout: Product Catalog grid (left) + Cart sidebar (right). Filter bar with search input and department dropdown. Products shown as cards with 🍞 emoji, name, department, UoM. Cart is empty with "Empty cart" message. |
| **Result** | |

### TC-GUI-POS-02 — Add Product to Cart
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-2 |
| **Precondition** | On POS page |
| **Steps** | 1. Click on a product card (e.g., White Bread) 2. QuickAdd overlay appears 3. Set Qty: 3, Unit Price: 8000 4. Observe running total (24000) 5. Click "Add to Cart" |
| **Expected** | QuickAdd overlay closes. Cart sidebar shows 1 line: White Bread × 3 = 24,000. Cart total updates. |
| **Result** | |

### TC-GUI-POS-03 — Modify Cart Quantities
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-3 |
| **Precondition** | At least 1 item in cart |
| **Steps** | 1. In cart, click "+" to increase qty 2. Click "−" to decrease qty 3. Edit qty input directly to a new value 4. Click "✕" to remove item |
| **Expected** | Quantities and line totals update in real time. Removing last item shows "Empty cart" again. |
| **Result** | |

### TC-GUI-POS-04 — Complete CASH Sale
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-4 |
| **Precondition** | Cart has items, total > 0 |
| **Steps** | 1. In checkout: Customer Name defaults to "Walk-In" 2. Payment Method toggle: select CASH 3. Enter Cash Received: amount greater than total (e.g., total is 24000, enter 30000) 4. Verify change calculation shows: 6000 5. Click "Complete Sale" |
| **Expected** | Green success banner with sale ID and change amount. Cart is cleared. |
| **Result** | |

### TC-GUI-POS-05 — Complete CARD Sale
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-5 |
| **Precondition** | Cart has items |
| **Steps** | 1. Payment Method toggle: select CARD 2. Cash Received input should be hidden or disabled 3. Click "Complete Sale" |
| **Expected** | Green success banner with sale ID. Change = 0 (not shown). Cart cleared. |
| **Result** | |

### TC-GUI-POS-06 — Filter Products by Department
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-6 |
| **Precondition** | Products exist in different departments |
| **Steps** | 1. Select a department from the dropdown filter 2. Observe product grid |
| **Expected** | Only products from selected department are shown. |
| **Result** | |

### TC-GUI-POS-07 — Search Products
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-7 |
| **Precondition** | Multiple products exist |
| **Steps** | 1. Type "Croiss" in the search input |
| **Expected** | Product grid filters to show only matching products (e.g., Croissant). |
| **Result** | |

### TC-GUI-POS-08 — Clear Cart
| Field | Value |
|-------|-------|
| **Requirement** | FE-POS-8 |
| **Precondition** | Cart has multiple items |
| **Steps** | 1. Click "Clear" button on the cart |
| **Expected** | All items removed. Cart shows empty state. Total = 0. |
| **Result** | |

---

# Section 12: Reports (`/reports`)

### TC-GUI-RPT-01 — Reports Page Load with Revenue Summary
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-1 |
| **Precondition** | Logged in as admin/finance/viewer, at least 1 POS sale made |
| **Steps** | 1. Click "Reports" in sidebar |
| **Expected** | Revenue summary banner always visible: Today, Last 7 Days, Last 30 Days, All-time — each with amount and currency (UZS). Refresh button with last-refresh timestamp. 4 tabs: Orders, Inventory, Production, Revenue. |
| **Result** | |

### TC-GUI-RPT-02 — Orders Report Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-2 |
| **Precondition** | On Reports page, orders exist |
| **Steps** | 1. Click "Orders" tab |
| **Expected** | KPIs: Total orders, Active, Delivered, Cancelled, Rush count, Revenue total. Revenue by Status table. Top Customers table. All Orders table (sortable by placed date). |
| **Result** | |

### TC-GUI-RPT-03 — Inventory Report Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-3 |
| **Precondition** | On Reports page, inventory positions exist |
| **Steps** | 1. Click "Inventory" tab |
| **Expected** | KPIs: Total Positions, Stock Value, Unique Items, Low Stock Alerts count. Valuation by Item Type (cards). Low Stock Alerts table (item, type, on hand, threshold, deficit). Full Positions Detail table with all lots. |
| **Result** | |

### TC-GUI-RPT-04 — Production Report Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-4 |
| **Precondition** | On Reports page, production plans exist |
| **Steps** | 1. Click "Production" tab |
| **Expected** | KPIs: Total Plans, Completed Plans, Total WOs, Completed WOs. Plans by Status table with progress bars. Work Orders Summary breakdown. All Plans table with completion percentage. |
| **Result** | |

### TC-GUI-RPT-05 — Revenue Report Tab
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-5 |
| **Precondition** | On Reports page, POS sales exist |
| **Steps** | 1. Click "Revenue" tab |
| **Expected** | Top Products table (last 7 days) with product name, total quantity sold, total revenue. |
| **Result** | |

### TC-GUI-RPT-06 — Refresh Reports
| Field | Value |
|-------|-------|
| **Requirement** | FE-RPT-6 |
| **Precondition** | On Reports page |
| **Steps** | 1. Click the "Refresh" button |
| **Expected** | Data reloads. "Last refresh" timestamp updates to current time. |
| **Result** | |

---

# Section 13: Technologist Dashboard (`/technologist`)

### TC-GUI-TECH-01 — Technologist Page Load
| Field | Value |
|-------|-------|
| **Requirement** | FE-TECH-1 |
| **Precondition** | Logged in as technologist or admin |
| **Steps** | 1. Navigate to `/technologist` |
| **Expected** | 4 KPI cards: Products with active recipe, Missing active recipe, Products with lead time set, Avg production lead time. Recipe Health table. Production Frequency chart. Recent Production Plans section. |
| **Result** | |

### TC-GUI-TECH-02 — Recipe Health Table
| Field | Value |
|-------|-------|
| **Requirement** | FE-TECH-2 |
| **Precondition** | Some products have active recipes, some don't |
| **Steps** | 1. Scroll to Recipe Health section 2. Expand a product row |
| **Expected** | Each product shows: name, department, health badge (✓ Active vN or ⚠ No Active Recipe), lead time. Expanded: batch/yield/lead time info, production notes, ingredients table. Missing recipe → red warning with link to `/recipes`. |
| **Result** | |

### TC-GUI-TECH-03 — Production Frequency Chart
| Field | Value |
|-------|-------|
| **Requirement** | FE-TECH-3 |
| **Precondition** | At least 1 production plan with work orders |
| **Steps** | 1. Scroll to Production Frequency section |
| **Expected** | Horizontal bar chart showing top 8 products by work order count. |
| **Result** | |

### TC-GUI-TECH-04 — Recent Production Plans
| Field | Value |
|-------|-------|
| **Requirement** | FE-TECH-4 |
| **Precondition** | Production plans exist |
| **Steps** | 1. Scroll to Recent Production Plans |
| **Expected** | Last 10 plans with date, shift, WO count, completion % progress bar, status badge. Link to `/production-plans`. |
| **Result** | |

---

# Section 14: Role-Based Access & Sidebar Visibility

### TC-GUI-ROLE-01 — Admin Sees Full Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-1 |
| **Precondition** | Logged in as admin |
| **Steps** | 1. Observe sidebar navigation |
| **Expected** | All sections visible: Dashboard, Orders, Production Plans, Inventory, POS, Reports, Floor, Technologist, Admin Panel, Departments, Products, Recipes |
| **Result** | |

### TC-GUI-ROLE-02 — Floor Worker (production) Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-2 |
| **Precondition** | Logged in as `production` |
| **Steps** | 1. Observe sidebar |
| **Expected** | Only sees: "My Shift" → Production Floor, "Operations" → Production Plans. Does NOT see: Dashboard, Orders, Inventory, POS, Reports, Admin, Departments, Products, Recipes. |
| **Result** | |

### TC-GUI-ROLE-03 — Cashier Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-3 |
| **Precondition** | Logged in as `cashier` |
| **Steps** | 1. Observe sidebar |
| **Expected** | Only sees POS in the sidebar. No Dashboard, Orders, Inventory, Reports, Admin, etc. |
| **Result** | |

### TC-GUI-ROLE-04 — Warehouse Worker Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-4 |
| **Precondition** | Logged in as `warehouse` |
| **Steps** | 1. Observe sidebar |
| **Expected** | Only sees Inventory in the sidebar. |
| **Result** | |

### TC-GUI-ROLE-05 — Finance User Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-5 |
| **Precondition** | Logged in as `finance` |
| **Steps** | 1. Observe sidebar |
| **Expected** | Sees: Dashboard, Reports. Does NOT see: Orders, Production Plans, Inventory, POS, Admin, Departments, Products, Recipes. |
| **Result** | |

### TC-GUI-ROLE-06 — Technologist Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-6 |
| **Precondition** | Logged in as `technologist` |
| **Steps** | 1. Observe sidebar |
| **Expected** | Sees: "Workshop" → Recipes, Products. "Analysis" → Technologist. Does NOT see: Dashboard, Orders, Production Plans, Inventory, POS, Reports, Admin, Departments. |
| **Result** | |

### TC-GUI-ROLE-07 — Direct URL Access Denied (Sidebar Only)
| Field | Value |
|-------|-------|
| **Requirement** | FE-ROLE-7 |
| **Precondition** | Logged in as `cashier` |
| **Steps** | 1. Manually navigate to http://localhost:3000/admin 2. Navigate to http://localhost:3000/orders |
| **Expected** | Page may load but backend API returns 403 (no data shown, or error displayed). Sidebar does not show links to these pages. |
| **Result** | |

---

# Section 15: Cross-Cutting & UI Quality

### TC-GUI-UI-01 — Responsive Sidebar
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-1 |
| **Precondition** | Logged in |
| **Steps** | 1. Resize browser to narrow width (~768px) 2. Resize back to full width |
| **Expected** | Sidebar collapses or becomes a hamburger menu on small screens. Content area adjusts. No horizontal overflow. |
| **Result** | |

### TC-GUI-UI-02 — Loading States (Spinner)
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-2 |
| **Precondition** | Any page |
| **Steps** | 1. Navigate to a data-heavy page (Dashboard, Orders, Reports) 2. Watch for loading indicator |
| **Expected** | Spinner or loading indicator shown while data is being fetched from backend. Disappears when data loads. |
| **Result** | |

### TC-GUI-UI-03 — Error Handling (Backend Down)
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-3 |
| **Precondition** | Frontend running, stop the backend (Ctrl+C on mvn spring-boot:run) |
| **Steps** | 1. Navigate to `/orders` or any data page |
| **Expected** | Red error alert displayed (not a white screen/crash). User sees a meaningful error message. |
| **Result** | |

### TC-GUI-UI-04 — Session Expiry (401 Redirect)
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-4 |
| **Precondition** | Logged in |
| **Steps** | 1. Open DevTools → Application → LocalStorage 2. Replace `bc_token` value with "invalid-token" 3. Navigate to any page |
| **Expected** | Backend returns 401. Frontend clears credentials and redirects to `/login`. |
| **Result** | |

### TC-GUI-UI-05 — Status Badges Consistency
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-5 |
| **Precondition** | Data exists across pages |
| **Steps** | 1. Check badge colors across pages: DRAFT (gray/blue), ACTIVE (green), CONFIRMED (blue), CANCELLED (red), IN_PROGRESS (yellow/amber), COMPLETED (green) |
| **Expected** | Same status name uses the same badge color throughout all pages. |
| **Result** | |

### TC-GUI-UI-06 — Modal Close Behavior
| Field | Value |
|-------|-------|
| **Requirement** | FE-UI-6 |
| **Precondition** | Any page with a modal (e.g., New Order) |
| **Steps** | 1. Open a modal 2. Click the "×" close button 3. Open the modal again 4. Click outside the modal (on the overlay) |
| **Expected** | Modal closes cleanly in both cases. Form data is cleared on close. |
| **Result** | |

---

# Full Workflow: End-to-End Scenario

### TC-GUI-E2E-01 — Complete Business Cycle (Admin)

This test walks through the entire bakery workflow from setup to sale.

| Step | Action | Screen | Expected |
|------|--------|--------|----------|
| 1 | Login as admin | /login | Redirected to /dashboard |
| 2 | Create department "Test Bakery" (2h lead, SHARED) | /departments | New department appears in table |
| 3 | Create product "Test Loaf" (PIECE, 8000 UZS, 12% VAT) in dept "Test Bakery" | /products | Product appears in table |
| 4 | Create recipe for "Test Loaf": batch 10, yield 9.5, ingredients: Flour 5kg, Sugar 1kg | /recipes | Recipe appears as DRAFT |
| 5 | Activate the recipe | /recipes | Status changes to ACTIVE |
| 6 | Receive 200 kg Flour @ 3000/kg and 50 kg Sugar @ 5000/kg | /inventory | Two lots appear in Stock Levels |
| 7 | Create order: customer "Daily Market", delivery tomorrow, 20× Test Loaf @ 8000 | /orders | DRAFT order appears |
| 8 | Confirm the order | /orders | Status → CONFIRMED |
| 9 | Create production plan for tomorrow, MORNING shift | /production-plans | DRAFT plan created |
| 10 | Generate work orders | /production-plans | WOs appear under plan |
| 11 | Approve plan | /production-plans | Status → APPROVED |
| 12 | Start plan | /production-plans | Status → IN_PROGRESS |
| 13 | Switch to floor worker: login as `production` | /floor | See today's plan |
| 14 | Open WO detail, check all technology steps | /floor | Green "all confirmed" banner |
| 15 | Start WO, then Complete WO | /floor | Progress bar fills |
| 16 | Login as admin, advance order: IN_PRODUCTION → READY → DELIVERED | /orders | Status progresses |
| 17 | Login as cashier | /pos | POS screen loads |
| 18 | Add 3× Test Loaf to cart, CASH payment, give 30000 | /pos | Sale completes, change = 6000 |
| 19 | Login as admin, check reports | /reports | Revenue tab shows Test Loaf sales |
| 20 | Check dashboard | /dashboard | KPIs reflect orders, plans, inventory, revenue |

| **Result** | |
| **Notes** | |

---

# Execution Checklist

| Section | Tests | Pass | Fail | Skip |
|---------|-------|------|------|------|
| 1. Login | 7 | | | |
| 2. Dashboard | 6 | | | |
| 3. Orders | 10 | | | |
| 4. Production Plans | 11 | | | |
| 5. Inventory | 9 | | | |
| 6. Floor | 8 | | | |
| 7. Recipes | 8 | | | |
| 8. Products | 2 | | | |
| 9. Departments | 2 | | | |
| 10. Admin Panel | 9 | | | |
| 11. POS | 8 | | | |
| 12. Reports | 6 | | | |
| 13. Technologist | 4 | | | |
| 14. Role Access | 7 | | | |
| 15. Cross-Cutting UI | 6 | | | |
| E2E Workflow | 1 | | | |
| **TOTAL** | **104** | | | |

---

*End of GUI Test Plan v1.0*
