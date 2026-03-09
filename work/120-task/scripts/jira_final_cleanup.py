#!/usr/bin/env python3
"""
jira_final_cleanup.py — Final Jira cleanup: assign sprints, close sprints,
transition remaining epics, release versions.

Actions:
  1. Transition R3-FE epics BC-245, BC-246 → Done
  2. Assign R2 stories BC-88..BC-121 to their sprints (8–13)
  3. Move R2 sprints 8–13 to 'active' then 'closed'
  4. Move R3 sprints 14–16 to 'active' then 'closed'
  5. Mark R1.5 sprint 4–7 as 'closed' (if not already)
  6. Release versions R1.5, R2, R3

Usage:
    python jira_final_cleanup.py          # dry run
    python jira_final_cleanup.py --run    # live
"""
import sys, json, base64, urllib.request, urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

DRY_RUN = "--run" not in sys.argv
MODE = "DRY RUN" if DRY_RUN else "LIVE"

_TOKEN = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
_HDRS = {
    "Authorization": f"Basic {_TOKEN}",
    "Accept": "application/json",
    "Content-Type": "application/json",
}


def api(method, path, body=None):
    url = JIRA_BASE_URL + path
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=_HDRS, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode()[:300]}


def get_transitions(key):
    code, data = api("GET", f"/rest/api/3/issue/{key}/transitions")
    if code != 200:
        return {}
    return {t["name"].lower(): t["id"] for t in data.get("transitions", [])}


def transition_to_done(key):
    transitions = get_transitions(key)
    done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
    if done_id:
        code, _ = api("POST", f"/rest/api/3/issue/{key}/transitions", {"transition": {"id": done_id}})
        return code in (200, 204)
    ip_id = transitions.get("in progress") or transitions.get("start progress")
    if ip_id:
        api("POST", f"/rest/api/3/issue/{key}/transitions", {"transition": {"id": ip_id}})
        transitions = get_transitions(key)
        done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
        if done_id:
            code, _ = api("POST", f"/rest/api/3/issue/{key}/transitions", {"transition": {"id": done_id}})
            return code in (200, 204)
    print(f"    WARN: no Done transition for {key}, available: {list(transitions.keys())}")
    return False


def banner(num, title):
    print(f"\n{'=' * 60}")
    print(f"{num}. {title}  [{MODE}]")
    print(f"{'=' * 60}")


# ═══════════════════════════════════════════════════════════
#  1. Transition R3-FE epics BC-245, BC-246 → Done
# ═══════════════════════════════════════════════════════════
banner(1, "TRANSITION R3-FE EPICS TO DONE")
r3fe_epics = ["BC-245", "BC-246"]
for key in r3fe_epics:
    if DRY_RUN:
        print(f"  DRY  {key} → Done")
    else:
        if transition_to_done(key):
            print(f"  DONE  {key}")
        else:
            print(f"  FAIL  {key}")

# ═══════════════════════════════════════════════════════════
#  2. Assign R2 stories to their sprints
# ═══════════════════════════════════════════════════════════
banner(2, "ASSIGN R2 STORIES TO SPRINTS")

# Story JIRA keys → Sprint ID mapping
# Sprint 8 (id=44): Customer Portal BC-88..BC-92
# Sprint 9 (id=45): Loyalty BC-93..BC-98
# Sprint 10 (id=46): Suppliers BC-99..BC-104
# Sprint 11 (id=47): Delivery BC-105..BC-110
# Sprint 12 (id=48): Finance BC-111..BC-115 + Subscription BC-120..BC-121
# Sprint 13 (id=49): Reports BC-116..BC-119 + WhatsApp BC-85..BC-87
SPRINT_ASSIGNMENTS = {
    44: [f"BC-{n}" for n in range(88, 93)],     # Sprint 8 Portal
    45: [f"BC-{n}" for n in range(93, 99)],      # Sprint 9 Loyalty
    46: [f"BC-{n}" for n in range(99, 105)],     # Sprint 10 Suppliers
    47: [f"BC-{n}" for n in range(105, 111)],    # Sprint 11 Delivery
    48: [f"BC-{n}" for n in range(111, 116)]     # Sprint 12 Finance
       + ["BC-120", "BC-121"],                    # Sprint 12 Subscription
    49: [f"BC-{n}" for n in range(116, 120)]     # Sprint 13 Reports
       + ["BC-85", "BC-86", "BC-87"],             # Sprint 13 WhatsApp
}

