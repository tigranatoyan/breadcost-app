#!/usr/bin/env python3
"""jira_cleanup.py — Comprehensive Jira cleanup:
  Phase 1: Delete duplicate version, sprints, epics, stories
  Phase 2: Move stray stories to correct sprints
  Phase 3: Close/delete empty old sprints
  Phase 4: Fix epic fixVersions and statuses

Usage:
  python jira_cleanup.py          # dry run
  python jira_cleanup.py --run    # live run
"""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json",
         "Content-Type": "application/json"}

# ── Release IDs ──
VER_R1   = "10000"   # R1 — Core MVP (released)
VER_R15  = "10035"   # R1.5 — Frontend E2E Completion
VER_R2   = "10001"   # R2 — Growth (canonical)
VER_R2X  = "10036"   # R2 — Growth (DUPLICATE, 0 stories)
VER_R3   = "10002"   # R3 — AI + Mobile

# ── Sprint IDs ──
SP2_OLD     = 5      # Sprint 2 (old R2 bulk sprint, active, 26 stories)
SP3_OLD     = 6      # Sprint 3 (old R3 placeholder, empty)
SP_BC1      = 3      # BC Sprint 1 (orphan, empty)
SP_R3_14DUP = 82     # R3 Sprint 14 (duplicate, empty)

# R2 target sprints
SP_R2_8   = 44  # Portal
SP_R2_9   = 45  # Loyalty
SP_R2_10  = 46  # Suppliers
SP_R2_11  = 47  # Delivery
SP_R2_12  = 48  # Finance
SP_R2_13  = 49  # Reports + Subscriptions
# R3 target sprint
SP_R3_14  = 83  # R3 S14 WhatsApp + FX (the one with stories)

# ── Story moves: from wrong sprints to correct ones ──
# R2 stories currently in Sprint 2 (id=5)
MOVE_FROM_S2 = {
    SP_R2_8:  ["BC-85", "BC-86", "BC-87",     # WhatsApp partial (R2)
               "BC-88", "BC-89", "BC-90", "BC-91", "BC-92"],  # Customer Portal
    SP_R2_9:  ["BC-93", "BC-94", "BC-95", "BC-96", "BC-97", "BC-98"],  # Loyalty
    SP_R2_10: ["BC-99", "BC-100", "BC-101", "BC-102", "BC-103", "BC-104"],  # Suppliers
    SP_R2_11: ["BC-105", "BC-106", "BC-107", "BC-108", "BC-109", "BC-110"],  # Delivery
}
# R2 stories currently in R1.5 sprints
MOVE_FROM_R15 = {
    SP_R2_12: ["BC-111", "BC-112", "BC-113", "BC-114", "BC-115"],  # Invoicing (from S4)
    SP_R2_13: ["BC-116", "BC-117", "BC-118", "BC-119",  # Report Constructor (from S5)
               "BC-120", "BC-121"],                      # Subscriptions (from S6)
}
# R3 stories currently in R1.5 Sprint 7
MOVE_FROM_S7 = {
    SP_R3_14: ["BC-122", "BC-123", "BC-124", "BC-125"],  # AI WhatsApp (from S7)
}

# ── Duplicate issues to DELETE ──
DUP_STORIES = [f"BC-{i}" for i in range(161, 218)]   # BC-161..217 (57 dupe R1 stories)
DUP_EPICS   = [f"BC-{i}" for i in range(137, 161)]   # BC-137..160 (24 dupe epics)

# ── Epic fixVersion + status fixes ──
R1_EPICS = [f"BC-{i}" for i in range(1, 12)]   # BC-1..11
R2_EPICS = [f"BC-{i}" for i in range(12, 19)]  # BC-12..18
DONE_TRANSITION = "31"

# ═══════════════════════════════════════════════════════════════

def jira(method, path, body=None):
    url = JIRA_BASE_URL + path
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=_HDRS, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        try: return e.code, json.loads(raw)
        except: return e.code, {"raw": raw.decode()[:300]}

ok = 0
fail = 0

