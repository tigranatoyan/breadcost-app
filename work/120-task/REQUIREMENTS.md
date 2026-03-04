# BreadCost App — Full Requirements Document
**Version:** 1.0  
**Date:** 2026-03-04  
**Status:** Draft — Release Mapping Complete  

---

## Table of Contents
1. [Domain 1 — Customer & Order Management](#domain-1)
2. [Domain 2 — Customer App & Loyalty](#domain-2)
3. [Domain 3 — Production Planning](#domain-3)
4. [Domain 4 — Recipe & Product Management](#domain-4)
5. [Domain 5 — Inventory & Warehouse Management](#domain-5)
6. [Domain 6 — Supplier Management](#domain-6)
7. [Domain 7 — Delivery & Shipping](#domain-7)
8. [Domain 8 — Point of Sale](#domain-8)
9. [Domain 9 — Bookkeeping & Cost Tracking](#domain-9)
10. [Domain 10 — Reporting & Dashboard](#domain-10)
11. [Domain 11 — Configuration & Access Management](#domain-11)
12. [Domain 12 — AI Module (Premium)](#domain-12)
13. [Global Non-Functional Requirements](#global-nfr)

---

## Domain 1 — Customer & Order Management <a name="domain-1"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-1.1 | The system shall allow an operator to create an order on behalf of a registered B2B customer, selecting products, quantities, and requested delivery date/time. | F1.1 |
| FR-1.2 | The system shall allow orders to be received via WhatsApp, parsed by AI, and presented as a draft order for operator review and confirmation. | F1.2 |
| FR-1.3 | The AI bot shall conduct a two-way WhatsApp conversation with the customer to confirm order details, suggest upsells, and handle modifications. | F1.3 |
| FR-1.4 | The upsell function of the AI bot shall be configurable per customer — it can be disabled if the customer has opted out. | F1.3 |
| FR-1.5 | When a customer requests human interaction during an AI-driven order conversation, the system shall trigger an alert/call task to the assigned operator. | F1.4 |
| FR-1.6 | The system shall enforce a configurable daily order cutoff time (default window 8–10 PM). After the cutoff, order creation shall be blocked for standard orders. | F1.5 |
| FR-1.7 | The system shall allow rush orders to be created after the cutoff. Rush orders shall be automatically flagged and a configurable price premium (%) shall be applied. | F1.6 |
| FR-1.8 | The rush order price premium shall be overridable manually by an authorized user at order creation time. | F1.6 |
| FR-1.9 | When a product from a department with a configured lead time cannot meet the customer's requested delivery time, the system shall notify the customer of the earliest available time and require explicit acceptance. | F1.7 |
| FR-1.10 | If the customer does not accept the lead time for a line item, that item shall be automatically dropped from the order, and the customer shall be notified. | F1.7 |
| FR-1.11 | An order may contain products from multiple departments with different lead times. The system shall present each conflict independently for customer resolution. | F1.8 |
| FR-1.12 | For multi-department orders with different delivery times, the customer shall choose between: (a) split delivery with courier charge, or (b) consolidated delivery at the later time. | F1.8 |
| FR-1.13 | Courier charges for split delivery shall be calculated and presented to the customer before confirmation. | F1.8 |
| FR-1.14 | Management shall be able to waive courier charges on an order. A waiver shall require management-level authorization and shall be logged. | F1.9 |
| FR-1.15 | The system shall allow order modification (add/remove/change line items) within a configurable window before the cutoff. | F1.10 |
| FR-1.16 | The system shall allow order cancellation by the operator within a configurable window. Cancellations after production has started shall require management approval. | F1.10 |
| FR-1.17 | The system shall provide real-time order status tracking visible to the operator and (in future) the customer. Statuses: Draft, Confirmed, In Production, Ready, Out for Delivery, Delivered, Cancelled. | F1.11 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-1.1 | Order creation and confirmation response time shall be under 2 seconds under normal load. |
| NFR-1.2 | WhatsApp AI parsing shall return a draft order within 5 seconds of receiving a complete message. |
| NFR-1.3 | Order data shall be persisted immediately upon creation. No data loss on system restart. |
| NFR-1.4 | All order state transitions shall be recorded in the audit trail with timestamp and actor. |
| NFR-1.5 | Concurrent order creation by multiple operators shall not result in duplicate orders or race conditions. |

---

## Domain 2 — Customer App & Loyalty <a name="domain-2"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-2.1 | The system shall provide a web-based customer portal (mobile iOS/Android in future releases). | F2.1 |
| FR-2.2 | Customers shall be able to register with name, contact details, and delivery address(es). | F2.1 |
| FR-2.3 | Customers shall be able to log in securely and manage their profile. | F2.1 |
| FR-2.4 | The customer portal shall display the product catalog with current prices applicable to that customer (global price list or customer-specific override). | F2.2 |
| FR-2.5 | Customers shall be able to place orders via the portal subject to cutoff rules and lead time constraints. | F2.3 |
| FR-2.6 | The system shall award loyalty points to customers for each completed purchase, based on configurable rules (e.g., points per currency unit spent). | F2.4 |
| FR-2.7 | The loyalty program shall support configurable tiers (e.g., Bronze, Silver, Gold). Tier advancement rules (point thresholds, number of rush orders, etc.) shall be configurable from the UI. | F2.5 |
| FR-2.8 | Tier benefits (discounts, free items, priority, etc.) shall be configurable per tier. | F2.5 |
| FR-2.9 | Customers shall be able to redeem loyalty points at checkout against eligible orders. | F2.6 |
| FR-2.10 | The customer portal shall display loyalty point balance, tier status, and points history. | F2.7 |
| FR-2.11 | The customer portal shall display full order history with status and invoice details. | F2.7 |
| FR-2.12 | The customer portal shall display real-time status of active orders. | F2.8 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-2.1 | The customer portal shall be responsive and usable on desktop and mobile browsers. |
| NFR-2.2 | Customer authentication shall use secure password hashing and support future OAuth/SSO integration. |
| NFR-2.3 | Loyalty point calculations shall be transactionally consistent — no points awarded for cancelled/refunded orders. |
| NFR-2.4 | The portal shall load the product catalog within 3 seconds. |

---

## Domain 3 — Production Planning <a name="domain-3"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-3.1 | The system shall automatically generate a production plan nightly based on all confirmed orders for the next delivery window, taking into account per-department lead times and delivery deadlines. | F3.1 |
| FR-3.2 | The production plan shall aggregate quantities per product per department and calculate required material quantities using the current active recipe and expected yield. | F3.1 |
| FR-3.3 | The system shall check material availability against the plan and flag shortfalls before the plan is approved. | F3.1 |
| FR-3.4 | A manager shall be able to review, adjust, and approve the production plan before it is released to the production floor. | F3.2 |
| FR-3.5 | Upon approval, each department shall receive a work order detailing: products to produce, quantities, batch sizes, recipe steps, and material issue instructions. | F3.3 |
| FR-3.6 | The production team shall be able to mark a batch as started and completed via the system UI. | F3.4 |
| FR-3.7 | Upon batch completion, the production team shall record actual yield. The system shall compare actual vs expected yield and flag variances. | F3.5 |
| FR-3.8 | If a production delay or shortfall is reported, the system shall alert management via in-app notification and WhatsApp message. | F3.6 |
| FR-3.9 | Management shall be able to manually halt a production line (e.g., due to material shortage) from the UI. Affected orders shall be flagged and customers notified. | F3.7 |
| FR-3.10 | After a halt or shortage event, the system shall recalculate and present an updated production plan for manager re-approval. | F3.8 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-3.1 | Production plan generation shall complete within 30 seconds regardless of order volume. |
| NFR-3.2 | Work orders shall be available to department terminals immediately upon plan approval. |
| NFR-3.3 | Plan generation shall be idempotent — re-running for the same window shall produce the same result unless input data has changed. |
| NFR-3.4 | All plan approval and halt actions shall be logged in the audit trail. |

---

## Domain 4 — Recipe & Product Management <a name="domain-4"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-4.1 | The system shall allow a Technologist to create and manage recipes. Each recipe shall define: product name, department, ingredients with quantities, units, yield, and waste factor per ingredient. | F4.1 |
| FR-4.2 | Ingredients shall support three unit modes: (a) by piece, (b) by weight, (c) combination (piece count + weight per piece = total weight). | F4.1 |
| FR-4.3 | Each ingredient in a recipe shall define its purchasing unit (e.g., 400g block) and its recipe unit (e.g., piece of 4g), with a configurable conversion ratio. | F4.4 |
| FR-4.4 | The system shall calculate total ingredient consumption for a batch as: (recipe quantity × batch size × waste factor), then convert to purchasing units using the conversion ratio. | F4.5 |
| FR-4.5 | Recipe yield shall be defined as the expected output quantity (pieces or weight) for a given input batch size. Yield is used in production planning unit calculations. | F4.6 |
| FR-4.6 | Recipes shall be versioned. Editing a recipe creates a new version; the previous version is retained. A recipe version can be set as active per product. | F4.2 |
| FR-4.7 | Each product shall be linked to exactly one active recipe version per department. | F4.3 |
| FR-4.8 | Products shall be classified as sold by piece, by weight, or both. Pricing shall reflect the unit of sale. | F4.1 |
| FR-4.9 | Only users with the Technologist role (or equivalent configured permission) shall be able to create or modify recipes. | F4.1 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-4.1 | Recipe version history shall be retained indefinitely and accessible for audit. |
| NFR-4.2 | Changing the active recipe version on a product shall not affect batches already in production — those shall continue using the recipe version at the time of batch creation. |
| NFR-4.3 | Unit conversion calculations shall use at minimum 4 decimal places to avoid rounding errors in cost computation. |

---

## Domain 5 — Inventory & Warehouse Management <a name="domain-5"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-5.1 | The system shall allow receiving a raw material lot from a supplier, recording: item, quantity, unit, cost per unit, currency, exchange rate to main currency, and supplier reference. | F5.1 |
| FR-5.2 | Each received lot shall be assigned a unique lot ID and stored with its cost basis for FIFO valuation. | F5.1 |
| FR-5.3 | The system shall issue materials to a production batch using FIFO (oldest lot consumed first). Partial lot consumption shall be supported. | F5.2 |
| FR-5.4 | The system shall support inventory transfer between departments when warehouse mode is configured as shared. | F5.3 |
| FR-5.5 | The system shall support inventory adjustment entries (waste, spoilage, correction) with mandatory reason codes. | F5.4 |
| FR-5.6 | FIFO cost layer tracking shall be maintained per item per lot. Each consumption transaction shall record which lot(s) were consumed and at what cost. | F5.5 |
| FR-5.7 | The system shall maintain a configurable minimum stock threshold per item per department (or warehouse). When stock falls below the threshold, an alert shall be triggered. | F5.6 |
| FR-5.8 | Stock alerts shall be sent via in-app notification and WhatsApp to configured recipients. | F5.6, F5.9 |
| FR-5.9 | Warehouse configuration (shared vs isolated per department) shall be set at setup and modifiable from the UI. Cost calculations shall always remain department-specific regardless of warehouse mode. | F5.10 |
| FR-5.10 | Management shall be able to manually allocate or block inventory to a specific department, overriding the automatic FIFO allocation. | F5.8 |
| FR-5.11 | All inventory movements (receipts, issues, transfers, adjustments) shall be recorded as immutable ledger entries with full traceability. | F5.1–F5.5 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-5.1 | FIFO cost calculations shall be accurate to 4 decimal places in main currency. |
| NFR-5.2 | Inventory ledger entries shall be immutable. Corrections shall be made via adjustment entries, never by modifying existing records. |
| NFR-5.3 | Stock level queries shall reflect real-time state with no more than 1-second lag. |
| NFR-5.4 | Concurrent material issues to multiple batches shall not result in negative stock or double consumption of the same lot. |

---

## Domain 6 — Supplier Management <a name="domain-6"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-6.1 | The system shall maintain a supplier catalog with: supplier name, contact details, items supplied, price per item, and lead time. | F6.1 |
| FR-6.2 | The system shall generate purchase order (PO) suggestions when stock falls below the configured minimum threshold, based on consumption rate and lead time. | F6.2 |
| FR-6.3 | A manager shall be able to review, adjust quantities, and approve a PO suggestion before it is actioned. | F6.3 |
| FR-6.4 | An approved PO shall be exportable to Excel (formatted). Future: sent directly via supplier API. | F6.4 |
| FR-6.5 | When a supplier delivery is received, it shall be matched against the open PO. Discrepancies (quantity, item, price) shall be flagged for review. | F6.5 |
| FR-6.6 | All purchase transactions in foreign currencies shall record the exchange rate at the time of receipt. The system shall convert to main currency for bookkeeping. | F6.6 |
| FR-6.7 | Exchange rates shall be entered manually per transaction. Future: pulled automatically from an exchange rate API. | F6.6 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-6.1 | PO export to Excel shall complete within 5 seconds. |
| NFR-6.2 | All PO approval actions shall be logged in the audit trail. |
| NFR-6.3 | The system shall support future API integration with suppliers without structural changes to the PO domain. |

---

## Domain 7 — Delivery & Shipping <a name="domain-7"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-7.1 | The system shall assign confirmed orders to delivery runs based on delivery time windows and driver availability. | F7.1 |
| FR-7.2 | The system shall generate a delivery manifest per driver/run listing: orders, customers, addresses, items, and quantities. | F7.2 |
| FR-7.3 | The driver shall be able to mark each delivery as completed (initially via operator/system; later via driver app). | F7.3 |
| FR-7.4 | The system shall support failed delivery recording with reason and triggering of a return or re-delivery workflow. | F7.4 |
| FR-7.5 | When a customer opts for split delivery, the system shall calculate and apply a courier charge to the secondary delivery. | F7.5 |
| FR-7.6 | Management shall be able to waive courier charges on a delivery. The waiver shall require management authorization and be logged. | F7.6 |
| FR-7.7 | The system shall be designed to integrate a future driver mobile app for real-time delivery tracking, packaging confirmation, and on-spot payment collection. | F7.7 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-7.1 | Delivery manifests shall be printable and exportable to PDF. |
| NFR-7.2 | All delivery status updates shall be timestamped and logged. |
| NFR-7.3 | The delivery domain shall be designed as a modular bounded context to allow plugging in the driver app without re-architecting core order/delivery data. |

---

## Domain 8 — Point of Sale <a name="domain-8"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-8.1 | The system shall provide a POS interface for walk-in B2C sales, allowing product selection and quantity entry. | F8.1 |
| FR-8.2 | The POS shall display current price list and apply any applicable discounts or loyalty redemptions. | F8.2 |
| FR-8.3 | The POS shall support payment by cash and card. | F8.3 |
| FR-8.4 | The system shall generate and display a receipt on completion of a sale. Receipts shall be printable. | F8.4 |
| FR-8.5 | The system shall support refunds and exchanges, with mandatory reason recording. | F8.5 |
| FR-8.6 | The system shall support end-of-day POS reconciliation: total cash, card, refunds, and net sales. | F8.6 |
| FR-8.7 | POS sales shall deduct from real-time inventory (finished goods). | F8.1 |
| FR-8.8 | The POS shall be extensible to support driver payment collection in a future release. | F8.7 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-8.1 | POS transaction processing shall complete within 1 second. |
| NFR-8.2 | POS shall remain operable offline for short periods, with transaction sync when connectivity is restored. |
| NFR-8.3 | All POS transactions shall be logged with cashier ID, timestamp, and items. |

---

## Domain 9 — Bookkeeping & Cost Tracking <a name="domain-9"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-9.1 | All material receipts shall be recorded with cost in original currency and automatically converted to main currency using the exchange rate at receipt time. | F9.1 |
| FR-9.2 | The system shall calculate the actual cost of each production batch as the sum of FIFO-valued material issues for that batch. | F9.2 |
| FR-9.3 | The cost of goods produced for each product shall be derivable from: actual material cost + waste cost, normalized per unit of yield. | F9.3 |
| FR-9.4 | The system shall compute variance reports comparing planned material consumption (from recipe) vs actual consumption per batch and per period. | F9.4 |
| FR-9.5 | The system shall compute inventory valuation (FIFO cost of stock on hand) at any point in time. | F9.5 |
| FR-9.6 | The system shall support accounting period close: locking all transactions in the period, generating period-end reports, and preventing backdated entries after close. | F9.6 |
| FR-9.7 | Exchange rates shall be stored per transaction. A separate exchange rate table shall allow manual entry of rates per currency per date. Future: consumed from external API. | F9.7 |
| FR-9.8 | B2B customer invoices shall be generated from confirmed and delivered orders, with configurable payment terms (net 7, net 14, net 30, etc.) per customer. | F9.8 |
| FR-9.9 | The system shall track invoice payment status (unpaid, partially paid, paid, overdue). | F9.8 |
| FR-9.10 | When a customer's outstanding balance exceeds their credit limit or an invoice is overdue by a configurable number of days, the system shall automatically block new order creation for that customer and alert the finance role. | F9.9 |
| FR-9.11 | Customer-specific pricing and discount rules shall be stored and applied at order creation. History of price changes shall be retained. | F9.10 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-9.1 | All financial calculations shall be performed in main currency with 2 decimal place precision for display and 4 decimals for intermediate calculations (to prevent rounding drift). |
| NFR-9.2 | Period close shall be an atomic operation — either all records are locked or none are. |
| NFR-9.3 | Invoice and payment records shall never be deleted; reversals shall be made via credit notes or adjustment entries. |
| NFR-9.4 | All financial entries shall be traceable to the originating event (receipt, batch issue, sale, adjustment). |

---

## Domain 10 — Reporting & Dashboard <a name="domain-10"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-10.1 | The system shall provide a management dashboard displaying: today's orders, production plan status, stock alert count, revenue (day/week/month), top-selling products. | F10.1 |
| FR-10.2 | The system shall provide a report constructor where users can select and combine predefined KPI blocks to build custom reports. | F10.2 |
| FR-10.3 | The following standard reports shall be available as predefined KPI blocks: gross revenue, net revenue, COGS, gross margin, profitability by product/department, inventory turnover, variance (plan vs actual), customer balance summary, top customers by revenue. | F10.3 |
| FR-10.4 | Reports shall support date range filtering (day, week, month, custom range) and filters by department, product, and customer. | F10.3 |
| FR-10.5 | All reports shall be viewable in-app with table and chart visualizations. | F10.4 |
| FR-10.6 | Reports shall be exportable to Excel and PDF from the in-app view. | F10.5 |
| FR-10.7 | Access to specific reports shall be governed by the user's role and subscription tier. | F10.3 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-10.1 | Standard reports shall render within 5 seconds for up to 12 months of data. |
| NFR-10.2 | The report constructor shall be extensible — new KPI blocks shall be addable without UI changes. |
| NFR-10.3 | Report data shall reflect the state as of the last completed transaction. Real-time accuracy is preferred but eventual consistency (< 30s lag) is acceptable for reports. |

---

## Domain 11 — Configuration & Access Management <a name="domain-11"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-11.1 | The system shall allow an Admin to create and configure departments from the UI. Department configuration includes: name, product types produced, lead time, warehouse mode (shared/isolated), assigned recipes, and staff. | F11.1 |
| FR-11.2 | The system shall support up to 10 departments per tenant. This limit shall be configurable at the system level. | F11.1 |
| FR-11.3 | The system shall provide a role management UI where roles are defined with granular action-level permissions (e.g., "create order", "approve PO", "close period"). Access layers (what data is visible) shall also be configurable per role. | F11.2 |
| FR-11.4 | Staff accounts shall be manageable from the UI: create, edit, deactivate, assign role. | F11.3 |
| FR-11.5 | Order cutoff time and rush order premium percentage shall be configurable from the UI by Admin or Manager. | F11.4 |
| FR-11.6 | Loyalty tier rules (tier names, thresholds, benefits, point earning rates) shall be fully configurable from the UI. | F11.5 |
| FR-11.7 | Subscription tier assignment per tenant shall be manageable by a super-admin. Feature access shall be enforced based on the assigned tier (Basic / Professional / Premium+AI). | F11.6 |
| FR-11.8 | The system shall maintain a complete audit trail of all significant actions (order state changes, approvals, configuration changes, financial entries, role changes). Each entry shall record: actor, action, timestamp, before/after state. | F11.7 |
| FR-11.9 | The audit trail shall be read-only and accessible to Admin and Finance roles. | F11.7 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-11.1 | Configuration changes shall take effect immediately without requiring a system restart. |
| NFR-11.2 | Role and permission checks shall be enforced at the API layer, not only at the UI layer. |
| NFR-11.3 | The audit trail shall be stored in an append-only structure with no update or delete capability. |
| NFR-11.4 | The system shall support multi-tenancy: each tenant's data shall be fully isolated. |

---

## Domain 12 — AI Module (Premium) <a name="domain-12"></a>

### Functional Requirements

| ID | Requirement | Flows |
|----|-------------|-------|
| FR-12.1 | The AI module shall process incoming WhatsApp messages, extract order intent, conduct a confirmation conversation, and optionally present upsell suggestions. The upsell capability shall be togglable per customer. | F12.1 |
| FR-12.2 | The AI bot shall support two-way conversation to handle clarifications, alternatives (e.g., lead time conflicts), and collect explicit customer acceptance. | F12.1 |
| FR-12.3 | The AI module shall analyze historical consumption data and generate weekly/monthly replenishment hints per item, displayed to the purchasing/warehouse manager. | F12.2 |
| FR-12.4 | The AI module shall provide production planning suggestions: optimal batch sizes, sequencing, and resource allocation based on order history and trends. | F12.3 |
| FR-12.5 | The AI module shall suggest pricing adjustments or customer-specific discounts based on purchase volume, frequency, and margin data. | F12.4 |
| FR-12.6 | The AI module shall surface anomaly alerts in reports (e.g., unexpected cost spike, unusual variance, revenue drop) with brief explanations. | F12.5 |
| FR-12.7 | The AI module shall provide demand forecasting per product per period, presented as a planning aid to management. | F12.6 |
| FR-12.8 | All AI suggestions shall be advisory only — no AI action shall modify data without explicit human confirmation. | F12.1–F12.7 |
| FR-12.9 | The AI module shall be a separable premium add-on. Its unavailability shall not affect the core system operation. | F12.1–F12.7 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-12.1 | AI module failures shall degrade gracefully — the system shall continue functioning without AI features and display an appropriate status indicator. |
| NFR-12.2 | AI suggestions shall include a confidence score or explanation where applicable. |
| NFR-12.3 | WhatsApp AI response latency shall be under 5 seconds for a standard order conversation turn. |
| NFR-12.4 | AI training data shall be per-tenant and shall not be shared or cross-contaminated between tenants. |

---

## Global Non-Functional Requirements <a name="global-nfr"></a>

### Performance
| ID | Requirement |
|----|-------------|
| NFR-G.1 | All primary user-facing operations (order creation, inventory issue, POS sale) shall respond within 2 seconds at p95 under expected peak load. |
| NFR-G.2 | The system shall support at least 50 concurrent users per tenant without degradation. |
| NFR-G.3 | Batch processing (production plan generation, report rendering) shall not degrade interactive response times. |

### Security
| ID | Requirement |
|----|-------------|
| NFR-G.4 | All API endpoints shall require authentication. Unauthenticated requests shall return HTTP 401. |
| NFR-G.5 | All authorization checks shall be enforced server-side. Role and permission definitions shall not be trusted from client input. |
| NFR-G.6 | Passwords shall be stored using a strong adaptive hashing algorithm (bcrypt or equivalent). |
| NFR-G.7 | All data in transit shall be encrypted via TLS 1.2+. |
| NFR-G.8 | Sensitive financial and personal data at rest shall be encrypted. |
| NFR-G.9 | Session tokens shall expire after a configurable idle timeout. |

### Reliability & Data Integrity
| ID | Requirement |
|----|-------------|
| NFR-G.10 | The system shall use an event-sourced architecture for all domain state changes, ensuring a full and immutable history of all events. |
| NFR-G.11 | Commands that modify state shall be idempotent — resubmitting the same command shall not produce duplicate effects. |
| NFR-G.12 | The system shall support database backups with a recovery point objective (RPO) of no more than 1 hour. |
| NFR-G.13 | The system shall target 99.5% uptime (excluding planned maintenance windows). |

### Scalability & Extensibility
| ID | Requirement |
|----|-------------|
| NFR-G.14 | The system shall be multi-tenant from inception. Tenant isolation shall be enforced at the data and application layer. |
| NFR-G.15 | The system shall be designed to allow new modules (e.g., driver app, supplier API, exchange rate API) to be added without re-architecting existing bounded contexts. |
| NFR-G.16 | The subscription tier enforcement mechanism shall be a cross-cutting concern implemented at the API/service layer and configurable without code deployment. |

### Observability
| ID | Requirement |
|----|-------------|
| NFR-G.17 | The system shall emit structured logs for all significant operations, errors, and state transitions. |
| NFR-G.18 | The system shall expose health check and readiness endpoints. |
| NFR-G.19 | Critical alerts (stock shortage, production halt, overdue customer) shall be delivered via both in-app notification and WhatsApp message to configured recipients. |

### Internationalization
| ID | Requirement |
|----|-------------|
| NFR-G.20 | The system shall support multi-currency input with a single main bookkeeping currency per tenant. |
| NFR-G.21 | The UI shall be designed to support localization (i18n) in future releases. Date, time, and number formats shall use locale-aware formatting. |

---

## Subscription Tier Feature Matrix
*(To be finalized once full FR list is approved)*

| Feature Area | Basic | Professional | Premium + AI |
|---|---|---|---|
| Order Management | ✅ | ✅ | ✅ |
| Production Planning | ✅ | ✅ | ✅ |
| Inventory Management | ✅ | ✅ | ✅ |
| Recipe Management | ✅ | ✅ | ✅ |
| Supplier Management (manual) | ✅ | ✅ | ✅ |
| Supplier API Integration | ❌ | ✅ | ✅ |
| POS | ✅ | ✅ | ✅ |
| Customer App & Loyalty | ❌ | ✅ | ✅ |
| Advanced Reporting Constructor | ❌ | ✅ | ✅ |
| Basic Reports (P&L, Margin, Revenue) | ✅ | ✅ | ✅ |
| Exchange Rate API | ❌ | ✅ | ✅ |
| WhatsApp AI Order Intake | ❌ | ❌ | ✅ |
| AI Replenishment Hints | ❌ | ❌ | ✅ |
| AI Demand Forecasting | ❌ | ❌ | ✅ |
| AI Pricing Suggestions | ❌ | ❌ | ✅ |
| AI Report Anomaly Alerts | ❌ | ❌ | ✅ |
| Driver App | ❌ | Future | Future |

---

---

## Release Mapping

### Release 1 — Core MVP
**Goal:** A working factory management system covering daily operations: orders, recipes, production planning, inventory (FIFO), cost tracking, POS, configuration and basic reporting. No external integrations required.

#### Flows Included
| Domain | Flows |
|--------|-------|
| Order Management | F1.1, F1.5, F1.6, F1.7, F1.8, F1.9, F1.10, F1.11 |
| Production Planning | F3.1, F3.2, F3.3, F3.4, F3.5, F3.6, F3.7, F3.8 |
| Recipe & Product Management | F4.1, F4.2, F4.3, F4.4, F4.5, F4.6 |
| Inventory & Warehouse | F5.1, F5.2, F5.3, F5.4, F5.5, F5.6, F5.8, F5.9, F5.10, F5.11 |
| Bookkeeping & Cost | F9.1, F9.2, F9.3, F9.4, F9.5, F9.6, F9.7 (manual rates) |
| POS | F8.1, F8.2, F8.3, F8.4, F8.5, F8.6 |
| Reporting & Dashboard | F10.1, F10.4, F10.5, F10.6 (basic standard reports) |
| Config & Access Management | F11.1, F11.2, F11.3, F11.4, F11.5, F11.7, F11.8, F11.9 |

#### FRs Included
| Domain | FRs |
|--------|-----|
| Order Management | FR-1.1, FR-1.6 → FR-1.17 |
| Production Planning | FR-3.1 → FR-3.10 |
| Recipe & Product | FR-4.1 → FR-4.9 |
| Inventory | FR-5.1 → FR-5.11 |
| Bookkeeping | FR-9.1 → FR-9.7 |
| POS | FR-8.1 → FR-8.6 |
| Reporting | FR-10.1, FR-10.4, FR-10.5, FR-10.6, FR-10.7 (role-gating only) |
| Config & Access | FR-11.1 → FR-11.9 |
| Global NFRs | NFR-G.1 → NFR-G.19 (all apply from day 1) |

#### Explicitly Out of R1
- Customer portal and loyalty
- Supplier PO workflow (stock received manually via FR-5.1)
- Delivery management and courier charges
- Invoice / credit / customer blocking
- Report constructor (advanced)
- WhatsApp integration
- AI module
- Exchange rate API

---

### Release 2 — Growth
**Goal:** Add customer-facing capabilities, structured supplier and delivery workflows, B2B invoicing and credit control, and advanced reporting.

#### Flows Included
| Domain | Flows |
|--------|-------|
| Order Management | F1.2 (WhatsApp draft — operator reviews), F1.3 (partial bot), F1.4 (escalate to human) |
| Customer App & Loyalty | F2.1, F2.2, F2.3, F2.4, F2.5, F2.6, F2.7, F2.8 |
| Supplier Management | F6.1, F6.2, F6.3, F6.4 (Excel export), F6.5, F6.6, F6.7 (manual rates) |
| Delivery & Shipping | F7.1, F7.2, F7.3, F7.4, F7.5, F7.6 |
| Bookkeeping | F9.8, F9.9, F9.10, F9.11 |
| Reporting | F10.2, F10.3, F10.4 (full constructor + advanced KPIs) |
| Config | F11.6 (subscription tier management) |

#### FRs Included
| Domain | FRs |
|--------|-----|
| Order Management | FR-1.2, FR-1.3, FR-1.4, FR-1.5 |
| Customer App & Loyalty | FR-2.1 → FR-2.12 |
| Supplier Management | FR-6.1 → FR-6.7 |
| Delivery & Shipping | FR-7.1 → FR-7.6 |
| Bookkeeping | FR-9.8 → FR-9.11 |
| Reporting | FR-10.2, FR-10.3, FR-10.4 |
| Config | FR-11.6 |

#### Explicitly Out of R2
- Full AI WhatsApp conversation (bot handles end-to-end)
- AI replenishment / forecasting / pricing / anomaly
- Driver mobile app
- Driver POS
- Supplier API integration
- Exchange rate API
- Customer app iOS/Android

---

### Release 3 — AI + Mobile
**Goal:** Full AI layer across all modules (Premium tier), driver mobile app, external API integrations (supplier, exchange rate), mobile customer app.

#### Flows Included
| Domain | Flows |
|--------|-------|
| Order Management | F1.2 → F1.3 (full AI-driven WhatsApp conversation, upsell, confirmation) |
| Inventory | F5.7 (AI replenishment hints) |
| Supplier | F6.4 (supplier API), F6.7 (exchange rate API) |
| Delivery | F7.7 (driver app) |
| POS | F8.7 (driver POS) |
| Bookkeeping | F9.7 (exchange rate API) |
| Customer App | FR-2.1 mobile (iOS/Android) |
| AI Module | F12.1 → F12.6 (all AI flows) |

#### FRs Included
| Domain | FRs |
|--------|-----|
| AI Module | FR-12.1 → FR-12.9 |
| Supplier API | FR-6.4 (API part), FR-6.7 (API part) |
| Exchange Rate API | FR-9.7 (API part) |
| Delivery Driver App | FR-7.7 |
| POS Driver | FR-8.7, FR-8.8 |
| Customer App Mobile | FR-2.1 (mobile) |

---

## Release Summary

| Area | R1 MVP | R2 Growth | R3 AI + Mobile |
|---|---|---|---|
| Operator Order Entry | ✅ | ✅ | ✅ |
| Order Cutoff & Rush Orders | ✅ | ✅ | ✅ |
| WhatsApp Order Intake (AI) | ❌ | Partial (draft only) | ✅ Full AI |
| Production Planning | ✅ | ✅ | ✅ + AI suggestions |
| Recipe Management | ✅ | ✅ | ✅ |
| Inventory & FIFO | ✅ | ✅ | ✅ + AI hints |
| Cost Tracking & Period Close | ✅ | ✅ | ✅ |
| POS (in-store) | ✅ | ✅ | ✅ |
| POS (driver) | ❌ | ❌ | ✅ |
| Customer Portal (web) | ❌ | ✅ | ✅ |
| Customer App (iOS/Android) | ❌ | ❌ | ✅ |
| Loyalty Program | ❌ | ✅ | ✅ |
| Supplier Management (manual) | ❌ | ✅ | ✅ |
| Supplier API | ❌ | ❌ | ✅ |
| Delivery Management | ❌ | ✅ | ✅ |
| Driver App | ❌ | ❌ | ✅ |
| B2B Invoicing & Credit Control | ❌ | ✅ | ✅ |
| Exchange Rate (manual) | ✅ | ✅ | ✅ |
| Exchange Rate API | ❌ | ❌ | ✅ |
| Basic Reports & Dashboard | ✅ | ✅ | ✅ |
| Report Constructor (advanced) | ❌ | ✅ | ✅ |
| AI Module (all) | ❌ | ❌ | ✅ |
| Config & Role Management | ✅ | ✅ | ✅ |
| Audit Trail | ✅ | ✅ | ✅ |

---

*Next Step: Architecture review — compare R1 scope against existing codebase*
