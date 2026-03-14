# ArcMap Framework

> **NOTE:** This is a methodology reference, not project-specific. It has already been applied to produce [ARCMAP.md](ARCMAP.md). No further analysis needed from this file.

**Version:** 1.0 | **Paradigm:** Structured Analysis → Agile Implementation

> *ArcMap* is a software design methodology that decomposes a system into end-to-end
> actor-driven flows ("arcs"), maps them to releases, and tracks progress top-down.
> Analysis is exhaustive and upfront. Implementation is iterative (scrum).
> The ArcMap file is the source of truth for *what the software does*.
> The tracking engine (JIRA/stories) is the source of truth for *how it gets built*.

---

## 1. When to Use ArcMap

| Software Type | Fit | Arc Discovery Method |
|---------------|-----|---------------------|
| Business process (ERP, CRM, e-commerce) | **Excellent** | Actor-driven flows: who does what, in what order |
| API / Platform software | Good | Consumer journey arcs: how an integrator onboards, authenticates, calls, handles errors |
| Infrastructure / DevOps | Moderate | System event arcs: deploy, scale, fail, recover, monitor |
| Library / SDK | Low | Use contract-first design instead. Arcs = consumer usage patterns |
| R&D / Exploratory | Low | Use spike-based discovery. Arcs emerge after prototyping, not before |

---

## 2. Roles

| Role | Responsibility |
|------|----------------|
| **Domain Expert** | Describes how the business works today (manual or digital) |
| **Stakeholder** | Defines business goals, priorities, and constraints |
| **Architect (or AI)** | Interviews, decomposes arcs, identifies gaps, writes the ArcMap |
| **Product Owner** | Owns the ArcMap, approves release boundaries, validates completeness |
| **Dev Team** | Implements stories, flags technical constraints that affect arcs |

---

## 3. Discovery Process

When starting a new project or module, the Architect reads this framework and conducts
a structured interview. The interview has 4 phases.

### Phase 1: Domain Landscape (5-10 questions)

Goal: Understand the business before touching software.

```
1. What does this business/system do in one sentence?
2. Who are the primary ACTORS (humans or external systems that interact with it)?
   - List each with their role and goal.
3. What is the CORE TRANSACTION? (The one thing that, if it doesn't work, 
   the software is useless.)
4. What other businesses/systems does this integrate with?
5. What is the current process? (Manual, spreadsheet, legacy software?)
6. What are the top 3 PAIN POINTS with the current process?
7. Are there regulatory or compliance requirements?
8. What are the business metrics for success? (Revenue, time saved, error rate?)
9. How many users? Tenants? Scale expectations?
10. What is already built (if anything)?
```

### Phase 2: Arc Discovery (per actor)

Goal: Map every end-to-end flow. Ask for each actor identified in Phase 1:

```
1. What TRIGGERS this actor to start? (Event, schedule, notification, manual decision?)
2. What is the HAPPY PATH? Step by step, what happens?
   - For each step: Who acts → What action → What system response → Who is next?
3. What can GO WRONG at each step? (Unhappy paths)
   - For each failure: What happens? Who is notified? How is it recovered?
4. Where does the flow END? What is the completion signal?
5. Does this flow HAND OFF to another arc? (Cross-arc dependency)
6. What DATA is created, read, updated, or deleted at each step?
7. What NOTIFICATIONS or ALERTS happen during this flow?
8. Are there TIME CONSTRAINTS? (SLAs, deadlines, batch windows)
```

### Phase 3: Arc Validation

Goal: Catch gaps before writing stories.

```
1. Read each arc back to the domain expert. "Did I miss anything?"
2. For each HANDOFF between arcs: is the receiving arc documented?
3. For each UNHAPPY PATH: is there a recovery or escalation?
4. For each DATA entity: is it created by one arc and consumed by another?
   If yes, document the dependency.
5. Are there arcs that CONFLICT? (Two actors doing the same thing differently?)
6. PERMISSION CHECK: For each step, which roles can perform it?
```

### Phase 4: Prioritization & Release Mapping

Goal: Sequence arcs into releases.

```
1. Which arcs are CORE? (System is unusable without them.) → Release 1.
2. Which arcs EXTEND core functionality? → Release 2-3.
3. Which arcs are NICE-TO-HAVE or FUTURE? → Release 4+.
4. Within each release, which arcs have DEPENDENCIES on other arcs?
   → These determine sprint order.
5. What is the MINIMUM VIABLE ARC? (Can we ship a partial arc that's useful?)
```

---

## 4. ArcMap File Format

The output of discovery is a single file called `ARCMAP.md` (or `<PROJECT>_ARCMAP.md`).

### Structure

