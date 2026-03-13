# BreadCost — ArcMap
**Version:** 1.1 | **Last Updated:** 2026-03-11 | **Status:** Active

---

## Actors

| Actor | Role | Primary Arcs |
|-------|------|-------------|
| **Customer** | B2B buyer (bakery client, restaurant, shop) | 1, 4, 8 |
| **Admin** | Tenant owner, full system access | 1, 2, 3, 5, 6, 7, 8 |
| **Manager** | Approves plans, views reports, manages operations | 1, 2, 3, 5, 6, 7 |
| **Floor Worker** | Executes work orders on production floor | 2 |
| **Technologist** | Manages recipes, technology steps, quality | 2, 3 |
| **Warehouse** | Receives goods, manages stock, transfers | 3 |
| **Cashier** | Walk-in POS sales | 5 |
| **Finance** | Invoicing, payments, credit control | 1, 6 |
| **Driver** | Delivers orders, tracks location | 1 |
| **AI Bot** | WhatsApp order intake, autonomous responses | 4 |
| **System** | Automated triggers (stock alerts, tier checks, forecasts) | 3, 7, 8 |

---

## Arc 1: Order Lifecycle — Customer places order → Order delivered & invoiced

**Actors:** Customer, Admin, Manager, Floor Worker, Driver, Finance
**Status:** `[R1-R3]` core CRUD + confirm + production link; `[R4]` portal checkout, tracking, timeline; `[R5]` email notifications on status change
**Depends on:** Arc 2 (production), Arc 3 (stock check), Arc 6 (invoicing)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Customer | Places order (portal or WhatsApp) | Order created (DRAFT), rush/lead-time checks applied | 2 | BC-201, BC-2804 |
| 2 | System | Validates pricing + lead time | Lead-time conflicts flagged, rush premium calculated. Orders go to production (night shift) or against existing stock — no stock reservation. | 3 | BC-203 |
| 3 | Admin/Manager | Reviews & confirms order | Status → CONFIRMED, event emitted | 4 | BC-204 |
| 4 | Manager | Creates production plan for date, generates WOs | WOs created per dept/product, linked to order lines | Arc 2 | BC-301–308 |
| 5 | Floor Worker | Executes work orders | WO status PENDING→IN_PROGRESS→COMPLETE | 6 | BC-301–308 |
| 6 | System | All WOs complete → plan complete | Order status → READY | 7 | BC-305 |
| 7 | Admin | Creates delivery run, assigns orders | DeliveryRunOrder created, courier charge split | 8 | BC-1401–1406 |
| 8 | Driver | Starts session, delivers to customer | Stop marked DELIVERED, GPS logged | 9 | BC-2101–2106 |
| 9 | Finance | Generates invoice from delivered order | Invoice DRAFT → ISSUED, outstanding balance updated | 10 | BC-1501–1505 |
| 10 | Customer | Pays invoice | Invoice → PAID, balance decremented, loyalty points awarded | — | BC-1503, BC-1201 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 1 | After cutoff hour | Auto-flag rush, apply premium % | BC-203 |
| 2 | Lead time conflict (recipe too slow) | Flag on order line, admin decides proceed/split | BC-203 |
| 3 | Admin cancels order | Status → CANCELLED, no production | BC-205 |
| 5 | Ingredient shortage mid-production | WO blocked from starting until stock resolved (→ Arc 3) | `[R5+]` BC-TBD |
| 8 | Delivery failed | Driver marks FAILED, re-assign to new run | BC-1405 |
| 9 | Customer over credit limit | Invoice blocked, admin notified | BC-1504 |
| 10 | Invoice overdue | Status → OVERDUE, visible in finance reports | BC-1505 |

### Cross-Arc Dependencies

