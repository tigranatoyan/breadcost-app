# BreadCost — Next Steps & Status

**Last Updated**: March 9, 2026  
**Status**: R1–R3 backend + frontend complete, Phase 3a infrastructure done, security hardened

---

## 1. What's Done

### Releases Completed
| Release | Scope | Stories | Status |
|---------|-------|---------|--------|
| R1 | Core domain — CRUD, orders, inventory, POS, recipes, departments | 36 | ✅ Done |
| R1.5 | Frontend — 14 pages (Next.js 14, React 18, TypeScript) | 14 | ✅ Done |
| R2 | Extended domain — production plans, reports, admin, batch cost, finance | 34 | ✅ Done |
| R2-FE | Frontend — 7 additional pages | 7 | ✅ Done |
| R3 | Advanced features — subscriptions, tenant config, KPI dashboards, work orders | ~40 | ✅ Done |
| R3-FE | Frontend — 6 new pages + 1 modified | 7 | ✅ Done |

**Total**: 142 stories implemented, 396 tests, 0 failures, 67+ REST endpoints

### Infrastructure (Phase 3a) ✅
- PostgreSQL 16 as primary database
- Flyway V1 migrations
- TenantContext (thread-local tenant from JWT)
- TenantFilter sets tenant per request
- Docker-ready configuration

### Security Hardening ✅
- JWT authentication via `JwtAuthFilter`
- `@PreAuthorize` RBAC on all endpoints
- 8 roles: Admin, Manager, ProductionUser, FinanceUser, Viewer, Cashier, Warehouse, Technologist
- `getPrincipalName()` returns real authenticated username (not hardcoded)
- Manager role: can read inventory, orders, plans; can approve/publish production plans
- Plan approval restricted to Admin/Manager only (floor workers excluded)

### Jira ✅
- All 142 stories → Done
- All 15 sprints → Closed
- All 4 versions (R1.5, R2, R3, R3_FE) → Released

---

## 2. Remaining Work

### Phase 3b — Async Processing + Caching (Not Started)
- **RabbitMQ event bus**: Replace sync event notifications with RabbitMQ publish; 3 priority queues
- **Redis caching**: Cache inventory views and batch cost reads (TTL 5 min, invalidate on write)

### Phase 3c — Business Logic + Infrastructure (Not Started)
- **Keycloak**: Replace in-memory `UserDetailsService` with Keycloak JWT realm isolation
- **OpenAPI / Swagger docs**: Auto-generate from controllers
- **WhatsApp Gateway**: Separate microservice for order intake via WhatsApp Business API + GPT-4 NLP
- **Complete batch lifecycle**: `BackflushConsumption`, `RecognizeProduction`, `FGValueAdjustment`
- **Native mobile apps**: Android/iOS POS and floor worker interfaces

### Documentation
- Generate OpenAPI spec from existing `@RestController` annotations
- API usage examples for frontend developers

---

## 3. Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.4.2, Gradle 8.11 |
| Database | PostgreSQL 16 (prod), H2 (test) |
| Migrations | Flyway |
| Frontend | Next.js 14.2, React 18, TypeScript 5, Tailwind CSS |
| Auth | JWT + Spring Security (Keycloak planned) |
| Testing | JUnit 5, Spring MockMvc, 396 tests |
| CI | Gradle build, `./gradlew test` |

---

## 4. Run the Application

```powershell
# Backend (port 8080)
cd d:\Projects\breadcost-app
.\gradlew.bat bootRun

# Frontend (port 3000)
cd frontend
npm run dev
```
> I want to start Phase 3a: add multi-tenancy with `tenant_id`, switch from H2 to PostgreSQL with Row-Level Security, persist the EventStore to a database table, and add a Docker Compose file for local development. Please answer the open decisions in `NEXT_STEPS.md` section 2 first, then we'll begin.

---

## 8. File Map for the New Chat

```
C:\workspace\breadcost-app\
├── ARCHITECTURE_PLAN.md      ← Full Phase 1 + 2 design
├── NEXT_STEPS.md             ← This file — Phase 3 plan
├── SUMMARY.md                ← All 38 source files explained
├── README.md                 ← Project overview + API examples
├── QUICKREF.md               ← Quick reference
├── pom.xml                   ← Maven dependencies
└── src/main/java/com/breadcost/
    ├── BreadCostApplication.java
    ├── api/                  ← 4 REST controllers
    ├── commands/             ← 5 DTOs + 3 handlers + CommandResult
    ├── domain/               ← 11 entities
    ├── events/               ← 9 event classes
    ├── eventstore/           ← EventStore, StoredEvent, IdempotencyService
    ├── projections/          ← ProjectionEngine + 2 view entities
    ├── security/             ← SecurityConfig
    ├── validation/           ← ValidationService
    └── finance/              ← FinanceService
```
