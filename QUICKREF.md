# BreadCost Quick Reference

## Project Info
- **Directory**: C:\workspace\hello-genai\work\breadcost-app
- **Total Files**: 50
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.2
- **Build Tool**: Maven

## Build & Run Commands

```powershell
# Navigate to project
cd C:\workspace\hello-genai\work\breadcost-app

# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package

# Run application
mvn spring-boot:run

# Or run JAR
java -jar target/breadcost-app-1.0.0-SNAPSHOT.jar
```

## Access URLs

- **API Base**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:breadcost`
  - Username: `sa`
  - Password: (empty)

## Demo Credentials

| Username   | Password   | Role                | Access                    |
|------------|------------|---------------------|---------------------------|
| admin      | admin      | Admin               | Full access               |
| production | production | ProductionUser      | Production operations     |
| finance    | finance    | FinanceUser         | Finance operations        |
| viewer     | viewer     | Viewer              | Read-only                 |

## API Endpoints

### Commands (POST)

```bash
# Receive inventory
POST /v1/inventory/receipts

# Transfer inventory
POST /v1/inventory/transfers

# Issue to batch
POST /v1/batches/{batchId}/issues

# (Additional endpoints in BatchController - to be implemented)
POST /v1/batches                           # CreateBatch
POST /v1/batches/{batchId}/release         # ReleaseBatch
POST /v1/batches/{batchId}/backflush       # BackflushConsumption
POST /v1/batches/{batchId}/close           # CloseBatch
POST /v1/finance/periods/{periodId}/close  # ClosePeriod
```

### Queries (GET)

```bash
# Batch cost view
GET /v1/views/batch-cost/{batchId}

# Inventory valuation
GET /v1/views/inventory-valuation?siteId={siteId}

# WIP view
GET /v1/views/wip?siteId={siteId}

# COGS bridge
GET /v1/views/cogs-bridge/{periodId}

# Exception queue
GET /v1/views/exceptions?status={status}
```

## curl Examples

### Receive Lot
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

### Query Inventory Valuation
```bash
curl http://localhost:8080/v1/views/inventory-valuation \
  -u viewer:viewer
```

## PowerShell Examples

### Receive Lot
```powershell
$headers = @{ "Content-Type" = "application/json" }
$body = @{
    tenantId = "TENANT001"
    siteId = "SITE001"
    receiptId = "RCV001"
    itemId = "FLOUR"
    lotId = "LOT123"
    qty = 1000
    uom = "KG"
    unitCostBase = 2.50
    occurredAtUtc = "2026-03-03T10:00:00Z"
    idempotencyKey = "rcv001-key"
} | ConvertTo-Json

$cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin"))
$headers["Authorization"] = "Basic $cred"

Invoke-RestMethod -Uri "http://localhost:8080/v1/inventory/receipts" `
  -Method Post -Headers $headers -Body $body
```

## Key Architecture Concepts

### Event Sourcing
- All changes recorded as immutable events
- Events ordered by `ledgerSeq`
- Events stored in `EventStore`
- Projections rebuild from events

### CQRS
- **Commands** → Handlers → Events → EventStore
- **Queries** → Projections (read models)
- Separate write and read paths

### Idempotency
- Every command requires `idempotencyKey`
- Duplicate commands return cached result
- Exactly-once guarantee

### Entry Classes
- **FINANCIAL**: Affects costs, delays period close
- **OPERATIONAL**: Markers/transfers, does NOT delay close

### Lot References
Per LOT_REFERENCE_LAW:
- Lot key: `(siteId, lotId)`
- Always include both fields

### Period Close
Per closePeriodLaw:
1. Period: OPEN → CLOSING
2. Compute cutoffSeqFinancial (FINANCIAL only)
3. Wait for financialWatermark >= cutoffSeqFinancial
4. Finalize reports
5. Period: CLOSING → LOCKED

## File Structure

```
breadcost-app/
├── pom.xml                    # Maven config
├── README.md                  # Full documentation
├── SETUP.md                   # Installation guide
├── SUMMARY.md                 # Generated files summary
├── QUICKREF.md                # This file
└── src/
    ├── main/
    │   ├── java/com/breadcost/
    │   │   ├── BreadCostApplication.java
    │   │   ├── api/           # REST controllers (4)
    │   │   ├── commands/      # Commands & handlers (9)
    │   │   ├── domain/        # Domain entities (11)
    │   │   ├── events/        # Event classes (9)
    │   │   ├── eventstore/    # Event sourcing (3)
    │   │   ├── projections/   # Read models (3)
    │   │   ├── security/      # RBAC (1)
    │   │   ├── validation/    # Rules (1)
    │   │   └── finance/       # Finance logic (1)
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/breadcost/
            └── BreadCostApplicationTests.java
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Maven not found | Install Maven, add to PATH, restart PowerShell |
| Java not found | Install Java 17, set JAVA_HOME |
| Port 8080 in use | Change `server.port` in application.properties |
| 401 Unauthorized | Include Basic Auth header with credentials |
| Context fails to load | Check logs, verify dependencies |

## Development Workflow

1. **Make changes** to Java files
2. **Rebuild**: `mvn clean compile`
3. **Test**: `mvn test`
4. **Run**: `mvn spring-boot:run`
5. **Test API** with curl or Postman
6. **Check logs** for event processing
7. **Query H2** console for projection state

## IDE Tips

### IntelliJ IDEA
- Right-click `BreadCostApplication.java` → Run
- Use HTTP Client for API testing
- Enable Lombok plugin

### VS Code
- Install Java Extension Pack
- Install Spring Boot Extension Pack
- F5 to debug

## Monitoring

### Logs
Watch console for:
- Event appended messages
- Projection updates
- Watermark changes
- Command executions

### H2 Console
Query tables:
- `batch_cost_view`
- `inventory_valuation_view`

### Event Store
Check in-memory event store via logs:
- Events by ledgerSeq
- Idempotency records

## Next Steps

1. ✅ Install Java 17 and Maven (see SETUP.md)
2. ✅ Build project: `mvn clean package`
3. ✅ Run application: `mvn spring-boot:run`
4. ✅ Test with curl examples above
5. ⚠️ Implement remaining batch commands
6. ⚠️ Add projection calculations
7. ⚠️ Implement recognition workflow
8. ⚠️ Add persistent storage

## Documentation

- **README.md**: Complete documentation (9000+ words)
- **SETUP.md**: Installation guide
- **SUMMARY.md**: All files summary
- **QUICKREF.md**: This quick reference

## Support

For detailed information, see:
- README.md - Architecture, API examples, development
- SETUP.md - Installation troubleshooting
- Code comments - Implementation details
- Specs - breadcost_v16_final_artifact_pack/

---

**Version**: 1.0.0-SNAPSHOT  
**Date**: March 3, 2026
