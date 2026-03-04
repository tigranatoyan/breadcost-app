# BreadCost Manufacturing Cost Accounting System

Event-sourced CQRS application for manufacturing cost accounting with complete audit trail, idempotency, and financial controls.

## Overview

BreadCost implements a complete event-sourced manufacturing cost accounting system based on comprehensive domain specifications. It tracks inventory movements, production batches, cost allocations, and generates financial reports with strict consistency guarantees.

### Key Features

- **Event Sourcing**: All state changes recorded as immutable events with strict ordering (ledgerSeq)
- **CQRS**: Separate command and query paths with eventual consistency
- **Idempotency**: All commands are idempotent via idempotency keys
- **Financial Controls**: Dual-entry accounting with FINANCIAL/OPERATIONAL classification
- **Period Close**: Sophisticated period close process waiting on financial watermark only
- **Audit Trail**: Complete audit trail via event store and ledger entries
- **RBAC**: Role-based access control per governance specifications

## Architecture

```
┌─────────────┐
│   REST API  │  Commands: POST /v1/inventory/receipts, /v1/batches/{id}/issues, etc.
└──────┬──────┘  Queries:  GET /v1/views/batch-cost/{id}, /v1/views/inventory-valuation
       │
       v
┌──────────────┐
│   Command    │  ReceiveLot, IssueToBatch, TransferInventory, CloseBatch, etc.
│   Handlers   │  Validate, check idempotency, emit events
└──────┬───────┘
       │
       v
┌──────────────┐
│  Event Store │  In-memory store with ledgerSeq ordering
└──────┬───────┘  Emits events to listeners
       │
       v
┌──────────────┐
│  Projections │  BatchCostView, InventoryValuationView, WIPView, etc.
└──────────────┘  Tracks financial/operational watermarks
```

## Domain Model

### Core Entities

- **Item**: Materials, packaging, finished goods (FG), byproducts, WIP, sentinels
- **Lot**: Inventory lots tracked by (siteId, lotId) per LOT_REFERENCE_LAW
- **Batch**: Production batches accumulating costs
- **LedgerEntry**: Immutable financial/operational entries ordered by ledgerSeq
- **Period**: Accounting periods (OPEN → CLOSING → LOCKED)
- **RecognitionOutputSet**: Immutable snapshot of batch outputs at recognition

### Key Events

- **ReceiveLot**: Record inventory receipt with cost (FINANCIAL)
- **IssueToBatch**: Issue materials to production batch (FINANCIAL)
- **TransferInventory**: Move inventory between locations (OPERATIONAL, amountBase=0)
- **BackflushConsumption**: Auto-consume per recipe (FINANCIAL)
- **CloseBatch**: Close batch, trigger RecognizeProduction
- **RecognizeProduction**: Recognize finished goods, relieve WIP (FINANCIAL)
- **FGValueAdjustment**: Adjust FG value for late entries (FINANCIAL)
- **LateEntryNotEligibleForFGAdj**: Marker for ineligible late entries (OPERATIONAL)

## Tech Stack

- **Java 21**
- **Spring Boot 3.4.2**
- **Maven**
- **Spring Data JPA** (for projections/read models)
- **H2 Database** (in-memory for read models)
- **Spring Security** (RBAC implementation)
- **Jackson** (JSON/YAML serialization)
- **Lombok** (boilerplate reduction)

## Project Structure