```markdown
# [Project Name] — ArcMap
**Version:** X.Y | **Last Updated:** YYYY-MM-DD | **Status:** Draft | Active | Frozen

## Actors
| Actor | Role | Primary Arcs |
|-------|------|-------------|

## Arc 1: [Name] — [Trigger] → [End State]
**Actors:** A, B, C | **Status:** DONE | R4 | R5+ | GAP
**Depends on:** Arc X (step Y)

### Happy Path
Step | Actor        | Action                    | System Response          | Next
-----|-------------|---------------------------|--------------------------|-----
1    | Customer    | Places order via portal    | Order created (DRAFT)    | 2
2    | System      | Validates stock + pricing  | Stock reserved           | 3
3    | Admin       | Reviews & confirms order   | Status → CONFIRMED       | 4
...

### Unhappy Paths
Step | Failure              | Recovery                    | Stories
-----|---------------------|-----------------------------|--------
2    | Insufficient stock   | Notify customer, suggest alt | BC-XXXX
2    | Price changed        | Show updated total, re-confirm | BC-XXXX

### Cross-Arc Dependencies
- Step 2 requires: Inventory Arc → stock check
- Step 6 triggers: Production Arc → create production plan
- Step 9 triggers: Delivery Arc → assign driver

### Release Coverage
| Release | Steps Covered | Notes |
|---------|--------------|-------|
| R1-R3   | 1-5, 8       | Core order CRUD, no portal |
| R4      | 1-9 (full)   | Portal checkout + tracking  |
```

### Rules

1. **Max 200 lines.** If longer, arcs are too detailed — push detail into stories.
2. **Story IDs are references only.** Never duplicate acceptance criteria here.
3. **Update per release**, not per story or sprint.
4. **Each arc step is one line.** If a step needs explanation, it's actually two steps.
5. **Status per arc**, not per step. Status = the release that completes the arc.
6. **Unhappy paths can be briefer than happy paths.** Name the failure + recovery, link the story.
7. **Cross-arc dependencies are mandatory.** If an arc has none, you missed something.

---

## 5. Lifecycle

```
┌──────────────────────────────────────────────────────────┐
│  DISCOVERY (waterfall)                                    │
│  Interviews → Arc decomposition → Validation → ArcMap     │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  RELEASE PLANNING                                         │
│  ArcMap → Epics → Stories → Sprint assignment              │
│  (JIRA_RX.md is created here, referencing ArcMap steps)   │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  SPRINT EXECUTION (scrum)                                 │
│  Stories → Code → Tests → Review → Done                   │
│  (ArcMap is NOT updated during sprints)                   │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  RELEASE VALIDATION                                       │
│  Walk each arc end-to-end (E2E test / demo / manual)      │
│  ✓ Happy path works across all steps                      │
│  ✓ Unhappy paths handled                                  │
│  ✓ Cross-arc handoffs verified                            │
│  ✗ Broken seam found → hotfix story for next sprint       │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  ARCMAP UPDATE                                            │
│  Mark completed arcs/steps as DONE                        │
│  Update release coverage table                            │
│  Flag new GAPs discovered during validation               │
│  Commit alongside release tag                             │
└──────────────────────────────────────────────────────────┘
```

---

## 6. Anti-Patterns

| Anti-Pattern | Why It Fails | Fix |
|-------------|-------------|-----|
| ArcMap has acceptance criteria | Duplicates JIRA, drifts immediately | Story IDs only, never criteria |
| ArcMap updated per story | Too frequent, creates merge conflicts, noise | Update per release only |
| Arcs describe UI, not flow | "User sees a table" is not a flow step | Rewrite as Actor → Action → System Response |
| No unhappy paths | 60% of bugs live in error handling | Mandate at least 1 unhappy path per arc |
| No cross-arc dependencies | Sprint planning misses prerequisites | Validate during Phase 3 |
| ArcMap > 300 lines | Nobody reads it, becomes stale | Split into sub-ArcMaps per domain |
| Arc = Epic | 1:1 mapping removes the value of arcs | Arcs cross epics — that's the point |

---

## 7. Adapting for Non-Business Software

### API / Platform
- **Actor** = API consumer (developer)
- **Arc** = integration journey (discover → auth → first call → error handling → scale)
- **Unhappy path** = rate limit, auth failure, breaking change migration

### Infrastructure
- **Actor** = system component or operator
- **Arc** = operational scenario (deploy → health check → auto-scale → failure → recovery → alert)
- **Unhappy path** = cascade failure, data corruption, network partition

### Library / SDK
- **Actor** = downstream developer
- **Arc** = usage pattern (install → configure → basic use → advanced use → extend)
- **Unhappy path** = version conflict, misconfiguration, edge case

For all types, the discovery interview adapts:
- Replace "business process" questions with "operational scenario" or "consumer journey" questions
- Replace "regulatory compliance" with "SLA/SLO requirements"
- Replace "pain points" with "failure modes"

---

## 8. AI-as-PO Vision

The discovery process (Phase 1-4) is a structured interview. An AI agent can:

1. **Conduct the interview** — ask Phase 1-4 questions, follow up on vague answers
2. **Generate the ArcMap draft** — structure answers into the standard format
3. **Validate completeness** — check for missing unhappy paths, undocumented handoffs
4. **Cross-reference existing code** — if code exists, audit it against discovered arcs
5. **Maintain the ArcMap** — update status after release validation

This does NOT replace the Product Owner. It replaces the *mechanical work* of:
- Taking interview notes and structuring them
- Checking for gaps in coverage
- Cross-referencing stories to arcs
- Generating release plans from arc priorities

The PO still makes **judgment calls**: priority, scope cuts, business trade-offs.

---

*When you encounter this file at the start of a project, begin Phase 1 discovery immediately.*