- **Step 2** note: Orders trigger production, not stock deduction. Stock check is at production time (Arc 2).
- **Step 4** triggers: Arc 2 production planning
- **Step 7** triggers: Arc 1 delivery (sub-arc)
- **Step 9** requires: Arc 6 invoicing
- **Step 10** triggers: Arc 8 loyalty points award

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1 | 1–3 (staff only), 4–6 (basic) | Order CRUD + confirm + production link |
| R2 | 7–10 | Delivery, invoicing, loyalty wired |
| R3 | 1 (WhatsApp bot), 8 (driver app) | AI order intake, driver GPS tracking |
| R4 | 1 (portal checkout), order tracking timeline | BC-2804, BC-2806, BC-2903 |

---

## Arc 2: Production Planning — Confirmed orders → Finished goods

**Actors:** Manager, Floor Worker, Technologist
**Status:** `[DONE]` full lifecycle (R1)
**Depends on:** Arc 1 (confirmed orders as input), Arc 3 (ingredient availability)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Manager | Creates production plan (date + shift) | Empty plan (DRAFT) | 2 | BC-301 |
| 2 | Manager | Generates work orders | System aggregates confirmed orders by dept/product, creates WOs with batch counts | 3 | BC-302 |
| 3 | Manager | Approves/publishes plan | Status → APPROVED → PUBLISHED | 4 | BC-303, BC-304 |
| 4 | Manager | Starts plan | Status → IN_PROGRESS | 5 | BC-305 |
| 5 | Floor Worker | Views assigned WOs on floor view | WO queue shown per department | 6 | BC-306 |
| 6 | Floor Worker | Marks WO in-progress, follows tech steps | WO status → IN_PROGRESS, tech step checklist available | 7 | BC-401–408 |
| 7 | Floor Worker | Completes WO | WO → COMPLETE | 8 | BC-306 |
| 8 | System | All WOs done → plan auto-completes | Plan → COMPLETE, orders → READY | Arc 1.6 | BC-305 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 2 | No confirmed orders for date | Empty WO list, manager decides to wait or plan ahead | — |
| 6 | Recipe ingredient missing | **WO blocked** — system checks ingredient stock before WO can start, blocks if insufficient | `[R5+]` BC-TBD |
| 6 | Technology step unclear | Technologist updates recipe/steps, floor worker refreshes | BC-401–408 |
| 7 | Quality issue (yield too low) | Re-run batch (create new WO manually) | `[GAP]` no yield tracking |

### Cross-Arc Dependencies

- **Input:** Arc 1, step 3 (confirmed orders feed WO generation)
- **Step 6** needs: Arc 3 ingredient stock (**must block WO if insufficient** — `[R5+]`)
- **Step 6** uses: Recipe + technology steps (master data, not a separate arc)
- **Output:** Arc 1, step 6 (plan complete → orders READY)

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1 | 1–8 (full) | Core production lifecycle complete |
| R3 | AI production suggestions | BC-1902 — suggests optimal product mix |

---

## Arc 3: Inventory & Supply Chain — Stock management → Procurement → Receiving

**Actors:** Warehouse, Admin, Manager, System, Supplier (external)
**Status:** `[R1-R2]` manual CRUD + PO workflow; `[R3]` AI replenishment hints; `[R5]` auto-PO from stock alerts
**Depends on:** Arc 2 (production consumes stock), Arc 6 (supplier invoicing)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | System | Stock level drops below reorder point | Alert visible on dashboard + **auto-generate production order** | 2 | BC-501–507 |
| 2 | Warehouse | Reviews stock positions | Stock levels table with item/qty/status | 3 | BC-501 |
| 3 | Manager | Creates purchase order to supplier | PO created (DRAFT), lines with item/qty/price | 4 | BC-1301–1306 |
| 4 | Manager | Sends PO to supplier | PO status → SENT | 5 | BC-1303 |
| 5 | Supplier | Delivers goods | (External — out of system) | 6 | — |
| 6 | Warehouse | Receives goods (lot receipt) | ReceiveLotCommand → stock position updated, batch tracked | 7 | BC-502 |
| 7 | Warehouse | Transfers stock to department | TransferCommand → source decremented, destination incremented | — | BC-503 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 1 | No reorder point configured | No alert fires — manual monitoring only | `[GAP]` |
| 3 | No approved supplier for item | Manager manually finds supplier | `[GAP]` no supplier-item mapping |
| 1 | Stock low on finished product | **Auto-create production order** for restock (not PO) | `[R5+]` BC-TBD |
| 5 | Supplier delivers wrong qty | Warehouse receives partial, PO stays open | `[GAP]` partial receipt |
| 6 | Batch quality issue | Inventory adjustment (write-off) | BC-504 |