```
breadcost-app/
├── pom.xml                          # Maven dependencies
├── src/main/java/com/breadcost/
│   ├── BreadCostApplication.java   # Spring Boot entry point
│   ├── domain/                      # Domain entities
│   │   ├── Item.java
│   │   ├── Lot.java
│   │   ├── Batch.java
│   │   ├── LedgerEntry.java
│   │   ├── Period.java
│   │   └── ...
│   ├── events/                      # Domain events
│   │   ├── DomainEvent.java
│   │   ├── ReceiveLotEvent.java
│   │   ├── IssueToBatchEvent.java
│   │   ├── RecognizeProductionEvent.java
│   │   └── ...
│   ├── commands/                    # Command DTOs and handlers
│   │   ├── ReceiveLotCommand.java
│   │   ├── ReceiveLotCommandHandler.java
│   │   ├── IssueToBatchCommand.java
│   │   ├── IssueToBatchCommandHandler.java
│   │   └── ...
│   ├── eventstore/                  # Event sourcing infrastructure
│   │   ├── EventStore.java          # In-memory event store
│   │   ├── IdempotencyService.java
│   │   └── StoredEvent.java
│   ├── projections/                 # Read model projections
│   │   ├── ProjectionEngine.java    # Event listener & watermarks
│   │   ├── BatchCostView.java
│   │   └── InventoryValuationView.java
│   ├── api/                         # REST controllers
│   │   ├── InventoryController.java
│   │   ├── BatchController.java
│   │   ├── ViewController.java
│   │   └── GlobalExceptionHandler.java
│   ├── security/                    # RBAC implementation
│   │   └── SecurityConfig.java
│   ├── validation/                  # Governance rules
│   │   └── ValidationService.java
│   └── finance/                     # Financial rules
│       └── FinanceService.java
└── src/main/resources/
    └── application.properties       # Configuration
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build and Run

1. **Build the project:**
   ```powershell
   cd C:\workspace\hello-genai\work\breadcost-app
   mvn clean package
   ```

2. **Run the application:**
   ```powershell
   mvn spring-boot:run
   ```

3. **Access the application:**
   - API: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
     - JDBC URL: `jdbc:h2:mem:breadcost`
     - Username: `sa`
     - Password: (empty)

### Demo Users

The application includes demo users per RBAC_MATRIX roles:

- **admin/admin** - Full access (Admin role)
- **production/production** - Production operations (ProductionUser role)
- **finance/finance** - Finance operations (FinanceUser role)
- **viewer/viewer** - Read-only access (Viewer role)

## API Examples

### Receive Inventory

```bash
curl -X POST http://localhost:8080/v1/inventory/receipts \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "tenantId": "TENANT001",
    "siteId": "SITE001",
    "receiptId": "RCV001",
    "itemId": "FLOUR",
    "lotId": "LOT123",
    "qty": 1000,
    "uom": "KG",
    "unitCostBase": 2.50,
    "occurredAtUtc": "2026-03-03T10:00:00Z",
    "idempotencyKey": "rcv001-key"
  }'
```

### Issue to Batch

```bash
curl -X POST http://localhost:8080/v1/batches/BATCH001/issues \
  -H "Content-Type: application/json" \
  -u production:production \
  -d '{
    "tenantId": "TENANT001",
    "siteId": "SITE001",
    "itemId": "FLOUR",
    "qty": 100,
    "uom": "KG",
    "lotId": "LOT123",
    "occurredAtUtc": "2026-03-03T11:00:00Z",
    "idempotencyKey": "issue001-key"
  }'
```

### Transfer Inventory

```bash
curl -X POST http://localhost:8080/v1/inventory/transfers \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "tenantId": "TENANT001",
    "siteId": "SITE001",
    "itemId": "FLOUR",
    "qty": 50,
    "fromLocationId": "BIN01",
    "toLocationId": "STATION05",
    "occurredAtUtc": "2026-03-03T12:00:00Z",
    "idempotencyKey": "transfer001-key"
  }'
```

### Query Batch Cost

```bash
curl http://localhost:8080/v1/views/batch-cost/BATCH001 \
  -u finance:finance
```

### Query Inventory Valuation

```bash
curl http://localhost:8080/v1/views/inventory-valuation?siteId=SITE001 \
  -u finance:finance
