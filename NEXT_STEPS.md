# BreadCost — Phase 3: Next Steps & Handoff Document

**Status**: Ready to start Phase 3 implementation  
**Date**: March 3, 2026  
**Preceding work**: Phases 1 & 2 complete — see [ARCHITECTURE_PLAN.md](../ARCHITECTURE_PLAN.md) for full design

---

## 1. Where We Are Right Now

### Working Application
The Spring Boot app runs at `http://localhost:8080` with:
- Full event-sourcing infrastructure (in-memory `EventStore`)
- CQRS with `ProjectionEngine` (watermark-based period close)
- 3 working commands: `ReceiveLot`, `IssueToBatch`, `TransferInventory`
- RBAC with Spring Security (roles: admin, production, finance, viewer)
- REST API at `/v1/inventory/*`, `/v1/batches/*`, `/v1/views/*`
- H2 in-memory database (data lost on restart — intentional for now)

### Start the app
```powershell
cd C:\workspace\breadcost-app
mvn spring-boot:run
```

### Key source files
| File | Purpose |
|------|---------|
| `src/main/java/com/breadcost/eventstore/EventStore.java` | In-memory event store — replace with PostgreSQL |
| `src/main/java/com/breadcost/projections/ProjectionEngine.java` | Reads events → builds views |
| `src/main/java/com/breadcost/commands/` | All command DTOs + handlers |
| `src/main/java/com/breadcost/security/SecurityConfig.java` | RBAC — replace with Keycloak/JWT |
| `src/main/resources/application.properties` | Config — add tenant + DB settings |

---

## 2. Open Decisions (Answer These First in the New Chat)

The following were left unanswered and affect what gets built next:

1. **Scope of Phase 3 start?**
   - Option A: MVP only — add multi-tenancy to existing monolith, keep H2 for now
   - Option B: Full switch — PostgreSQL + RabbitMQ + Redis from day one
   - *Recommendation: Option A to ship faster, migrate to B in Phase 3b*

2. **Database schema first or code-first?**
   - Option A: Design PostgreSQL schema + RLS policies first, then code
   - Option B: Code JPA entities with `tenant_id`, let Hibernate generate schema
   - *Recommendation: Option B for speed, review schema output before production*

3. **Dockerize from the start?**
   - Yes: `docker-compose.yml` with Postgres + Redis + RabbitMQ + App
   - No: Run services locally, dockerize later
   - *Recommendation: Yes — having compose from day 1 avoids "works on my machine"*

4. **What to build first?**
   - Option A: Multi-tenancy + persistent storage (foundation)
   - Option B: WhatsApp integration (most visible business value)
   - Option C: Complete batch lifecycle (most missing business logic)
   - *Recommendation: Option A first — everything else depends on it*

---

## 3. Phase 3 Implementation Plan

### Phase 3a — Foundation (Multi-Tenancy + Persistence)

**Step 1: Add `tenant_id` to domain**
- Add `String tenantId` to all domain entities (`Batch`, `Lot`, `InventoryPosition`, `LedgerEntry`, etc.)
- Add `TenantContext` thread-local holder (set per request)
- Add `TenantInterceptor` on all API requests (reads tenant from JWT or header)

**Step 2: Replace H2 with PostgreSQL + Row-Level Security**
- Switch `application.properties` to PostgreSQL datasource
- Enable RLS on all tables:
  ```sql
  ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
  CREATE POLICY tenant_isolation ON batches
      USING (tenant_id = current_setting('app.current_tenant')::UUID);
  ```
- Add `TenantAwareJpaRepository` base class that sets `app.current_tenant` before queries

**Step 3: Persist the EventStore**
- Replace in-memory `EventStore` with a `stored_events` PostgreSQL table
- Keep the same `EventStore` interface — swap implementation only
- Schema:
  ```sql
  CREATE TABLE stored_events (
      id BIGSERIAL PRIMARY KEY,
      tenant_id UUID NOT NULL,
      aggregate_id VARCHAR(255),
      event_type VARCHAR(255),
      payload JSONB,
      occurred_at TIMESTAMPTZ,
      ledger_seq BIGINT
  );
  ```

**Step 4: Add Tier-Based Feature Flags**
- Create `Tenant` entity with `tier` field (STARTER / PROFESSIONAL / ENTERPRISE)
- Create `TierConfiguration` service:
  ```java
  boolean isFeatureEnabled(String tenantId, Feature feature);
  ```
- Annotate feature endpoints with `@RequiresTier(PROFESSIONAL)`

