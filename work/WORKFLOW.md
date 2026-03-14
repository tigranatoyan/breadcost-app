# BreadCost — Standard Development Workflow

**Created**: March 14, 2026
**Status**: Active — follow this on every work session

---

## Phase 0 — Pick Work

1. Open the **active Jira sprint** on board BC. Pick the highest-priority To Do ticket.
2. If no sprint is active, create one first (see Release Workflow below).
3. If the ticket is vague, check `requirements/` and `architecture/` docs to understand the spec.
4. Create a todo list for the item.
5. **Jira**: Move ticket **To Do → In Progress**: `python scripts/jira_tracker.py transition BC-XXX in_progress`

## Phase 1 — Branch

5. Create feature branch from `main`: `git checkout -b feat/<JIRA-KEY>-<short-name>`
6. Branch naming conventions:
   - `feat/BC-123-pos-deduction` — new feature
   - `fix/BC-124-reports-crash` — bug fix
   - `refactor/BC-125-cleanup-auth` — refactoring
   - `docs/BC-126-update-arcmap` — documentation only
7. **Jira**: Add branch name to the ticket (link to GitHub branch if integration active)

## Phase 2 — Analyze

8. Read relevant source files to understand current behavior
9. Identify all files that need changes (backend + frontend if applicable)
10. If the fix touches an arc, re-check `architecture/ARCMAP.md` for cross-cutting concerns

## Phase 3 — Develop

11. Implement changes — backend first, then frontend
12. Keep commits atomic and focused (one logical change per commit)
13. Commit messages: `<type>(scope): <JIRA-KEY> description`
    - `fix(pos): BC-124 deduct inventory on sale`
    - `feat(reports): BC-130 add yield tracking chart`
    - `test(e2e): BC-124 add POS deduction E2E test`
14. **Jira**: If work reveals sub-tasks or blockers, create them immediately in Jira — don't defer

## Phase 4 — Test

15. Run backend tests: `./gradlew test` — all 469+ must pass
16. Run E2E tests: `cd frontend && npx playwright test` — all 95+ must pass
17. If a new feature, add tests before marking done
18. Fix any regressions before proceeding
19. **Jira**: If tests reveal new bugs, create Jira tickets for them immediately (don't just note them)

## Phase 5 — Merge

20. Push feature branch: `git push -u origin feat/<JIRA-KEY>-<short-name>`
21. Create merge request (MR) from feature branch → `main`
    - MR title: `<JIRA-KEY>: <description>`
    - MR description: link to Jira ticket, summary of changes, test results
22. Review the diff — no stray files, no debug code, no secrets
23. Merge (squash or regular merge depending on commit count)
24. Delete the feature branch after merge
25. Pull latest main: `git checkout main && git pull`
26. **Jira**: Move ticket to **Done**: `python scripts/jira_tracker.py transition BC-XXX done`

## Phase 6 — Update Docs

27. If the fix changes behavior documented in `requirements/` or `architecture/`, update those docs
28. Update `work/NEXT_STEPS.md` if release scope changed (new test count, new endpoint count, etc.)
29. Commit doc updates on `main`: `docs: update NEXT_STEPS after BC-124`

## Phase 7 — Repeat

31. Return to Phase 0, pick next item

---

## Jira Lifecycle (Throughout)

Jira is not a phase — it runs **parallel to all phases**. State transitions must happen in real time, not batched.

### Ticket States

| Git/Dev Event | Jira Transition | When |
|---------------|-----------------|------|
| Item picked from sprint | **To Do → In Progress** | Phase 0, step 5 |
| Branch created | Add branch link to ticket | Phase 1, step 7 |
| Blocker found during dev | Create blocker ticket, link it | Phase 3, step 14 |
| Bug found in testing | Create bug ticket immediately | Phase 4, step 19 |
| MR created | Add MR link to ticket | Phase 5, step 21 |
| MR merged | **In Progress → Done** | Phase 5, step 26 |

### Release Workflow

**Current release**: R6 — Arc Validation Fixes (59 tickets, all To Do)

To start working on R6 (or any future release):

1. **Create Sprint** in Jira: group tickets by priority (P0 first, then P1, etc.), set sprint goal
2. **Start Sprint**: all sprint tickets are To Do
3. **During Sprint**: Tickets move To Do → In Progress → Done per the workflow phases above
4. **Sprint End**: Close sprint, move incomplete tickets to next sprint, update velocity
5. **Release Close**: When all tickets Done — mark release as shipped in Jira, tag in git: `git tag R6`

For future releases:
1. **Create Release** in Jira (version = `R7`, etc.)
2. **Create tickets** in Jira (Jira is the sole source of truth for all work items)
3. Follow steps 1–5 above

### Git ↔ Jira Synchronization

- `work/BACKLOG.md` is archived — Jira has all 59 R6 tickets (historical snapshot in `archive/BACKLOG.md`)
- **Jira is the source of truth** for what to work on (sprints, priorities, assignments)
- Every Jira ticket gets a **git branch** (naming: `feat/BC-123-short-name`)
- Every commit references the **Jira key** in the message (`fix(pos): BC-123 ...`)
- Every MR title includes the **Jira key** (`BC-123: Fix POS inventory deduction`)
- GitHub Issues mirror Jira tickets for code-level tracking (use GitHub issue number in commits if needed: `Fixes #45`)

---

## Rules

- **Never commit directly to `main`** for feature/fix work
- **All tests must pass** before merge (backend + E2E)
- **One Jira ticket per branch** (exception: trivial batch of related fixes)
- **Jira state must match reality** — if you're working on it, ticket is In Progress; if it's merged, ticket is Done
- **Always use `jira_tracker.py`** for transitions — it logs timestamps locally for sprint reports
- **Update NEXT_STEPS.md** on the merge commit to `main`, not on the feature branch
- **No orphan work** — every code change traces to a Jira ticket

## Sprint Dashboard

```bash
python scripts/jira_tracker.py status     # Current sprint board
python scripts/jira_tracker.py report     # Generate XLSX time report → data/sprint_report.xlsx
```