for sprint_id, keys in SPRINT_ASSIGNMENTS.items():
    for key in keys:
        if DRY_RUN:
            print(f"  DRY  {key} → sprint {sprint_id}")
        else:
            # Move issue into sprint via Agile API
            code, data = api("POST",
                f"/rest/agile/1.0/sprint/{sprint_id}/issue",
                {"issues": [key]})
            # Agile API returns 204 on success
            if code in (200, 204):
                print(f"  OK   {key} → sprint {sprint_id}")
            else:
                print(f"  FAIL {key} → sprint {sprint_id} (HTTP {code})")

# Also assign R3 stories to R3 sprints if not already assigned
# R3 S14 (id=83): WhatsApp + FX BC-122..BC-125, BC-134, BC-135
# R3 S15 (id=84): Forecast + Driver BC-126..BC-128, BC-131..BC-133
# R3 S16 (id=85): Pricing + Mobile BC-129, BC-130, BC-136
R3_SPRINT_ASSIGNMENTS = {
    83: ["BC-122", "BC-123", "BC-124", "BC-125", "BC-134", "BC-135"],  # S14 WhatsApp + FX
    84: ["BC-126", "BC-127", "BC-128", "BC-131", "BC-132", "BC-133"],  # S15 Forecast + Driver
    85: ["BC-129", "BC-130", "BC-136"],                                 # S16 Pricing + Mobile
}

print()
print("  -- R3 stories --")
for sprint_id, keys in R3_SPRINT_ASSIGNMENTS.items():
    for key in keys:
        if DRY_RUN:
            print(f"  DRY  {key} → sprint {sprint_id}")
        else:
            code, data = api("POST",
                f"/rest/agile/1.0/sprint/{sprint_id}/issue",
                {"issues": [key]})
            if code in (200, 204):
                print(f"  OK   {key} → sprint {sprint_id}")
            else:
                print(f"  FAIL {key} → sprint {sprint_id} (HTTP {code})")

# Also assign R3-FE stories to their sprints
# R3-FE Sprint 8 (reuse R3 S14=83 for stories): BC-247, BC-248, BC-249, BC-250
# R3-FE Sprint 9 (reuse R3 S15=84 for stories): BC-251, BC-252, BC-253
# Actually R3-FE used the same R3 version. Let me check the sprint setup...
# From setup_r3fe_sprints.py, R3-FE stories were put in R3 version sprints.
# The R3 sprints are already listed as S14, S15, S16. But R3-FE stories
# (BC-247..BC-253) may already be assigned. Let me include them just in case.

print()
print("  -- R3-FE stories --")
R3FE_SPRINT_ASSIGNMENTS = {
    83: ["BC-247", "BC-248", "BC-249", "BC-250"],  # S14 (Sprint 8 scope)
    84: ["BC-251", "BC-252", "BC-253"],              # S15 (Sprint 9 scope)
}
for sprint_id, keys in R3FE_SPRINT_ASSIGNMENTS.items():
    for key in keys:
        if DRY_RUN:
            print(f"  DRY  {key} → sprint {sprint_id}")
        else:
            code, data = api("POST",
                f"/rest/agile/1.0/sprint/{sprint_id}/issue",
                {"issues": [key]})
            if code in (200, 204):
                print(f"  OK   {key} → sprint {sprint_id}")
            else:
                print(f"  FAIL {key} → sprint {sprint_id} (HTTP {code})")

# ═══════════════════════════════════════════════════════════
#  3. Close R2 sprints (future → active → closed)
# ═══════════════════════════════════════════════════════════
banner(3, "CLOSE R2 SPRINTS 8–13")

R2_SPRINT_IDS = [44, 45, 46, 47, 48, 49]
R3_SPRINT_IDS = [83, 84, 85]
R15_SPRINT_IDS = [40, 41, 42, 43]