**Step 5: Docker Compose**
```yaml
services:
  postgres:    # PostgreSQL 16 with RLS
  redis:       # For caching views
  rabbitmq:    # For async processing (Phase 3b)
  app:         # Spring Boot app
```

---

### Phase 3b — Async Processing + Caching

**Step 6: Add RabbitMQ event bus**
- Replace sync `EventStore.append()` notifications with RabbitMQ publish
- 3 priority queues: `enterprise.high`, `professional.normal`, `starter.low`
- `ProjectionEngine` becomes a RabbitMQ consumer (async)

**Step 7: Add Redis caching layer**
- Cache `InventoryValuationView` and `BatchCostView` reads
- TTL: 5 minutes, invalidate on write events
- Expected 95% cache hit rate

---

### Phase 3c — Business Logic Completion

**Step 8: Complete batch lifecycle**
Missing commands (see `ARCHITECTURE_PLAN.md`):
- `CreateBatch`
- `ReleaseBatch` 
- `BackflushConsumption` logic
- `RecognizeProduction` trigger
- `FGValueAdjustment` calculations

**Step 9: Replace Spring Basic Auth with Keycloak**
- Self-hosted Keycloak in Docker
- JWT bearer tokens
- Map Keycloak roles to existing Spring Security roles
- Multi-tenant realm isolation

**Step 10: WhatsApp Gateway Service**
- Separate Spring Boot microservice (`whatsapp-gateway/`)
- Connects to WhatsApp Business API
- Sends message text to GPT-4 for NLP parsing
- Emits order commands via RabbitMQ
- Returns confirmation messages to customer

---

## 4. Architecture Decisions Already Made

| Decision | Choice | Reason |
|----------|--------|--------|
| Event bus | RabbitMQ | Simpler + cheaper than Kafka |
| NLP | OpenAI GPT-4 | Pay-per-use, no infra |
| Cloud | AWS | Best tooling |
| Containers | Docker Compose → Kubernetes | Start simple |
| Auth | Keycloak (self-hosted) | Free + enterprise-grade |
| Database | PostgreSQL + RLS | Shared DB with tenant isolation |
| Cache | Redis | Industry standard |
| Monitoring | Prometheus + Grafana | Self-hosted, free |
| API Gateway | Kong CE | Free tier available |

---

## 5. Pricing Tiers (Already Decided)

| Tier | Price | DB | Batches/month | Backup |
|------|-------|----|---------------|--------|
| Starter | $99/month | Shared | 1,000 | Daily |
| Professional | $299/month | Shared (priority) | 10,000 | Hourly |
| Enterprise | $999/month | Dedicated | Unlimited | Real-time |

**Target economics (200 tenants):**
- Revenue: ~$36,800/month
- Infrastructure: ~$3,150/month
- Gross margin: **91%**

---

## 6. Not Yet Implemented (from SUMMARY.md)

| Feature | Status | Phase |
|---------|--------|-------|
| Persistent event store | ❌ | 3a |
| Multi-tenancy (tenant_id + RLS) | ❌ | 3a |
| Feature tier enforcement | ❌ | 3a |
| Docker Compose | ❌ | 3a |
| Redis caching | ❌ | 3b |
| RabbitMQ async processing | ❌ | 3b |
| Complete batch commands | ❌ | 3c |
| BackflushConsumption logic | ❌ | 3c |
| RecognizeProduction trigger | ❌ | 3c |
| FG adjustment calculations | ❌ | 3c |
| PPA (Period Price Adjustment) | ❌ | 3c |
| Keycloak / JWT auth | ❌ | 3c |
| WhatsApp Gateway service | ❌ | 3c |
| OpenAPI / Swagger docs | ❌ | 3c |
| Golden test fixtures | ❌ | 3c |

---

## 7. Prompt to Start the New Chat

Paste this as your **first message** in the new chat:

> I'm continuing work on the BreadCost manufacturing cost SaaS app. The codebase is at `C:\workspace\breadcost-app`. Phases 1 (requirements) and 2 (architecture) are complete and documented in `ARCHITECTURE_PLAN.md`. The app currently runs as a Spring Boot monolith with in-memory event sourcing, H2 database, and basic Spring Security. 
>
> Please read these files first before we start:
> - `ARCHITECTURE_PLAN.md` — full architecture decisions and technology choices
> - `NEXT_STEPS.md` — Phase 3 plan and open decisions
> - `SUMMARY.md` — inventory of all 38 existing source files
>
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