### Cross-Arc Dependencies

- **Step 1** triggered by: Arc 2 production consuming stock, Arc 5 POS sales
- **Step 3** may trigger: Arc 6 supplier invoicing (future)
- **Output:** Stock positions consumed by Arc 2 (production) and Arc 5 (POS)

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1 | 1–2, 6–7 | Stock positions, receive, transfer, adjust |
| R2 | 3–5 | Supplier management + PO workflow |
| R3 | AI hints | BC-1901 replenishment suggestions, BC-1903 demand forecast |

---

## Arc 4: Customer Self-Service Portal — Registration → Ordering → Tracking → Loyalty

**Actors:** Customer, System, Admin (escalation), AI Bot
**Status:** `[R2]` BE registration/login/catalog; `[R4-S1]` security hardening (JWT RBAC, own-data enforcement, /me); `[R4]` full portal FE + tracking + loyalty dashboard
**Depends on:** Arc 1 (order lifecycle), Arc 8 (loyalty)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Customer | Registers on portal | Account created, default loyalty (Bronze, 0 pts) | 2 | BC-1101, BC-2801 |
| 2 | Customer | Logs in | JWT issued with customer role | 3 | BC-1102, BC-2801 |
| 3 | Customer | Browses catalog | Product grid with search/filter/sort | 4 | BC-2802, BC-2901 |
| 4 | Customer | Adds items to cart, proceeds to checkout | Cart in localStorage, checkout page shown | 5 | BC-2804 |
| 5 | Customer | Places order (delivery date, address, notes) | Order created (DRAFT) → enters Arc 1 | 6 | BC-2804 |
| 6 | Customer | Receives confirmation | Confirmation page with order # and summary | 7 | BC-2805 |
| 7 | Customer | Tracks order status (portal + WhatsApp) | Timeline: confirmed → production → ready → delivering → delivered. **Notified via both portal and WhatsApp.** | 8 | BC-2806, BC-2903 |
| 8 | Customer | Receives delivery | Driver marks DELIVERED (Arc 1.8) | 9 | — |
| 9 | Customer | Views loyalty dashboard | Points balance, tier progress, history | — | BC-2807 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 1 | Duplicate email | Error: "Account already exists" | BC-1101 |
| 2 | Wrong password | Error + forgot-password flow | BC-2902 |
| 4 | Product out of stock | `[GAP]` — no real-time stock check on catalog |
| 5 | Credit limit exceeded at invoice time | Order completes but invoice blocked (Admin resolves) | BC-1504 |
| 7 | Order delayed | Notify customer via **both portal + WhatsApp** | `[R5+]` BC-TBD |

### Cross-Arc Dependencies

- **Step 5** enters: Arc 1 (order lifecycle)
- **Step 8** from: Arc 1, step 8 (delivery)
- **Step 9** uses: Arc 8 (loyalty)

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R2 | 1–2 (BE only) | Registration + login endpoints |
| R3 | Alt path via WhatsApp bot | AI order intake (→ Arc 1) |
| R4 | 1–9 (full portal FE) | BC-2801–2809, BC-2901–2905 |

---

## Arc 5: POS & Walk-in Sales — Customer at counter → Payment → Receipt

