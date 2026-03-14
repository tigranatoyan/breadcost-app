# BreadCost — Issue Backlog
**Source:** Arc-by-arc manual validation (2026-03-14)  
**Recovery:** `git tag pre-cleanup` or `git checkout pre-cleanup` to restore previous state  
**Raw notes:** [work/ARC_OBSERVATIONS.md](../work/ARC_OBSERVATIONS.md)

---

## Priority Legend
- **P0** — Crash / data loss / unusable feature
- **P1** — Functional bug affecting core workflow
- **P2** — UX / usability issue
- **P3** — i18n / cosmetic
- **P4** — Design improvement (future)

---

## Arc 1: Order Lifecycle

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A1-01 | P1 | BUG | /orders | Line Total shows 0.00 for all order lines — backend not calculating `qty × unitPrice` | Open |
| A1-02 | P3 | i18n | /orders | Status badges untranslated: DRAFT, CONFIRMED, IN_PRODUCTION, READY, OUT FOR DELIVERY, DELIVERED | Open |
| A1-03 | P3 | i18n | /orders | Column header "Տողի delays" — broken mixed-language translation | Open |
| A1-04 | P3 | i18n | /orders | "PCS" unit in English; RUSH badges in English | Open |
| A1-05 | P2 | FEATURE | /orders | DRAFT orders not editable — no edit button for lines, customer, delivery date | Open |
| A1-06 | P4 | DESIGN | /orders | Order lifecycle (READY→DELIVERED) driven from /orders, should be from /deliveries + /driver | Open |
| A1-07 | P1 | BUG | /deliveries | Marking "Out for Delivery" on /orders does NOT create a delivery run on /deliveries | Open |
| A1-08 | P2 | UX | /deliveries | "Assign Orders" requires typing raw UUIDs — should be selectable list of READY orders | Open |
| A1-09 | P2 | UX | /deliveries | Run ID displayed as raw UUID — should be sequential number | Open |
| A1-10 | P3 | i18n | /deliveries | All 15+ UI strings in English | Open |
| A1-11 | P1 | BUG | /driver | NOT IN NAVIGATION MENU — only accessible via direct URL | Open |
| A1-12 | P0 | BUG | /driver | Button labels show RAW i18n KEYS: "driver.refresh", "driver.lookup" | Open |
| A1-13 | P2 | UX | /driver | Requires typing raw UUIDs for Delivery Run ID & Session ID | Open |
| A1-14 | P4 | DESIGN | /driver | Should auto-show assigned delivery run for logged-in driver | Open |
| A1-15 | P3 | i18n | /driver | All text in English | Open |

---

## Arc 2: Production Planning

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A2-01 | P1 | BUG | /production-plans | Clicking "Create Plan" repeatedly creates duplicate plans for same date/shift — no idempotency | Open |
| A2-02 | P1 | BUG | /production-plans | "Generate Work Orders" on duplicate plans creates duplicate WOs from same orders | Open |
| A2-03 | P2 | UX | /production-plans | New plans generate 0 WOs (orders consumed) but no feedback to user | Open |
| A2-04 | P1 | BUG | /production-plans | Plan-level "Complete" button does nothing when clicked — no error, no feedback | Open |
| A2-05 | P1 | BUG | /production-plans | Yield shows "0.0h" — entered value not saved, or displays in wrong format (hours vs quantity) | Open |
| A2-06 | P1 | BUG | /production-plans | Plan completion does NOT auto-update linked order status — orders stuck at IN_PRODUCTION | Open |
| A2-07 | P2 | UX | /production-plans | Plans not sorted chronologically | Open |
| A2-08 | P2 | UX | /production-plans | No way to delete duplicate/empty plans | Open |
| A2-09 | P2 | UX | /production-plans | Only one plan expandable at a time — can't compare | Open |
| A2-10 | P4 | DESIGN | /production-plans | Approve without reject option — no way to send back to customer ops | Open |
| A2-11 | P4 | DESIGN | /production-plans | No auto-plan from confirmed orders — entire flow is manual | Open |
| A2-12 | P4 | DESIGN | /production-plans | "Mark Ready" on /orders is a production function — should auto-trigger from plan completion | Open |
| A2-13 | P3 | i18n | /production-plans | All status badges, headers, section names in English | Open |
| A2-14 | P4 | DESIGN | /floor | /floor is passive read-only — should be PRIMARY page for workers to START/COMPLETE WOs | Open |
| A2-15 | P1 | BUG | /floor | Date-locked to today — can't see other dates' plans; shows empty/orphaned plans | Open |
| A2-16 | P2 | UX | /floor | No date navigation | Open |
| A2-17 | P3 | i18n | /floor | All text in English | Open |

---