def close_sprint(sprint_id, name):
    """Transition sprint: future → active → closed."""
    if DRY_RUN:
        print(f"  DRY  close sprint {sprint_id} ({name})")
        return True

    # Get current state
    code, data = api("GET", f"/rest/agile/1.0/sprint/{sprint_id}")
    if code != 200:
        print(f"  FAIL  get sprint {sprint_id}: HTTP {code}")
        return False

    state = data.get("state", "unknown")

    if state == "future":
        # Must activate first
        code, _ = api("POST", f"/rest/agile/1.0/sprint/{sprint_id}",
                       {"state": "active"})
        if code not in (200, 204):
            # Try PUT instead
            code, _ = api("PUT", f"/rest/agile/1.0/sprint/{sprint_id}",
                          {"state": "active"})
        if code in (200, 204):
            print(f"  ACTIVE  sprint {sprint_id} ({name})")
        else:
            print(f"  FAIL  activate sprint {sprint_id}: HTTP {code}")
            return False

    if state in ("future", "active"):
        # Now close
        code, resp = api("PUT", f"/rest/agile/1.0/sprint/{sprint_id}",
                         {"state": "closed"})
        if code in (200, 204):
            print(f"  CLOSED  sprint {sprint_id} ({name})")
            return True
        else:
            # Try POST
            code, resp = api("POST", f"/rest/agile/1.0/sprint/{sprint_id}",
                             {"state": "closed"})
            if code in (200, 204):
                print(f"  CLOSED  sprint {sprint_id} ({name})")
                return True
            print(f"  FAIL  close sprint {sprint_id}: HTTP {code} {resp}")
            return False
    elif state == "closed":
        print(f"  SKIP  sprint {sprint_id} ({name}) already closed")
        return True
    else:
        print(f"  WARN  sprint {sprint_id} ({name}) in unexpected state: {state}")
        return False


SPRINT_NAMES = {
    44: "R2 Sprint 8 Portal", 45: "R2 Sprint 9 Loyalty",
    46: "R2 Sprint 10 Suppliers", 47: "R2 Sprint 11 Delivery",
    48: "R2 Sprint 12 Finance", 49: "R2 Sprint 13 Reports",
}

for sid in R2_SPRINT_IDS:
    close_sprint(sid, SPRINT_NAMES[sid])

# ═══════════════════════════════════════════════════════════
#  4. Close R3 sprints
# ═══════════════════════════════════════════════════════════
banner(4, "CLOSE R3 SPRINTS 14–16")
R3_SPRINT_NAMES = {
    83: "R3 S14 WhatsApp + FX",
    84: "R3 S15 Forecast + Driver",
    85: "R3 S16 Pricing + Mobile",
}
for sid in R3_SPRINT_IDS:
    close_sprint(sid, R3_SPRINT_NAMES[sid])

# ═══════════════════════════════════════════════════════════
#  5. Close R1.5 sprints (if not already)
# ═══════════════════════════════════════════════════════════
banner(5, "VERIFY R1.5 SPRINTS 4–7 CLOSED")
R15_SPRINT_NAMES = {
    40: "R1.5 Sprint 4 Inventory", 41: "R1.5 Sprint 5 POS",
    42: "R1.5 Sprint 6 Admin", 43: "R1.5 Sprint 7 Reports",
}
for sid in R15_SPRINT_IDS:
    close_sprint(sid, R15_SPRINT_NAMES[sid])

# ═══════════════════════════════════════════════════════════
#  6. Release versions R1.5, R2, R3
# ═══════════════════════════════════════════════════════════
banner(6, "RELEASE VERSIONS")

VERSIONS_TO_RELEASE = {
    "10035": "R1.5 — Frontend E2E Completion",
    "10001": "R2 — Growth",
    "10002": "R3 — AI + Mobile",
}

for vid, vname in VERSIONS_TO_RELEASE.items():
    if DRY_RUN:
        print(f"  DRY  release {vname} (id={vid})")
    else:
        code, data = api("PUT", f"/rest/api/3/version/{vid}",
                         {"released": True, "releaseDate": "2026-03-09"})
        if code in (200, 204):
            print(f"  RELEASED  {vname}")
        else:
            print(f"  FAIL  {vname}: HTTP {code} {data}")

# ═══════════════════════════════════════════════════════════
#  Summary
# ═══════════════════════════════════════════════════════════
print(f"\n{'=' * 60}")
print(f"CLEANUP COMPLETE  [{MODE}]")
print(f"{'=' * 60}\n")

if DRY_RUN:
    print("  Re-run with --run to execute changes.")