**Actors:** Cashier, Customer (walk-in, not registered)
**Status:** `[DONE]` full lifecycle (R1)
**Depends on:** Arc 3 (inventory deducted after sale)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Cashier | Opens POS, selects products | Product grid displayed with prices | 2 | BC-601 |
| 2 | Cashier | Adds items, adjusts qty | Cart built with running total | 3 | BC-602 |
| 3 | Cashier | Selects payment method (Cash/Card) | Payment modal: amount tendered input | 4 | BC-603 |
| 4 | Cashier | Completes payment | Change calculated, sale recorded | 5 | BC-604 |
| 5 | System | Sale persisted | Order created (CONFIRMED + DELIVERED in one step), **inventory decremented** | 6 | BC-605 |
| 6 | Cashier | Prints/views receipt | Receipt preview | — | BC-606 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 3 | Insufficient cash | Cashier removes items or customer adds cash | — |
| 5 | Product not in stock | **POS checks ingredient stock and logs warning** (warn-only, not blocking) | `[R5]` Implemented |

### Cross-Arc Dependencies

- **Step 5** must decrement: Arc 3 inventory (**confirmed: POS must decrease stock** — `[R5+]` BC-TBD)
- **Step 5** feeds: Arc 7 reports (daily revenue)

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1 | 1–6 (full) | Complete POS flow |

---

## Arc 6: Financial Operations — Invoicing → Credit → Payments → Reporting

**Actors:** Finance, Admin, Customer
**Status:** `[DONE]` invoice lifecycle (R2); `[R4]` subscription enforcement; `[R5+]` email invoices
**Depends on:** Arc 1 (delivered orders trigger invoices)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Finance | Generates invoice from delivered order | Invoice DRAFT created with lines from order | 2 | BC-1501 |
| 2 | System | Validates customer credit | Outstanding balance + new invoice vs credit limit | 3 | BC-1504 |
| 3 | Finance | Issues invoice | DRAFT → ISSUED, issuedDate set, balance incremented | 4 | BC-1502 |
| 4 | Customer | Pays (external process) | Finance records payment | 5 | — |
| 5 | Finance | Marks invoice paid | ISSUED → PAID, balance decremented | 6 | BC-1503 |
| 6 | Finance | Reviews reports | Revenue, outstanding balances, overdue invoices | — | BC-701–705 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 2 | Credit limit exceeded | Invoice blocked, admin manually extends limit or collects payment | BC-1504 |
| 3 | Invoice disputed | `[GAP]` — no dispute workflow |
| 4 | Payment overdue | Invoice → OVERDUE, visible on reports | BC-1505 |

### Cross-Arc Dependencies

- **Input:** Arc 1, step 9 (delivered orders)
- **Step 2** reads: Customer credit data
- **Output:** Financial reports feed Arc 7 (dashboard)
- Exchange rates from R3 (BC-2201–2204) used for multi-currency display

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R2 | 1–6 (full) | Invoice CRUD + credit control + reporting |
| R3 | Exchange rates | Multi-currency support |

---

## Arc 7: AI Assistance — System generates insights → Staff acts on them

**Actors:** System (AI), Manager, Admin, Customer (via WhatsApp)
**Status:** `[R3]` all BE; `[R4]` visual refresh; `[R5+]` real ML models
**Depends on:** Arc 1 (order history), Arc 3 (stock data), Arc 4 (customer interactions)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1a | Manager | Triggers replenishment hints | AI analyzes sales + stock, generates reorder suggestions | 2 | BC-1901 |
| 1b | Manager | Triggers demand forecast | AI predicts next 7 days per product | 2 | BC-1903 |
| 1c | Manager | Triggers pricing suggestions | AI analyzes 90-day history, suggests price adjustments | 2 | BC-2001 |
| 1d | System | Triggers anomaly detection | AI compares recent vs baseline, flags spikes/churn | 2 | BC-2002 |
| 2 | Manager | Reviews AI suggestions | Accept, dismiss, or ignore each suggestion | 3 | BC-1901–2002 |
| 3 | Manager | Acts on accepted suggestion | Creates PO (→Arc 3), adjusts price, adjusts plan (→Arc 2) | — | — |