## Arc 3: Inventory & Supply Chain

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A3-01 | P1 | BUG | /suppliers | Currency defaults to UZS instead of AMD (Armenian Dram) | Open |
| A3-02 | P0 | BUG | /suppliers | "Auto-Suggest POs" returns HTTP 500 — backend crash | Open |
| A3-03 | P2 | UX | /suppliers | Create PO requires manually typing Ingredient ID — should be dropdown | Open |
| A3-04 | P2 | UX | /suppliers | Ingredient Name doesn't auto-populate from Ingredient ID | Open |
| A3-05 | P4 | DESIGN | /suppliers | Mixes admin (supplier setup) with operations (PO creation) | Open |
| A3-06 | P3 | i18n | /suppliers | All 20+ UI strings in English | Open |
| A3-07 | P2 | FEATURE | /inventory | No way to ADD new inventory items — only receive for existing 7 seed items | Open |
| A3-08 | P2 | FEATURE | /inventory | No bulk CSV/XLSX import for items or stock receipts | Open |
| A3-09 | P3 | i18n | /inventory | All UI text in English | Open |

---

## Arc 4: Customer Portal

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A4-01 | P1 | BUG | /customers | Product Catalog tab returns errors | Open |
| A4-02 | P2 | UX | /customers | Customer names truncated ("armen-s-", "gago-s-") | Open |
| A4-03 | P2 | UX | /customers | Order IDs shown as truncated UUIDs | Open |
| A4-04 | P4 | DESIGN | /customers | Customer Orders tab duplicates /orders — no unique value | Open |
| A4-05 | P2 | UX | /customers | Creating order requires manually typing customer name — should be dropdown | Open |
| A4-06 | P3 | i18n | /customers | All text in English | Open |
| A4-07 | P2 | UX | /loyalty | Balance lookup requires raw Customer ID — should be name/autocomplete | Open |

---

## Arc 5: POS

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A5-01 | — | SKIP | /pos | Skipped — requires hardware integrations (POS device, barcode scanner, scales, tax API) | Deferred |

---

## Arc 6: Financial Operations

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A6-01 | P0 | BUG | /reports | Page crashes: "Cannot read properties of undefined (reading 'length')" | Open |
| A6-02 | P4 | DESIGN | /reports | Too many indicators ("christmas tree") — should be filterable or from report-builder | Open |
| A6-03 | P4 | DESIGN | /report-builder | 3-tier subscription model (standard → correlation → AI) — needs implementation clarity | Open |
| A6-04 | — | SKIP | /invoices | Skipped — requires tax authority e-invoicing integration | Deferred |

---

## Arc 7: AI Assistance

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A7-01 | P4 | DESIGN | AI pages | AI features should be embedded in context pages, not standalone pages | Open |
| A7-02 | P4 | DESIGN | /ai-whatsapp | WhatsApp = priority first AI implementation (supplier comms + customer ordering) | Open |
| A7-03 | P4 | DESIGN | /quality-predictions | Should embed in production floor/plans context | Open |

---

## Arc 8: Platform Administration

| ID | P | Type | Page | Issue | Status |
|----|---|------|------|-------|--------|
| A8-01 | P4 | DESIGN | /dashboard | Dashboard should be configurable — widget selection from admin panel | Open |
| A8-02 | P4 | DESIGN | /analytics | Overlaps with /reports — merge into dashboard widgets | Open |
| A8-03 | — | — | /technologist | Observation incomplete (chat broke before finishing) | Open |

---

## Cross-Cutting

| ID | P | Type | Area | Issue | Status |
|----|---|------|------|-------|--------|
| X-01 | P1 | BUG | All pages | Pages accessible without logging in — no auth guard on frontend routes | Open |
| X-02 | P2 | UX | All pages | UUID inputs everywhere instead of searchable dropdowns (deliveries, driver, POs, loyalty) | Open |

---

## Summary

| Priority | Count | Description |
|----------|-------|-------------|
| P0 | 3 | Crashes / raw keys / unusable |
| P1 | 12 | Functional bugs in core workflows |
| P2 | 16 | UX / usability issues |
| P3 | 10 | i18n / cosmetic |
| P4 | 14 | Design improvements (future) |
| Deferred | 2 | Requires external integration |
| **Total** | **57** | |

## Fix Priority Order

1. **P0 first** (3): reports crash, supplier 500, driver raw keys
2. **P1 next** (12): line totals, duplicate plans, yield format, plan→order sync, floor date filter, auth guard, etc.
3. **P2 batch** (16): UUID→dropdown, missing features, sort/delete/compare UX
4. **P3 sweep** (10): i18n pass across all pages
5. **P4 roadmap** (14): architecture/design decisions for next release
