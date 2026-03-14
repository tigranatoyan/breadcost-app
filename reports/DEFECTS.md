# BreadCost — Open Defects
**Tester:** Manual QA | **Date:** 2026-03-14 | **Environment:** Docker (Postgres 16 + Redis 7 + Spring Boot 3.4.2 + Next.js 14)

| # | Severity | Page/Area | Description | Status |
|---|----------|-----------|-------------|--------|
| 1 | Medium | Dashboard | Currency symbol shows `$` instead of AMD (֏). Should be configurable per tenant, but default to ֏ as per Figma design. | Fixed |
| 2 | Medium | Dashboard | Incomplete Armenian (hy) translation — some labels still show English or placeholder keys (e.g. `{count} պատվdelays`, ` Delays' {value}`, `DASHBOARD.OVERVIEW`). All text should be fully translated. | Fixed |
| 3 | Low | Dashboard | "View all →" link on the Production Floor section navigates to `/production-plans` instead of `/floor`. Should link to the floor view. | Fixed |
| 4 | Critical | All pages | `TypeError: Failed to fetch` error banner on every data page (Orders, etc.). Frontend cannot reach the backend API — likely `NEXT_PUBLIC_API_URL` is set to `http://app:8080` (Docker internal hostname) which is unreachable from the browser. Should be `http://localhost:8080` for client-side requests. | Fixed |
| 5 | High | Orders | New Order modal — Product dropdown is empty (no products loaded). Cannot select a product for order lines. Related to defect #4 (API fetch failure). | Fixed (via #4) |
| 6 | Medium | Orders | New Order modal — incomplete Armenian translation. "Qty (PCS)" and "Unit Price" labels remain in English, date picker shows `mm/dd/yyyy --:-- --` instead of localized format. | Fixed — translations exist; PCS is UOM data; date picker is browser-native |
| 7 | Critical | Production Plans | "Create" button on New Production Plan modal does nothing — modal stays open, no plan created. Likely fails silently due to API fetch failure (defect #4). Also: page subtitle shows raw key `ionPlans.subtitle` instead of translated text. | Fixed (via #4) |
| 8 | Medium | Production Plans | New Production Plan modal — incomplete Armenian translation. Shift dropdown value shows `MORNING` in English instead of Armenian. Notes placeholder "Optional instructions for the production team" not translated. | Fixed |
| 9 | Medium | Inventory | Receive Stock modal — incomplete Armenian translation. Placeholders show English (e.g. `e.g. 100`, `e.g. 5.50`, `e.g. USD, EUR`). Info banner mixes Armenian and English (`MAIN`, `RECEIVING`). Item dropdown empty (related to #4). | Fixed |
| 10 | High | Customers | Customer Management page loses the main sidebar navigation — page renders without the standard layout wrapper. User cannot navigate back to other pages without using browser back button. | Fixed |
| 11 | Medium | Technologist | Technologist KPI page (D1) — incomplete Armenian translation. Title shows Armenian but subtitle is English ("Key performance indicators and business insights"). All section headers (Financial Overview, Customer Insights, Operations) and KPI labels (REVENUE (MONTH), GROSS MARGIN, AVG ORDER VALUE, etc.) remain in English. | Fixed |
| 12 | High | AI / Navigation | All AI pages are missing from the app (no nav entries or routes) compared to the Figma design — includes AI Suggestions, Replenishment Hints, Demand Forecast, Pricing Suggestions, Anomaly Alerts, WhatsApp Conversations, Quality Predictions. Backend endpoints exist but no frontend pages. | Fixed — ENTERPRISE tier assigned to demo tenant |
| 13 | High | Navigation / Platform | Client-facing admin and corporate (vendor) admin panels are mixed into one sidebar section. Platform items (Notification Templates, Subscriptions, Exchange Rates, Mobile Admin, Tenant Management) should be separated from tenant-level admin. Per Figma, these should be distinct sections or a separate super-admin panel. | Fixed — Platform items feature-gated by SUBSCRIPTIONS |

---

## Round 2 — API Endpoint Audit (2026-03-14)

All 27 frontend pages audited; every `apiFetch` URL tested against the running backend. Five new defects found and fixed.

| # | Severity | Page/Area | Description | Status |
|---|----------|-----------|-------------|--------|
| 14 | Critical | AI WhatsApp | Frontend called `/v3/ai/whatsapp/conversations` but backend maps `/v3/ai/conversations` (no `/whatsapp/` segment). All 5 URLs in `ai-whatsapp/page.tsx` had the extra path segment. HTTP 500 `NoResourceFoundException`. | Fixed — removed `/whatsapp/` from 5 URLs |
| 15 | Critical | Customers (Orders tab) | Orders tab called `/v2/orders?tenantId=...` which is a customer-scoped endpoint requiring `customerId`. Admin overview should use `/v1/orders` instead. HTTP 500 `MissingServletRequestParameterException`. | Fixed — changed to `/v1/orders` |
| 16 | Critical | Report Builder | Frontend used `/v2/reports/custom/...` but backend maps `/v2/reports/...` (no `/custom` sub-path). String `custom` was matched as `{id}` param → "Report not found: custom". All 6 URLs fixed. Backend also lacked a `PUT /{id}` endpoint for updates. | Fixed — removed `/custom` from 6 URLs; added `PUT /v2/reports/{id}` controller method |
| 17 | High | Mobile Admin | `GET /v3/mobile/devices` required `customerId` as a mandatory `@RequestParam`, but the admin page sends it optionally (only when filter is set). HTTP 500 `MissingServletRequestParameterException`. | Fixed — made `customerId` optional; added `findByTenantId` repo method; service returns all tenant devices when `customerId` is null |
| 18 | High | Tenant Management (Platform) | `TenantManagementService.getPlatformOverview()` called `subscriptionRepo.findByTenantId(tid)` which returns `Optional<T>`. DevDataSeeder creates a new subscription each restart (deactivating the old one), leaving 2+ rows → `IncorrectResultSizeDataAccessException` / `NonUniqueResultException`. | Fixed — changed to `findByTenantIdAndActive(tid, true)` to fetch only the active subscription |