**WhatsApp sub-arc:**

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| W1 | Customer | Sends WhatsApp message | Webhook received, conversation created/resumed | W2 | BC-1801 |
| W2 | AI Bot | Parses intent (greeting/order/confirm/cancel/help) | Response generated based on intent | W3 | BC-1802 |
| W3 | AI Bot | Builds draft order from parsed text | Draft order with resolved products + quantities | W4 | BC-1803 |
| W4 | Customer | Confirms order | Draft committed → real order (→ Arc 1) | — | BC-1804 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 1a-d | Insufficient data (new tenant) | AI returns empty suggestions | — |
| W2 | Intent unrecognized (confidence < 0.5) | Escalate to human operator | BC-1805 |
| W3 | Product not found in catalog | Bot asks for clarification | BC-1803 |
| W4 | Customer cancels | Draft discarded, conversation continues | BC-1804 |

### Cross-Arc Dependencies

- **1a** reads: Arc 3 inventory levels + Arc 1 order history
- **1b** reads: Arc 1 order history (time series)
- **1c** reads: Arc 1 order history (volume + customer count)
- **W4** creates: Arc 1 order (from draft)

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R3 | All (BE only) | Rule-based AI, no real ML. FE pages built but basic. |
| R4 | WhatsApp flows sub-tab, visual refresh | BC-2809, BC-3211 |
| R5+ | Real ML models, email notifications | Replace regex with LLM |

---

## Arc 8: Platform Administration — Tenants, users, subscriptions, configuration

**Actors:** Admin, System
**Status:** `[R1-R2]` users + config + tiers; `[R4-S1]` RBAC @PreAuthorize on all v2/v3 controllers; `[R4]` enforcement wired; `[R5+]` billing
**Depends on:** All arcs (subscription gates features)

### Happy Path

| Step | Actor | Action | System Response | Next | Stories |
|------|-------|--------|-----------------|------|---------|
| 1 | Admin | Creates users with roles | Users added with RBAC roles | 2 | BC-801–807 |
| 2 | Admin | Configures tenant (currency, date format) | Tenant config persisted | 3 | BC-801 |
| 3 | Admin | Assigns subscription tier to tenant | Tier active, feature list set, user/product limits set | 4 | BC-1701 |
| 4 | System | Enforces feature gates on API calls | Blocked feature → 403 ERR_SUBSCRIPTION_REQUIRED | — | BC-3101 |
| 5 | System | Enforces user/product limits | Limit exceeded → 409 ERR_LIMIT_EXCEEDED | — | BC-3102 |
| 6 | Admin | Views platform dashboard | Stats, alerts, system health | — | BC-701 |

### Unhappy Paths

| Step | Failure | Recovery | Stories |
|------|---------|----------|---------|
| 1 | Max users reached (tier limit) | 409 error, admin upgrades tier | BC-3102 |
| 3 | Subscription expired | `[GAP]` — no expiry enforcement yet |
| 4 | Feature not in tier | 403 returned, FE hides nav item | BC-3101, BC-3103 |

### Cross-Arc Dependencies

- **Step 4** gates: Arc 7 (AI — ENTERPRISE only), Arc 1 delivery (STANDARD+), Arc 6 invoicing (STANDARD+)
- **Step 5** limits: Arc 8.1 user creation, Arc 2/5 product creation

### Release Coverage

| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1 | 1–2 | Users, config, roles |
| R2 | 3 | Tier CRUD + assignment (no enforcement) |
| R4 | 4–5 | BC-3101–3103 — actually wire enforcement |

---

## Cross-Arc Dependency Map

