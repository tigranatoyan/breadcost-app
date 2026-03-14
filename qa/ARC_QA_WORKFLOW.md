# Arc-Driven QA Workflow

> **Purpose:** Systematic, automated validation of all 8 business arcs defined in `architecture/ARCMAP.md`.  
> Run this workflow at every QA stage (pre-release, post-sprint, regression).

---

## Prerequisites

1. **Seed data script** — deterministic test data (orders, products, recipes, users with roles)
2. **ARCMAP.md** — arc definitions with step-level happy paths (`architecture/ARCMAP.md`)
3. **Route list** — derived from `frontend/app/` folder structure
4. **Armenian locale file** — `frontend/locales/hy.ts`
5. **Backend running** on port 8080, frontend on port 3000, PostgreSQL with test data loaded

---

## Phase 1: Arc Happy-Path E2E Tests

**Goal:** Verify every arc's happy path works end-to-end.

### Prompt to Agent

> Read `architecture/ARCMAP.md`. Each arc has a happy path with steps (Actor → Action → System Response → Next). For each arc, populate the Playwright E2E test skeleton in `frontend/e2e/tests/arc-{N}-{name}.spec.ts` — replace `test.skip` with real assertions that walk the full happy path end-to-end. Each step in the arc becomes a test assertion. If a step says "System creates delivery run," the test asserts a delivery run exists. If a step says "Status changes to READY," the test asserts the badge shows READY. Group assertions by page. Use the seed data already in the database.

### What It Catches

- Missing functionality (features the arc expects but don't exist)
- State sync issues (action on page A doesn't propagate to page B)
- Backend integration bugs (API returns wrong status, missing event triggers)
- Idempotency problems (duplicate creation on repeat actions)
- Cross-arc handoff failures (Arc 1→2 order confirmation doesn't trigger production plan)

### Expected Output

8 test files (skeletons already exist — fill in assertions):
- `frontend/e2e/tests/arc-1-order-lifecycle.spec.ts`
- `frontend/e2e/tests/arc-2-production-planning.spec.ts`
- `frontend/e2e/tests/arc-3-inventory-supply-chain.spec.ts`
- `frontend/e2e/tests/arc-4-customer-portal.spec.ts`
- `frontend/e2e/tests/arc-5-pos.spec.ts`
- `frontend/e2e/tests/arc-6-financial-operations.spec.ts`
- `frontend/e2e/tests/arc-7-ai-assistance.spec.ts`
- `frontend/e2e/tests/arc-8-platform-admin.spec.ts`

---

## Phase 2: i18n / Localization Sweep

**Goal:** Catch every untranslated string, raw i18n key, and raw enum value across all pages.

### Prompt to Agent

> The i18n sweep test skeleton is at `frontend/e2e/tests/i18n-sweep.spec.ts`. It already visits every route and checks for raw i18n keys and raw enum values. Enhance it to also compare all visible `textContent` against `frontend/locales/hy.ts` and flag any English string missing an Armenian translation. Output a structured report: `{page, element, rawText, issue}`.

### What It Catches

- English strings missing Armenian translations
- Raw i18n keys displayed to users (e.g., `driver.refresh`)
- Raw enum values displayed instead of localized labels (e.g., `DRAFT`, `IN_PRODUCTION`)
- Partially translated pages

### Expected Output

- `frontend/e2e/tests/i18n-sweep.spec.ts` (skeleton exists)
- On failure: structured JSON report with page, element selector, raw text, and issue type

---

## Phase 3: Cross-Arc Event Chain Tests

**Goal:** Verify that actions in one arc trigger expected side effects in downstream arcs.

### Prompt to Agent

> Read the "Cross-Arc Dependencies" section in `architecture/ARCMAP.md`. Write integration tests (backend JUnit or Playwright E2E) that verify each cross-arc handoff:
> - Arc 1→2: Confirming an order triggers production plan creation
> - Arc 2→1: Completing all work orders changes linked order status to READY
> - Arc 1→6: Delivering an order triggers invoice creation
> - Arc 3→2: Stock below threshold triggers PO suggestion
>
> Each test creates the trigger condition and asserts the downstream effect exists. If the downstream system has no record, the test fails with a descriptive message like: "Arc 2→1 broken: All WOs complete but order still IN_PRODUCTION (expected READY)."

### What It Catches

- Disconnected pages (e.g., `/deliveries` not linked to order flow)
- Missing event propagation (production completion not updating order status)
- Design/architecture violations (wrong actor driving a state change)
- Missing automation (manual steps where the system should auto-transition)

### Expected Output

- `frontend/e2e/tests/cross-arc-chains.spec.ts` (skeleton exists) and/or `src/test/java/.../CrossArcIntegrationTest.java` (backend)

---

## Phase 4: Observation Report Generation

**Goal:** Convert all test failures into a structured, prioritized observation report.

### Prompt to Agent

> Run all arc tests (`cd frontend/e2e && npx playwright test tests/arc-*.spec.ts tests/i18n-sweep.spec.ts tests/cross-arc-chains.spec.ts`). For each failed assertion, generate a structured observation with:
> - **Arc:** which arc number
> - **Page:** which URL
> - **Expected:** what ARCMAP says should happen
> - **Actual:** what the test found
> - **Category:** one of `BUG`, `I18N`, `DESIGN_ISSUE`, `MISSING_FEATURE`, `UX`, `DATA_BUG`
> - **Priority:** P0 if it blocks the arc happy path, P1 if partial, P2 if cosmetic
>
> Output as a structured table organized by arc number. Create Jira tickets for any P0/P1 issues not already tracked.

### Expected Output

- Console summary with pass/fail counts per arc
- Structured observation table (replaces the old manual `ARC_OBSERVATIONS.md`)
- New Jira tickets for untracked issues

---

## Execution Checklist

| Step | Command / Action | Pass Criteria |
|------|-----------------|---------------|
| 1 | Start backend: `./gradlew bootRun` | Port 8080 responding |
| 2 | Start frontend: `cd frontend && npm run dev` | Port 3000 responding |
| 3 | Load seed data (if needed) | Known test users, products, orders exist |
| 4 | Run Phase 1: `cd frontend/e2e && npx playwright test tests/arc-*.spec.ts` | All arc happy paths pass |
| 5 | Run Phase 2: `cd frontend/e2e && npx playwright test tests/i18n-sweep.spec.ts` | Zero untranslated strings |
| 6 | Run Phase 3: `cd frontend/e2e && npx playwright test tests/cross-arc-chains.spec.ts` | All cross-arc handoffs work |
| 7 | Run Phase 4: Generate report from failures | All P0 issues have Jira tickets |

---

## Key Principle

> **ARCMAP.md is the test specification.** Every step in an arc's happy path (`Actor → Action → System Response`) is a test assertion. This workflow converts those assertions into executable, repeatable, regression-proof tests — replacing manual page-by-page observation.