def log(tag, msg):
    global ok, fail
    if tag == "OK":
        ok += 1
    elif tag == "FAIL":
        fail += 1
    prefix = "  DRY " if DRY else f"  {tag:4s}"
    print(f"{prefix} {msg}")

# ═══════════════════════════════════════════════════════════════

def main():
    global ok, fail
    print("=" * 65)
    print(f"  JIRA CLEANUP — {'DRY RUN' if DRY else 'LIVE RUN'}")
    print("=" * 65)

    # ── PHASE 1: Delete duplicates ────────────────────────────
    print("\n── Phase 1: Delete duplicates ──")

    # 1a. Delete duplicate R2 version
    print(f"\n  1a. Duplicate R2 version (id={VER_R2X})")
    if DRY:
        log("DRY", f"DELETE version {VER_R2X}")
    else:
        s, d = jira("DELETE", f"/rest/api/3/version/{VER_R2X}")
        log("OK" if s in (200, 204) else "FAIL", f"DELETE version {VER_R2X}: {s}")

    # 1b. Delete duplicate R3 Sprint 14
    print(f"\n  1b. Duplicate R3 Sprint 14 (id={SP_R3_14DUP})")
    if DRY:
        log("DRY", f"DELETE sprint {SP_R3_14DUP}")
    else:
        s, d = jira("DELETE", f"/rest/agile/1.0/sprint/{SP_R3_14DUP}")
        log("OK" if s in (200, 204) else "FAIL", f"DELETE sprint {SP_R3_14DUP}: {s}")

    # 1c. Delete orphan BC Sprint 1
    print(f"\n  1c. Orphan BC Sprint 1 (id={SP_BC1})")
    if DRY:
        log("DRY", f"DELETE sprint {SP_BC1}")
    else:
        s, d = jira("DELETE", f"/rest/agile/1.0/sprint/{SP_BC1}")
        log("OK" if s in (200, 204) else "FAIL", f"DELETE sprint {SP_BC1}: {s}")

    # 1d. Delete duplicate R1 stories (BC-161..217)
    print(f"\n  1d. Duplicate R1 stories ({len(DUP_STORIES)} issues)")
    for key in DUP_STORIES:
        if DRY:
            log("DRY", f"DELETE issue {key}")
        else:
            s, d = jira("DELETE", f"/rest/api/3/issue/{key}")
            log("OK" if s in (200, 204) else "FAIL", f"DELETE {key}: {s}")

    # 1e. Delete duplicate epics (BC-137..160)
    print(f"\n  1e. Duplicate epics ({len(DUP_EPICS)} issues)")
    for key in DUP_EPICS:
        if DRY:
            log("DRY", f"DELETE issue {key}")
        else:
            s, d = jira("DELETE", f"/rest/api/3/issue/{key}")
            log("OK" if s in (200, 204) else "FAIL", f"DELETE {key}: {s}")

    # ── PHASE 2: Move stray stories ──────────────────────────
    print("\n── Phase 2: Move stray stories to correct sprints ──")

    all_moves = {}
    all_moves.update(MOVE_FROM_S2)
    for target, issues in MOVE_FROM_R15.items():
        all_moves.setdefault(target, []).extend(issues)
    for target, issues in MOVE_FROM_S7.items():
        all_moves.setdefault(target, []).extend(issues)

    for target_sprint, issue_keys in sorted(all_moves.items()):
        # Look up sprint name
        sprint_names = {
            44: "R2 S8 Portal", 45: "R2 S9 Loyalty", 46: "R2 S10 Suppliers",
            47: "R2 S11 Delivery", 48: "R2 S12 Finance", 49: "R2 S13 Reports",
            83: "R3 S14 WhatsApp+FX"
        }
        name = sprint_names.get(target_sprint, f"Sprint {target_sprint}")
        print(f"\n  → {name} (id={target_sprint}): {len(issue_keys)} stories")
        for k in issue_keys:
            if DRY:
                log("DRY", f"MOVE {k} → sprint {target_sprint}")
            # Live: use agile API to move issues to sprint
        if not DRY:
            s, d = jira("POST", f"/rest/agile/1.0/sprint/{target_sprint}/issue",
                        {"issues": issue_keys})
            if s in (200, 204):
                for k in issue_keys:
                    log("OK", f"MOVE {k} → sprint {target_sprint}")
            else:
                for k in issue_keys:
                    log("FAIL", f"MOVE {k} → sprint {target_sprint}: {s} {str(d)[:80]}")

    # ── PHASE 3: Close/delete old empty sprints ───────────────
    print("\n── Phase 3: Close/delete old sprints ──")

    # 3a. Close Sprint 2 (was active, now hopefully empty)
    print(f"\n  3a. Sprint 2 (id={SP2_OLD}) — close")
    if DRY:
        log("DRY", f"CLOSE sprint {SP2_OLD}")
    else:
        s, d = jira("POST", f"/rest/agile/1.0/sprint/{SP2_OLD}", {"state": "closed"})
        log("OK" if s in (200, 204) else "FAIL", f"CLOSE sprint {SP2_OLD}: {s} {str(d)[:80]}")

    # 3b. Delete Sprint 3 (old R3 placeholder, empty, future)
    print(f"\n  3b. Sprint 3 (id={SP3_OLD}) — delete")
    if DRY:
        log("DRY", f"DELETE sprint {SP3_OLD}")
    else:
        s, d = jira("DELETE", f"/rest/agile/1.0/sprint/{SP3_OLD}")
        log("OK" if s in (200, 204) else "FAIL", f"DELETE sprint {SP3_OLD}: {s}")

    # ── PHASE 4: Fix epic fixVersions and statuses ────────────
    print("\n── Phase 4: Fix epic fixVersions + statuses ──")

    # 4a. R1 epics → fixVersion=R1, transition to Done
    print(f"\n  4a. R1 epics (BC-1..11): fixVersion=R1 + Done")
    for key in R1_EPICS:
        if DRY:
            log("DRY", f"{key} → fixVersion=R1 + Done")
        else:
            s1, _ = jira("PUT", f"/rest/api/3/issue/{key}",
                         {"fields": {"fixVersions": [{"id": VER_R1}]}})
            s2, _ = jira("POST", f"/rest/api/3/issue/{key}/transitions",
                         {"transition": {"id": DONE_TRANSITION}})
            if s1 in (200, 204) and s2 in (200, 204):
                log("OK", f"{key} → fixVersion=R1 + Done")
            else:
                log("FAIL", f"{key}: fv={s1}, done={s2}")

    # 4b. R2 epics → fixVersion=R2
    print(f"\n  4b. R2 epics (BC-12..18): fixVersion=R2")
    for key in R2_EPICS:
        if DRY:
            log("DRY", f"{key} → fixVersion=R2")
        else:
            s, _ = jira("PUT", f"/rest/api/3/issue/{key}",
                         {"fields": {"fixVersions": [{"id": VER_R2}]}})
            log("OK" if s in (200, 204) else "FAIL", f"{key} → fixVersion=R2: {s}")

    # ── SUMMARY ───────────────────────────────────────────────
    print(f"\n{'='*65}")
    if DRY:
        total = len(DUP_STORIES) + len(DUP_EPICS)
        moves = sum(len(v) for v in all_moves.values())
        print(f"  DRY RUN SUMMARY:")
        print(f"    Versions to delete:   1")
        print(f"    Sprints to delete:    3 (dup R3 S14, orphan BC S1, old R3 S3)")
        print(f"    Sprints to close:     1 (Sprint 2)")
        print(f"    Issues to delete:     {total} ({len(DUP_STORIES)} stories + {len(DUP_EPICS)} epics)")
        print(f"    Stories to move:      {moves}")
        print(f"    Epics to fix:         {len(R1_EPICS) + len(R2_EPICS)}")
        print(f"\n  Run with --run to execute")
    else:
        print(f"  LIVE RUN COMPLETE: {ok} OK, {fail} FAIL")
    print("=" * 65)

if __name__ == "__main__":
    main()