```
Arc 1 (Orders) ──creates──→ Arc 2 (Production) ──consumes──→ Arc 3 (Inventory)
     │                            │                               ↑
     │                            └──alerts──→ Arc 3 (restock)    │
     │                                                            │
     ├──delivers──→ Arc 6 (Invoicing) ──reports──→ Arc 7 (Dashboard)
     │                                                            │
     ├──tracks──→ Arc 4 (Portal: customer sees status)            │
     │                                                            │
     └──awarded──→ Arc 8 (Loyalty via Arc 4.9)                    │
                                                                  │
Arc 5 (POS) ──should decrement──→ Arc 3 (Inventory) [GAP]        │
                                                                  │
Arc 7 (AI) ──reads history from──→ Arc 1, Arc 3                  │
     │                                                            │
     └──creates orders via WhatsApp──→ Arc 1                      │
                                                                  │
Arc 8 (Platform) ──gates features──→ All Arcs                     │
     └──subscription enforcement──→ Arc 7 (AI), Arc 1 (delivery) ─┘
```

---

## Known Gaps (not assigned to any release)

| # | Gap | Discovered In | Impact |
|---|-----|--------------|--------|
| G-1 | ~~POS sales don't decrement inventory~~ | Arc 5 → Arc 3 | **Closed R5-S1**: BackflushConsumption per sale line |
| G-2 | ~~Production doesn't auto-check ingredient stock~~ | Arc 2, step 6 | **Closed R5-S1**: startWO blocks if insufficient; completeWO backflushes |
| G-3 | ~~No proactive delay notification to customer~~ | Arc 4, step 7 | **Closed R5-S2**: advanceStatus auto-notifies via MobileAppService |
| G-4 | ~~Invoice dispute workflow missing~~ | Arc 6, step 3 | **Closed R5-S3**: DISPUTED status + disputeInvoice/resolveDispute with credit note |
| G-5 | ~~Subscription expiry not enforced~~ | Arc 8, step 3 | **Closed R5-S3**: getFeatureAccess/getMaxUsers/getMaxProducts check expiry; deactivateExpired() + findExpiringSoon() |
| G-6 | ~~No auto-production-order from stock alerts~~ | Arc 3, step 1 | **Closed R5-S2**: StockAlertService + POST /v1/inventory/auto-plan |
| G-7 | ~~Catalog doesn't show real-time stock~~ | Arc 4, step 4 | **Closed R5-S2**: CatalogProduct.inStock field |
| G-8 | ~~Order doesn't auto-reserve stock~~ | Arc 1, step 2 | **Not needed:** orders trigger production, not stock deduction | Closed (by design) |
| G-9 | ~~No yield/quality tracking in production~~ | Arc 2, step 7 | **Closed R5-S3**: WO completion accepts actualYield/wasteQty/qualityScore; yieldVariancePct calculated |
| G-10 | ~~No supplier-item mapping~~ | Arc 3, step 3 | **Closed R5-S3**: preferred flag on catalog items; reverse lookup; auto-PO from plan material requirements |

---

## Summary

| Arc | Name | Status | Key Release |
|-----|------|--------|-------------|
| 1 | Order Lifecycle | `[R1-R3]` partial, `[R4]` portal + tracking, `[R5]` email notifications | R5 adds email on status change |
| 2 | Production Planning | `[DONE]` | R1 |
| 3 | Inventory & Supply Chain | `[R1-R2]` manual, `[R3]` AI hints, `[R5]` auto-PO | R5 auto-PO from stock alerts |
| 4 | Customer Self-Service Portal | `[R2]` BE stubs, `[R4-S1]` security, `[R4]` full portal | R4 is the key release |
| 5 | POS & Walk-in Sales | `[DONE]`, `[R5]` stock pre-check | R5 adds ingredient availability warning |
| 6 | Financial Operations | `[DONE]` | R2 |
| 7 | AI Assistance | `[R3]` BE done, `[R4]` visual refresh | R5+ for real ML |
| 8 | Platform Administration | `[R1-R2]` config, `[R4-S1]` RBAC, `[R4]` enforcement | R4 wires subscription gates |
