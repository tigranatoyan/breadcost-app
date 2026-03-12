# BreadCost Frontend — Frozen Specification
**Last Updated:** 2026-03-04 (Technology Steps + Floor Worker session)
**Purpose:** Authoritative reference for the frontend. Do NOT change behaviour described here without explicit instruction.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | Next.js 14.2.18 (App Router) |
| UI Library | React 18, TypeScript |
| Styling | Tailwind CSS 3 |
| Dev Server | `npm run dev` → port 3000 |
| Root | `c:\workspace\breadcost-app\frontend\` |

---

## 1. Authentication & Shell (`frontend/components/AuthShell.tsx`)

### Login Flow
- `AuthContext` holds `{ user, token, login, logout }`
- On first load: `loading=true`. While loading show a spinner (`<div className="flex items-center justify-center min-h-screen"><div className="animate-spin ..."/></div>`) — **never return null** (causes login flash)
- After load: if no user → redirect to `/login`
- `frontend/app/login/page.tsx` — standalone page, not wrapped in AuthShell

### User Roles
- `admin` — full access
- `management` / `technologist` — operations + analysis
- `production` (floor worker) — floor screen only
- Role-based nav is controlled in `AuthShell.tsx` `navSections` array

### Navigation Structure (FROZEN)
```
All non-floor users:
  [Overview]
    Dashboard → /dashboard

Floor workers only (shown FIRST):
  [My Shift]
    My Shift Plan → /floor

Admin + Management (Operations):
  [Operations]
    Orders → /orders
    Production Plans → /production-plans   (mgmt also sees this)

Floor workers (limited Operations):
  [Operations]
    Production Plans → /production-plans

Admin only (Configuration):
  [Configuration]
    Admin Panel → /admin
    Departments → /departments
    Products → /products
    Recipes & Steps → /recipes

Admin only (Analysis):
  [Analysis]
    Technologist View → /technologist
    Floor View → /floor
```

### Floor Worker Auto-Redirect
```typescript
// In AuthShell useEffect:
if (user && hasRole('production') && !hasRole('admin') && !hasRole('management') 
    && pathname === '/dashboard') {
  router.replace('/floor');
}
```

---

## 2. Recipes Page (`frontend/app/recipes/page.tsx`)

### Features (FROZEN)
1. List all recipes for tenant, grouped by product
2. Each recipe row is expandable (click) — shows two tabs:
   - **Ingredients** — table of ingredients (item, quantity, unit, waste%) + productionNotes panel
   - **Technology Steps** — ordered list of process steps
3. Recipe header shows: `{name} v{version} · {status} · {ingr count} ingr · {step count} steps`

### Technology Step Display (per step)
- Number circle (step number in blue circle)
- Step name (bold)
- Activities text (if any)
- Instruments badge + duration badge + temperature badge (if set)
- Edit button + delete (×) button (visible to admin role)

### Technology Step Modal (create / edit)
Fields:
- Step Number (number, required)
- Step Name (text, required)
- Activities (textarea)
- Instruments (text)
- Duration (minutes, number)
- Temperature (°C, number)

Submit calls:
- Create: `POST /v1/technology-steps?tenantId=` with body `{recipeId, stepNumber, name, activities, instruments, durationMinutes, temperatureCelsius}`
- Edit: `PUT /v1/technology-steps/{stepId}?tenantId=`

### Caching
Steps are cached in `stepsRef.current[recipeId]` to avoid re-fetching on tab switch.
Use `loadSteps(recipeId, force=true)` to force refresh after save/delete.

---

## 3. Floor Page (`frontend/app/floor/page.tsx`)

### Purpose
Active screen for production floor workers. Shows production plan for the current shift with work orders as cards. Clicking a work order opens a side panel with step-by-step recipe guidance.

### Page Layout
- Header: tenant + shift info (date, shift badge colour, status, batchCount)
- Body: grid of `WorkOrderCard` components (one per WO in the plan)
- Side panel (`WOPanel`) slides in from right when a WO is clicked

### WorkOrderCard
- Product name (bold)
- Status badge (`shiftBg` colour map) + status icon (`woStatusIcon`)
- Target qty + unit
- Click → opens WOPanel

### WOPanel (side panel overlay)
- Position: fixed right-0, top-0, h-full, w-96 (dark bg overlay behind)
- Header: product name + close (×) button
- Two tabs: **Technology Steps** | **Recipe**
- Footer: action buttons depending on WO status:
  - `PLANNED` → [Start Production]
  - `IN_PROGRESS` → [Complete] + [Cancel]
  - Others → no action buttons
- `onAction` calls backend PUT and refreshes plan

### Technology Steps Tab
- Ordered list of steps from `/v1/technology-steps?tenantId=&recipeId=`
- Each step: checkbox + number circle + name + activities text + meta badges
- Checkbox state persisted to `localStorage` at key `step_{workOrderId}_{stepNumber}`
- Checking/unchecking: `localStorage.setItem(key, checked ? '1' : '0')`
- On panel open: checkboxes reflect stored values

### Recipe Tab
- List of ingredients from the recipe
- Shows: ingredient name, `{perBatch × batchCount} {unit}` required
- batchCount comes from `plan.batchCount` passed from FloorPage

### Caching (avoid re-renders)
```typescript
const recipeCache = useRef<Record<string, RecipeDetail>>({});
const stepsCache  = useRef<Record<string, TechnologyStep[]>>({});

