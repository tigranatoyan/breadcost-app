# BreadCost - Architecture Planning Session Summary
**Date**: March 3, 2026  
**Status**: Phase 1 & 2 Complete, Ready for Phase 3 Implementation

---

## Phase 1: Requirements ✅ COMPLETE

### Business Requirements
- **Purpose**: Manufacturing cost tracking + prevent leakage + optimize efficiency
- **Users**: Factory workers, accountants, managers, auditors, customers, suppliers
- **Scale**: 200 companies (multi-tenant SaaS), 100K batches/day, 100K inventory transactions/day
- **Critical**: 99.9% uptime, 30-min max downtime, zero data loss on customer orders
- **Data Retention**: 3 years online

### Key Features
1. Batch creation & cost calculation
2. Inventory turnover & warehouse tracking
3. Customer orders (WhatsApp/Web/Mobile)
4. Supplier auto-notification via WhatsApp
5. Real-time cost updates (night batch for heavy analytics)
6. Complete audit trail (every field change)

### User Roles
- Factory Workers (recipes, batch prep)
- Accountants (reports, audit)
- Managers (dashboards, approvals)
- Auditors (read-only, audit trail)
- Customers (order placement/tracking)
- Suppliers (order confirmations)

### Multi-Tenancy
- 200 companies (tenants)
- Custom workflows per tenant
- Custom fields per tenant
- White-label UI
- Separate database per tenant (Enterprise tier only)

---

## Phase 2: Architecture Design ✅ COMPLETE

### Pricing Tiers (Cost-Optimized Model)

**Tier 1: Starter** - $99/month
- Shared database
- Basic workflows
- Standard UI
- Email only
- 1,000 batches/month
- Daily backup

**Tier 2: Professional** - $299/month
- Shared database (priority)
- Custom workflows + 10 custom fields
- White-label UI
- WhatsApp + Email
- 10,000 batches/month
- Hourly backup

**Tier 3: Enterprise** - $999/month
- Dedicated database
- Unlimited custom fields
- Full white-label
- All integrations
- Unlimited batches
- Real-time backup (zero RPO)
- 99.9% uptime SLA

### Economics
- **Revenue** (200 tenants): ~$36,800/month
- **Infrastructure Cost**: ~$3,150/month
- **Gross Margin**: 91%

### Architecture Decisions

**Technology Stack:**
1. **Event Bus**: RabbitMQ (simpler, cheaper than Kafka)
2. **NLP for WhatsApp**: OpenAI GPT-4 (pay-per-use)
3. **Cloud**: AWS
4. **Container Orchestration**: Docker Compose → Kubernetes (as we scale)
5. **OAuth**: Keycloak (self-hosted, free)
6. **Database**: PostgreSQL (shared + RLS for Starter/Pro, dedicated for Enterprise)
7. **Cache**: Redis
8. **Monitoring**: Prometheus + Grafana (self-hosted)

**Architecture Pattern:**
- Monolith-first for cost efficiency
- Event-driven with RabbitMQ
- Multi-tenant shared database (with Row-Level Security)
- Feature flags per tier
- Background job processing (async cost calculations)

### Service Structure

```
API Gateway (Kong CE - Free)
├── Core Service (Monolith)
│   ├── Orders Module
│   ├── Batches Module
│   ├── Inventory Module
│   └── Tenants Module
├── WhatsApp Gateway Service
└── Worker Service
    ├── Cost Calculator
    ├── Report Generator
    ├── Audit Logger
    └── Notification Sender

Data Layer
├── PostgreSQL (Shared) + Read Replica
├── Redis Cache
└── RabbitMQ (3 priority queues)
```

### Key Technical Patterns

**1. Multi-Tenant Database Isolation**
```sql
-- Every table has tenant_id
-- Row-Level Security auto-filters by tenant
ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON batches
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
```

**2. Feature Flags**
```java
@Service
public class TierConfiguration {
    boolean isFeatureEnabled(String tenantId, Feature feature);
}
```

**3. Priority Queues**
- High priority: Enterprise (immediate)
- Normal priority: Pro (<1 min)
- Low priority: Starter (<5 min)

**4. Caching Strategy**
- Cache reads in Redis (5 min TTL)
- Invalidate on writes
- 95% cache hit rate expected

### WhatsApp Integration Flow

```
Customer Message → WhatsApp Business API → WhatsApp Gateway
→ NLP Parser (GPT-4) → Order Service → Event Bus
→ Workflow Engine → Response to Customer
```

Features:
- Natural language parsing ("I need 100kg flour")
- Interactive menus (buttons)
- Order status updates
- Supplier notifications

### Scaling Path

**Phase 1** (0-50 tenants): Monolith + Shared DB ~ $500/month  
**Phase 2** (50-150 tenants): Add microservices + Redis ~ $1,500/month  
**Phase 3** (150-500 tenants): Kubernetes + DB sharding ~ $3,000-5,000/month  
**Phase 4** (500+ tenants): Multi-region deployment ~ $10,000+/month

---

## Phase 3: Implementation (NEXT STEP)

### Ready to Build:
1. Refactor existing BreadCost monolith
2. Add multi-tenancy (tenant_id, RLS)
3. Implement tier-based feature flags
4. Add RabbitMQ for async processing
5. Implement WhatsApp Gateway service
6. Add Redis caching layer
7. Set up PostgreSQL HA with read replica
8. Configure monitoring (Prometheus/Grafana)

### Migration Strategy:
- Keep existing event sourcing architecture
- Add tenant context to all operations
- Migrate in-memory stores to PostgreSQL + Redis
- Deploy services incrementally

---

## Questions for Tomorrow's Session

Before we start implementation, decide:
1. Start with MVP (basic multi-tenancy) or full architecture?
2. Want to see database schema design first?
3. Should we dockerize everything from the start?
4. Priority: Backend refactor or WhatsApp integration first?

---

## Current State

**Working:**
- ✅ Event sourcing infrastructure
- ✅ Inventory operations (ReceiveLot, Transfer, Issue)
- ✅ React frontend with auth
- ✅ InventoryProjection (real-time view)

**Needs Refactoring:**
- ❌ Multi-tenancy support
- ❌ Feature tier enforcement
- ❌ Persistent event store (Kafka/RabbitMQ)
- ❌ Batch management workflow
- ❌ WhatsApp integration
- ❌ Cost calculation engine
- ❌ Production-ready auth (OAuth2)

---

**Next Session**: Start Phase 3 implementation with your prioritization input!
