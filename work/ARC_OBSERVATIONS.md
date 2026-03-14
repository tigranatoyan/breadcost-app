# Arc-by-Arc Page Observations

## Arc 1: Order Lifecycle

### /orders
- **Status:** OBSERVED
- **Observations:** 
  - Pages are accessible without logging in — no auth guard on frontend routes (cross-cutting)
  - Login page auto-redirects to dashboard (can't stay on login page) — likely stale token in localStorage
  - Order statuses are not translated — show raw English (DRAFT, CONFIRMED, IN_PRODUCTION, etc.) instead of Armenian
  - DRAFT orders should be editable before confirmation (edit order lines, customer, delivery date) — currently no edit functionality visible
  - Status pipeline labels untranslated: DRAFT, CONFIRMED, IN PRODUCTION, READY, OUT FOR DELIVERY, DELIVERED all show in English
  - "DRAFT" status badge shows English
  - "PCS" unit shows in English instead of Armenian
  - Product names in English (seed data — White Bread, Sourdough Bread, Baguette)
  - Line Total column shows 0.00 for all lines — backend not calculating lineTotal (qty × unitPrice should be 2000, 2500, 2200)
  - Column header "Տողի delays" appears to be broken/mixed translation 

### /deliveries
- **Status:** OBSERVED
- **Observations:** Empty — no delivery runs exist. This is expected because the full Arc 1 flow requires: Order → Confirm → Production Plan → Floor (work orders complete) → Order READY → Create Delivery Run. Need to walk through production first.
  - STILL EMPTY even after order marked READY and then OUT FOR DELIVERY from /orders page
  - "Delivery Management" title partially visible, page heading in English
  - "No delivery runs." message — no auto-creation of delivery run when order status changes
  - BUG: /deliveries page is completely disconnected from order flow. Marking an order "Out for Delivery" on /orders does NOT create a delivery run here.
  - DESIGN ISSUE: The entire order lifecycle (READY → OUT FOR DELIVERY → DELIVERED) is driven by buttons on /orders page, completely bypassing /deliveries and /driver pages. This is architecturally wrong:
    - "Out for Delivery" should be triggered from /deliveries by logistics team (create delivery run, assign driver, load orders, dispatch)
    - "Mark Delivered" should come from /driver page when driver confirms delivery with proof (signature, photo)
    - /orders should only show status changes passively — not drive logistics actions
  - The /deliveries page appears to be a dead/non-functional page — no way to create delivery runs from here that link to READY orders
  - CORRECTION: /deliveries page DOES work — "Create Delivery Run" creates a run (PENDING status), "Assign Orders" button available
  - BUG/UX: "Assign Orders to Run" dialog requires manually typing raw order UUIDs separated by commas (placeholder: "order-id-1, order-id-2, ..."). User would need to know UUIDs. Should auto-populate with READY orders as a selectable list with checkboxes, filterable by delivery date/area.
  - Run ID displayed as raw UUID (0b82d293) — not user-friendly, should be sequential run number
  - DRIV... column truncated — driver assignment column
  - "Delivery run created." success toast works but in English
  - i18n: "LOGISTICS", "Delivery Management", "Assign Orders to Run", "Enter order IDs separated by commas", "Cancel", "Assign Orders", "RUN ID", "DRIVER", "STATUS", "ACTIONS", "PENDING", "+ Create Delivery Run" all in English
  - i18n: "Delivery Management", "No delivery runs." all in English

### /driver
- **Status:** OBSERVED
- **Observations:**
  - NOT IN NAVIGATION MENU — only accessible by typing /driver in URL bar. Critical gap for a driver-facing page.
  - 3 tabs: Active Sessions, Packaging, Payments — all empty ("No active sessions.", "No packaging confirmations.", "No payments recorded.")
  - BUG: Button labels show RAW i18n KEYS instead of translated text: "driver.refresh", "driver.lookup" — translation completely broken on this page
  - Packaging tab requires typing raw "Delivery Run ID" UUID — same usability problem as /deliveries
  - Payments tab requires typing raw "Session ID" UUID
  - Page is completely unusable — no auto-populated data, no connection to delivery runs or orders, requires memorizing UUIDs
  - DESIGN ISSUE: Should auto-show assigned delivery run for logged-in driver, with list of orders to deliver, navigation, proof of delivery capture
  - i18n: "LOGISTICS", "Driver Sessions", "Active Sessions", "Packaging", "Payments", "Delivery Run ID", "Session ID", "No active sessions.", "No packaging confirmations.", "No payments recorded." all English; button labels show raw keys

### Cross-Arc Note
- Arc 2 (Production Planning) serves TWO purposes: (1) fulfilling confirmed orders from Arc 1, and (2) independent daily shop production (not tied to orders). E2E flow covers case 1, but Arc 2 must also be tested independently for case 2. 

---

## Arc 2: Production Planning

### /production-plans
- **Status:** OBSERVED
- **Observations:** 
  - Clicked "Start Production" on orders page (order moved to IN_PRODUCTION status) — but no auto-created plan
  - /production-plans page was initially empty — no production plan auto-created from order status change
  - "Create Plan" worked — plan created for 2026-03-15, Night shift, DRAFT status, 0 work orders
  - Plan shows 3 sections: WORK ORDERS (empty), SCHEDULE (LEAD TIME) (empty), MATERIAL REQUIREMENTS (empty)
  - "Generate Work Orders" worked — 3 WOs created (White Bread, Sourdough Bread, Baguette), all PENDING, 10 PCS each, 1 batch, Bread Department
  - Plan status changed from DRAFT → GENERATED
  - "Approve" and "Start" buttons now visible at plan level
  - Each WO has "Start" and "Cancel" action buttons
  - Yield column is empty
  - i18n: "GENERATED", "PENDING" badges in English; "WORK ORDERS", "Product", "Department", "Qty", "Batches", "Status", "Yield", "Actions" headers all in English
  - "SCHEDULE (LEAD TIME)" still empty — expected (no recipes with lead times configured)
  - "MATERIAL REQUIREMENTS" still empty — expected (no active WOs with recipes)
  - BUG: Clicking "Create Plan" repeatedly creates duplicate plans for the same date/shift. No idempotency check. Now showing 3 plans (two for 2026-03-15 Night, one for 2026-03-16 Afternoon) all with 3 WOs each.
  - BUG: "Generate Work Orders" on each duplicate plan creates duplicate WOs from the same confirmed orders — no guard against double-generation
  - UX: Only one plan can be expanded at a time — expanding one collapses the other. Should allow multiple plans expanded simultaneously for comparison.
  - BUG: More duplicate plans created — now 6 plans visible (mix of dates/shifts), 3 empty (0 WOs). Creating plans and generating WOs is completely manual and error-prone.
  - BUG: New plans generate 0 work orders because the confirmed orders were already consumed by previous WO generation — but no feedback to user about this.
  - DESIGN ISSUE: The entire order→production flow is wrong. Current: user manually creates plans, manually generates WOs. Expected: 
    - Confirming an order should auto-queue it into a production plan for the requested delivery date
    - Plans page should show a queue of confirmed orders awaiting manufacturing approval
    - Manager approves/starts production — doesn't manually "generate" WOs
    - Orders have customer ID + delivery date — should auto-group into plans by date
    - Separation of concerns: Order confirmation = customer operations; Plan management = manufacturing operations
  - On orders page: both orders show "IN PRODUCTION" with "Mark Ready" — but no plan was actually linked to them
  - Plans not sorted chronologically (2026-03-16 appears before 2026-03-14)
  - No way to delete duplicate/empty plans
  - Approve button shows browser confirm dialog "lock the order" — but there are no reject/send-back actions available
  - DESIGN ISSUE: Approve is meant for manufacturing to reject orders (lead time breach, insufficient inventory) — but those flags should surface at order confirmation/entry time, not at plan approval. Approve without reject option is meaningless.
  - Approval should show reasons to reject (lead time conflict, inventory shortage) and allow sending back to customer ops
  - After Approve → Start: plan status changed to IN PROGRESS
  - Individual WO Start works (White Bread → STARTED, shows Yield input and Complete/Cancel buttons)
  - BUG: Plan-level "Complete" button (top-right green) does nothing when clicked — no error, no feedback. Should disable when WOs not all completed, or show validation message "All work orders must be completed first."
  - UX: "SCHEDULE (LEAD TIME)" section shows "No scheduled work orders. Generate work orders with recipes that have lead times." — unclear purpose during active production
  - i18n: WORK ORDERS, Product, Department, Qty, Batches, Status, Yield, Actions, SCHEDULE (LEAD TIME), MATERIAL REQUIREMENTS section headers all English
  - All 3 WOs Started and Completed individually — plan auto-advanced to COMPLETED status ✓
  - BUG: Yield shows "0.0h" for all completed WOs — entered yield value wasn't saved, or displays in wrong format (hours instead of quantity/percentage)
  - BUG: Plan completion does NOT auto-update linked order status. Orders still show IN_PRODUCTION after plan is COMPLETED. No backend event/trigger linking plan completion → order READY.
  - DESIGN ISSUE: "Mark Ready" button appears on /orders page — this is a PRODUCTION function, not customer operations. Completing all WOs in a production plan should auto-advance the linked order to READY status. Customer ops should not need to manually track when production finishes.
  - Orders now show totals: Gago's shop 18,450.00 (1 line), Armen's shop 6,834.00 (3 lines) — header totals work but line-level totals were 0.00 (reported earlier)
  - RUSH badges visible: "+2.5%" on Gago's shop, "+2%" on Armen's shop — surcharge feature works but labels in English
  - i18n: "RUSH", "IN PRODUCTION", "Mark Ready" all English on orders page
  - "Mark Ready" clicked on Gago's shop → status changed to READY, button changed to "Out for Delivery" — works
  - "Out for Delivery" clicked → status changed to OUT FOR DELIVERY, button changed to "Mark Delivered"
  - DESIGN ISSUE: Entire order lifecycle driven by simple status buttons on /orders page, bypassing dedicated operational pages:
    - READY → OUT FOR DELIVERY should be driven from /deliveries (create delivery run, assign driver)
    - OUT FOR DELIVERY → DELIVERED should be driven from /driver (driver confirms delivery)
    - /orders page should only passively reflect status — not trigger logistics actions
  - This means /deliveries and /driver pages serve no functional purpose in the current flow

### /floor
- **Status:** OBSERVED
- **Observations:**
  - Shows "Production Floor" with today's date (Saturday, March 14) — but the completed plan was for 2026-03-15 Night shift
  - Shows one plan: MORNING 2026-03-14, GENERATED, 0/0 complete, "No work orders yet." — this is an empty/orphaned plan, not the one with actual WOs
  - Does NOT show the Night 2026-03-15 plan that was completed with 3 WOs — filtering is date-locked to today only
  - CRITICAL DESIGN ISSUE: /floor should be the PRIMARY page where production workers START and COMPLETE work orders — NOT /production-plans. The correct flow:
    - /production-plans: Plan generation (auto from confirmed orders), approval by manager
    - /floor: Floor workers see approved plans, START individual WOs, record yield, COMPLETE WOs
    - Currently backward: /production-plans has Start/Complete buttons, /floor is just a passive read-only display
  - No date navigation — can't browse other dates to see past/future plans
  - "PRODUCTION" category label, "Production Floor" heading in English
  - "MORNING" badge in English, "GENERATED" status in English
  - i18n: "PRODUCTION", "Production Floor", "Saturday, March 14", "MORNING", "GENERATED", "complete", "No work orders yet." all English

---

## Arc 3: Inventory & Supply Chain

### /inventory
- **Status:** OBSERVED
- **Observations:**
  - Page loads correctly with good structure: 2 tabs (Stock Levels 0, Items 7), 3 action buttons (Adjust, Transfer, Receive Stock)
  - Filters: All Types dropdown, All Locations dropdown, Search item text field, "Below threshold only" checkbox
  - Empty: "No stock on hand. Receive some stock to get started." — expected, needs seed data
  - Items tab shows 7 items exist in the system (seed data)
  - FIFO lots, stock alerts, receiving, transfers, and adjustments mentioned in subtitle — good feature scope
  - i18n: "INVENTORY", "Inventory", subtitle, "Stock Levels", "Items", "All Types", "All Locations", "Search item...", "Below threshold only", "Adjust", "Transfer", "Receive Stock", "No stock on hand..." all English
  - Receive Stock dialog works: Item dropdown shows 7 seed items (Wheat Flour KG, White Sugar KG, Butter KG, Dry Yeast KG, Table Salt KG, Eggs PCS, Whole Milk L), currency field, auto lot info: "A new lot will be created automatically. Site: MAIN · Location: RECEIVING"
  - Received 100 KG Wheat Flour at 220.0000 → Stock Levels (1), INGREDIENT type, RECEIVING location, lot cfcc84e1..., On Hand 100.000 KG, Avg Cost 220.0000, Total Value 22,000.00 — WORKS CORRECTLY
  - Lot detail expandable: "Lot Detail — FIFO Cost Layer" with full lot ID, On Hand, Avg Cost, Total Value — FIFO tracking works
  - Total Valuation footer shows 22,000.00 — correct
  - FEATURE GAP: Only 7 items in system (seed data). No way to ADD new inventory items (products/ingredients) from this page. Should be able to create new items with name, type, unit, threshold — not just receive stock for existing items.
  - FEATURE GAP: No bulk import via CSV/XLSX for inventory items or stock receipts. Essential for initial setup with hundreds of ingredients.
  - Item names in English (seed data): Wheat Flour, White Sugar, Butter, etc.
  - Units in English: KG, PCS, L

### /suppliers
- **Status:** OBSERVED
- **Observations:**
  - Page title: "Suppliers & Purchase Orders"
  - Supplier creation works — "Supplier created." success toast (Bagh-001)
  - Catalog dialog (Catalog — Bagh-001): shows "No catalog items", fields for Ingredient ID, Ingredient Name, Unit Price, Currency (defaulting to UZS), Lead Time, "+ Add" button
  - BUG: Currency defaults to UZS — should be AMD (Armenian Dram, ֏ sign)
  - BUG: "Error: 500: An unexpected error occurred" when generating PO from work orders (Auto-Suggest POs feature) — backend crash
  - Create PO dialog: Supplier dropdown (Bagh-001), Notes, FX Rate, FX Currency, Lines section (Ingredient ID, Ingredient Name, Quantity, Unit defaulting to "kg", Unit Price, remove button, + Add Line)
  - UX: Create PO requires manually typing Ingredient ID — should be a dropdown from supplier's catalog or inventory items. Same UUID problem as deliveries.
  - UX: Ingredient Name is a separate text field instead of auto-populating from Ingredient ID
  - "No purchase orders." shown in background — empty state
  - DESIGN ISSUE: Suppliers & Purchase Orders belongs in ADMIN panel for master data setup, not as an operational page. Operational PO flow should be: inventory falls below threshold → auto-suggest PO → approve → send to supplier. Current page mixes admin (supplier setup) with operations (PO creation).
  - "Auto-Suggest POs" button visible — good feature concept but returns 500
  - i18n: "Suppliers & Purchase Orders", "Catalog", "Ingredient ID", "Ingredient Name", "Unit Price", "Currency", "Lead Time", "Create PO", "Supplier", "Notes", "FX Rate", "FX Currency", "Lines", "Quantity", "Unit", "Cancel", "Save", "+ Add", "+ Add Line", "No catalog items.", "No purchase orders.", "Supplier created." all English

---

## Arc 4: Customer Portal

### /customers
- **Status:** OBSERVED
- **Observations:**
  - Page title: "Customer Management" under "CRM" category
  - 3 tabs: Customers, Product Catalog, Customer Orders
  - Customer creation works
  - BUG: Product Catalog tab returns errors
  - Customer Orders tab shows both orders (15276ec0 armen-s- IN PRODUCTION 6834.00, 30eb48ea gago-s-s OUT FOR DELIVERY 18450.00) — data is correct
  - Customer names truncated: "armen-s-", "gago-s-" — display names cut off or stored poorly
  - Order IDs shown as truncated UUIDs (15276ec0, 30eb48ea) — links are clickable (blue)
  - "+ Create Order" button available — duplicates order creation from /orders page
  - DESIGN ISSUE: Customer Orders tab here shows same data as /orders page — duplicate view. Should either link to /orders or show customer-specific view with different actions (no production/logistics buttons)
  - BUG/UX: When creating an order (on /orders page), customer name must be typed manually. Should be a dropdown or auto-complete populated from customer list. Currently no link between customer master data and order entry.
  - Status badges in English: "IN PRODUCTION", "OUT FOR DELIVERY"
  - Date format: 3/14/2026 (US format, should be localized)
  - i18n: "CRM", "Customer Management", "Customers", "Product Catalog", "Customer Orders", "ORDER ID", "CUSTOMER", "STATUS", "TOTAL", "DATE", "+ Create Order" all English

### /loyalty
- **Status:** OBSERVED
- **Observations:**
  - Loyalty page loads, program creation works
  - BUG/UX: Balance lookup requires entering raw Customer ID — should be customer name with auto-complete/search. Same UUID usability problem seen across deliveries, driver, PO creation.
  - i18n: Likely all English (consistent with other pages)

---

## Arc 5: POS

### /pos
- **Status:** SKIPPED
- **Observations:**
  - Skipped by user — POS requires hardware integrations (POS device, barcode scanner, scales, tax authority API) without which it is useless. Leave as-is for now.

---

## Arc 6: Financial Operations

### /invoices
- **Status:** SKIPPED
- **Observations:**
  - Skipped by user — invoices require tax authority integration (e-invoicing API). Orders already carry financial value. Leave as-is for now.

### /report-builder
- **Status:** OBSERVED
- **Observations:**
  - DESIGN: Report builder has 3 subscription tiers:
    - Lower tier: Standard indicators list shown, user selects, report saved. Appears on /reports page. When selected, runs the report. Must consider caching for repeated runs.
    - Middle tier: Choose several indicators and show how they correlate (cross-indicator analysis)
    - Higher tier: AI interface with CLI-style input — user describes what they want, system constructs the report
  - Current implementation status unclear — need to test separately

### /reports
- **Status:** OBSERVED
- **Observations:**
  - BUG: Page crashes — "Something went wrong: Cannot read properties of undefined (reading 'length')" — frontend JS error, likely API response is null/undefined where array expected
  - Shows too many indicators at once ("christmas tree" of indicators) — overwhelming, not useful
  - DESIGN: Should be filterable/selectable by department or generated via /report-builder
  - Reports should be generated from report-builder, saved, and then appear here as runnable saved reports
  - Running a report gave the crash error shown above

---

## Arc 7: AI Assistance

### /ai-suggestions
- **Status:** SKIPPED
- **Observations:**
  - DESIGN ISSUE: AI should NOT be separate pages. AI features (suggestions, pricing, quality predictions) should be context-dependent, embedded into relevant pages (e.g., pricing suggestions on /orders, quality predictions on /production-plans). This is a higher-tier subscription feature.

### /ai-pricing
- **Status:** SKIPPED
- **Observations:**
  - Same as above — should be embedded in context (order entry, product catalog), not standalone page.

### /ai-whatsapp
- **Status:** SKIPPED (for now)
- **Observations:**
  - PRIORITY FOR FIRST AI IMPLEMENTATION: WhatsApp integration will be first AI feature built.
  - Two use cases: (1) Auto-stocking via WhatsApp messaging (supplier communication), (2) Customer WhatsApp ordering (customers place orders via WhatsApp)
  - Will implement before other AI features.

### /quality-predictions
- **Status:** SKIPPED
- **Observations:**
  - Same as ai-suggestions — should be embedded in production floor/plans context, not standalone.

---

## Arc 8: Platform Administration

### /dashboard
- **Status:** OBSERVED
- **Observations:**
  - DESIGN: Dashboard should be configurable from admin panel — manager selects which widgets to display
  - Widget options should include: revenue, operational metrics, floor status, plan status, AI suggestions, etc.
  - Currently appears to be a fixed layout — no widget customization

### /analytics
- **Status:** OBSERVED
- **Observations:**
  - DESIGN: Analytics is a widget — should be embeddable in reports and on dashboard, not a standalone page
  - Currently overlaps with /reports — both pull from same /v1/reports/* endpoints
  - Merge: Analytics becomes dashboard widget source (configurable), reports become saved/generated output from report-builder

### /technologist
- **Status:** OBSERVED
- **Observations:**
  - CRITICAL STRATEGIC PAGE: This is the core differentiator that makes the software ultra-flexible across industries
  - DESIGN: Technologist = aggregation of Recipe + Technology (technological steps)
  - Should allow choosing templates from: recipes and technological steps
  - Can combine multiple templates: just bread bakery, bread + pastry, or combined bread + pastry + fast food
  - This customizability is what enables serving ALL industries (not just bakeries)
  - If recipes and technological steps can be fully custom, the platform becomes industry-agnostic
  - Current implementation needs to be reviewed against this vision
  - i18n: Likely all English (consistent pattern)

### /admin
- **Status:** PENDING
- **Observations:** 

### /products
- **Status:** PENDING
- **Observations:** 

### /departments
- **Status:** PENDING
- **Observations:** 

### /recipes
- **Status:** PENDING
- **Observations:** 

### /subscriptions
- **Status:** PENDING
- **Observations:** 

### /tenant-management
- **Status:** PENDING
- **Observations:** 

### /notification-templates
- **Status:** PENDING
- **Observations:** 

### /exchange-rates
- **Status:** PENDING
- **Observations:** 

### /mobile-admin
- **Status:** PENDING
- **Observations:** 
