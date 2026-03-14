# BreadCost Manufacturing Cost Accounting System

Event-sourced CQRS application for bakery manufacturing cost accounting with full-stack web UI, RBAC, i18n (Armenian/English), and 469 backend tests + 95 E2E Playwright tests.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.4.2, Spring Data JPA, Spring Security (JWT) |
| Database | PostgreSQL 16 (prod), H2 (test profile) |
| Migrations | Flyway |
| Build | Gradle 8 (Kotlin DSL) |
| Frontend | Next.js 14, React 18, TypeScript, TailwindCSS |
| E2E Tests | Playwright 1.58 |
| Infra | Docker Compose (PostgreSQL) |

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- Docker (for PostgreSQL)

### Run

```powershell
# Start PostgreSQL
docker-compose up -d

# Backend (port 8080)
./gradlew bootRun

# Frontend (port 3000)
cd frontend && npm install && npm run dev
```

### Test

```powershell
# Backend (469 tests)
./gradlew test

# E2E (95 tests — requires backend + frontend running)
cd frontend && npx playwright test
```

### Demo Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN — full access |
| manager | manager123 | MANAGER — production + reports |
| technologist | tech123 | TECHNOLOGIST — recipes + plans |
| cashier | cashier123 | CASHIER — POS operations |

## Repository Structure

```
breadcost-app/
│
├── src/                        # Java backend (Spring Boot)
│   ├── main/java/com/breadcost/
│   └── main/resources/         # application.properties, Flyway migrations
│
├── frontend/                   # Next.js frontend
│   ├── app/                    # Pages (login, dashboard, pos, products, recipes, …)
│   ├── components/             # Shared UI components
│   ├── lib/                    # API client, auth, i18n
│   └── locales/                # en.ts, hy.ts (Armenian)
│
├── requirements/               # Frozen specifications
│   ├── REQUIREMENTS.md         #   Functional & non-functional requirements
│   ├── FE_REQUIREMENTS.md      #   Frontend-specific requirements
│   ├── FRONTEND_SPEC.md        #   Page-by-page frontend specification
│   └── FIGMA_DESIGN_PROMPT.md  #   Design system & Figma guidance
│
├── architecture/               # Architecture documentation
│   ├── ARCMAP.md               #   Arc-by-arc architecture map
│   ├── ARCMAP_FRAMEWORK.md     #   Arc mapping methodology reference
│   ├── ARCHITECTURE_REVIEW.md  #   Architecture review findings
│   └── FRONTEND_INVENTORY.md   #   27-page component inventory
│
├── work/                       # Current project state
│   ├── BACKLOG.md              #   57 prioritized issues (P0–P4)
│   ├── NEXT_STEPS.md           #   Release plan & next actions
│   └── ARC_OBSERVATIONS.md     #   Raw arc validation notes (reference)
│
├── qa/                         # Test planning
│   ├── GUI_TEST_PLAN.md        #   GUI test scenarios
│   └── MANUAL_TEST_PLAN.md     #   Manual test checklist
│
├── jira/                       # Jira configuration & reports
│   ├── JIRA.md                 #   Jira setup & ticket mapping
│   └── JIRA_R4.md              #   Release 4 Jira plan
│
├── archive/                    # Stale/superseded documents & scripts
│
├── build.gradle.kts            # Gradle build (Java 21, Spring Boot 3.4.2)
├── docker-compose.yml          # PostgreSQL 16
├── Dockerfile                  # Production container build
└── README.md                   # ← you are here
```

## License

Copyright (c) 2026 BreadCost Project

---

**Version**: 1.0.0-SNAPSHOT  
**Generated**: March 3, 2026  
**Based on**: breadcost_v16_final_artifact_pack