// On WO click:
async function openWo(wo: WorkOrder) {
  // Load recipe if not cached
  if (!wo.recipeId || !recipeCache.current[wo.productId]) { ... fetch ... }
  // Load tech steps if not cached
  if (!wo.recipeId || !stepsCache.current[wo.recipeId]) { ... fetch ... }
  setSelectedWo(wo);
  setRecipe(recipeCache.current[wo.productId] ?? null);
  setTechSteps(stepsCache.current[wo.recipeId ?? ''] ?? []);
}
```

### Helper Constants
```typescript
const shiftBg: Record<string, string> = {
  PLANNED:     'bg-blue-100  text-blue-800',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
  COMPLETED:   'bg-green-100  text-green-800',
  CANCELLED:   'bg-red-100    text-red-800',
};

const woStatusIcon: Record<string, string> = {
  PLANNED: '🕐', IN_PROGRESS: '⚙️', COMPLETED: '✅', CANCELLED: '❌',
};
```

---

## 4. Backend API Reference (Technology Steps)

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/v1/technology-steps?tenantId=&recipeId=` | All | List steps ordered by stepNumber |
| POST | `/v1/technology-steps?tenantId=` | Admin/Production | Create step |
| PUT | `/v1/technology-steps/{stepId}?tenantId=` | Admin/Production | Update step |
| DELETE | `/v1/technology-steps/{stepId}?tenantId=` | Admin/Production | Delete step |

### TechnologyStep JSON
```json
{
  "stepId": "uuid",
  "tenantId": "tenant1",
  "recipeId": "uuid",
  "stepNumber": 1,
  "name": "Mixing",
  "activities": "Combine flour and water until smooth",
  "instruments": "Planetary mixer",
  "durationMinutes": 15,
  "temperatureCelsius": null
}
```

### Java Classes
- `com.breadcost.masterdata.TechnologyStepEntity` — JPA entity
- `com.breadcost.masterdata.TechnologyStepRepository` — Spring Data JPA
- `com.breadcost.masterdata.TechnologyStepService` — business logic
- `com.breadcost.api.TechnologyStepController` — REST endpoints

---

## 5. Generate Work Orders Fix

**File:** `com.breadcost.masterdata.ProductionPlanService.generateWorkOrders()`

**Change:** If no confirmed sales orders match the plan's date, fall back to ALL confirmed orders. This ensures the button always produces results when confirmed orders exist.

```java
if (relevantOrders.isEmpty()) {
    relevantOrders = confirmedOrders; // fallback: use all confirmed orders
}
```

---

## 6. Known User Accounts

| Username | Password | Role | Default Screen |
|---|---|---|---|
| admin | admin | admin | /dashboard |
| production | production | production (floor) | /floor (auto-redirect) |

> Actual credentials stored in Spring Security config. The table above reflects the intended demo accounts.

---

## 7. File Map

| Frontend File | Role |
|---|---|
| `frontend/app/login/page.tsx` | Login form |
| `frontend/app/dashboard/page.tsx` | Overview dashboard |
| `frontend/app/floor/page.tsx` | Floor worker shift view |
| `frontend/app/recipes/page.tsx` | Recipe list + technology steps management |
| `frontend/app/production-plans/page.tsx` | Production plan management + generate WOs |
| `frontend/app/orders/page.tsx` | Sales orders |
| `frontend/app/products/page.tsx` | Products |
| `frontend/app/departments/page.tsx` | Departments |
| `frontend/components/AuthShell.tsx` | Auth context + nav shell |
| `frontend/lib/api.ts` | Axios wrapper (adds bearer token) |
| `frontend/lib/auth.ts` | AuthContext provider |

---

## 8. Rules — Do NOT Change Without Explicit Instruction

1. Floor workers always land on `/floor`, not `/dashboard`
2. Technology steps tab is inside the recipe expand panel (not a separate page)
3. Step confirmations use `localStorage`, not server state
4. Recipe/steps are cached in `useRef` on the floor page (avoid re-fetch)
5. `AuthShell` loading state shows spinner, never returns `null`
6. WOPanel is a fixed side panel (not a modal dialog)
7. Generate Work Orders falls back to all confirmed orders if none match by date