```

## Key Implementation Details

### Event Sourcing

- All events stored in `EventStore` with monotonically increasing `ledgerSeq`
- Events generate `LedgerEntry` records with `entryClass` (FINANCIAL or OPERATIONAL)
- Event store notifies listeners (projections) asynchronously
- Strict ordering maintained via synchronized append operations

### Idempotency

- All commands require `idempotencyKey`
- `IdempotencyService` tracks (tenantId, commandName, idempotencyKey) → resultRef
- Duplicate commands return cached result without re-execution
- Guarantees exactly-once semantics

### Lot References

Per **LOT_REFERENCE_LAW**: All lot references include `siteId` for proper scoping.
- Lot key: `(siteId, lotId)`
- System sentinels like `__NO_LOT__` exist per site

### Financial vs Operational Entries

- **FINANCIAL**: Affects costs, included in financial watermark, delays period close
- **OPERATIONAL**: Markers and transfers (amountBase=0), does NOT delay period close
- Per **CLOSE_BLOCKING_LAW**: ClosePeriod waits on `financialWatermark` only

### Period Close Process

Per **closePeriodLaw** in READ_MODELS.yaml:

1. Period status: OPEN → CLOSING
2. Compute `cutoffSeqFinancial` = MAX(ledgerSeq) WHERE entryClass=FINANCIAL
3. Wait until projections' `financialWatermark >= cutoffSeqFinancial`
4. Finalize financial reports using FINANCIAL entries only
5. Period status: CLOSING → LOCKED

### Markers

**LateEntryNotEligibleForFGAdj** marker:
- OPERATIONAL entry (amountBase=0, qty=null)
- Records late entries that don't trigger FG adjustment
- Excluded from financial close cutoff
- Validates `sourceLedgerSeq` metadata

### Sentinel Items

Per **SENTINEL_ITEM_RULES**:
- System sentinels: `__NO_LOT__`, `__NO_LOC__` (per site)
- Excluded from costing, recipes, purchasing flows
- Validation enforced in `ValidationService`

## Configuration

Key settings in `application.properties`:

```properties
# Server
server.port=8080

# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:breadcost

# Event Store
breadcost.event-store.in-memory=true
breadcost.event-store.initial-ledger-seq=1000

# Projections
breadcost.projection.lag-warning-seconds=10
breadcost.projection.async-enabled=true

# Security
spring.security.user.name=admin
spring.security.user.password=admin
```

## Implemented Features

### P0 Stories from JIRA Backlog

This implementation covers core P0 stories:

- ✅ **BC-ARCH-Domain**: Canonical domain model with entities
- ✅ **BC-PROJ-CloseWatermark**: ClosePeriod waits on financialWatermark only
- ✅ **BC-OPS-Marker**: LateEntryNotEligibleForFGAdj marker implementation
- ✅ **BC-LED**: Ledger and idempotency infrastructure
- ✅ **BC-INV**: Core inventory operations (Receive, Transfer, Issue)
- ✅ **Event sourcing**: Full event store with ledgerSeq ordering

### Partially Implemented

- **Projections**: Infrastructure present, specific view calculations pending
- **Batch lifecycle**: Core commands present, full workflow pending
- **Recognition**: Event classes present, recognition logic pending
- **Financial posting**: Basic rules present, full posting engine pending

## Development

### Running Tests

```powershell
mvn test
```

### Building for Production

```powershell
mvn clean package -DskipTests
java -jar target/breadcost-app-1.0.0-SNAPSHOT.jar
```

### Extending the System

To add new commands:

1. Create command DTO in `commands/` package
2. Create command handler extending pattern
3. Create corresponding event in `events/` package
4. Add REST endpoint in appropriate controller
5. Update RBAC in `SecurityConfig` if needed
6. Add validation rules in `ValidationService`

## Specification References

This implementation is based on specifications from:
- `domain/DOMAIN_MODEL.yaml` - Domain entities
- `events/EVENT_CATALOG.yaml` - Event schemas
- `governance/COMMAND_REGISTRY.yaml` - Command catalog
- `governance/API_SURFACE.yaml` - REST API endpoints
- `governance/RBAC_MATRIX.yaml` - Security roles
- `projections/READ_MODELS.yaml` - Read model definitions
- `finance/POSTING_RULES.yaml` - Financial posting rules
- `backlog/JIRA_BACKLOG.csv` - Feature roadmap

## Troubleshooting

### Application won't start

- Ensure Java 17+ is installed: `java -version`
- Check port 8080 is available
- Review logs for specific errors

### API returns 401 Unauthorized

- Include Basic Auth credentials in requests
- Verify user credentials match demo users

### Events not appearing in projections

- Check `ProjectionEngine` logs for errors
- Verify event store listener registration
- Check watermark values in logs

## Future Enhancements

- Implement complete projection views with calculations
- Add persistent event store (PostgreSQL, EventStoreDB)
- Implement batch recognition workflow
- Add FG adjustment trigger logic
- Implement approval workflow engine
- Add metrics and monitoring (Prometheus, Grafana)
- Add integration tests with golden fixtures
- Implement OpenAPI/Swagger documentation

## License

Copyright (c) 2026 BreadCost Project

---

**Version**: 1.0.0-SNAPSHOT  
**Generated**: March 3, 2026  
**Based on**: breadcost_v16_final_artifact_pack
