#!/usr/bin/env python3
"""
align_jira.py â€” Comprehensive Jira alignment script.

Brings Jira project state in line with actual codebase state (R1â€“R5 all complete).

Actions:
  1. Delete 24 duplicate epics (BC-137..BC-160)
  2. Delete duplicate version "R2 â€” Growth" (id=10036)
  3. Delete orphan sprint "BC Sprint 1" (id=3)
  4. Transition 24 original epics (BC-1..BC-24) â†’ Done
  5. Transition 4 R1.5 epics (BC-218..BC-221) â†’ Done
  6. Transition 15 R3 stories (BC-122..BC-136) â†’ Done
  7. Find & transition R1.5 FE stories â†’ Done
  8. Assign R2 stories to sprints & fixVersion
  9. Close all open sprints
  10. Release versions R1.5, R2, R3
  11. Create R4 version + R5 version

Usage:
    python align_jira.py            # dry run â€” shows what would happen
    python align_jira.py --run      # live â€” makes changes
"""
import sys
import json
import base64
import urllib.request
import urllib.error
import time

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

stats = {"ok": 0, "fail": 0, "skip": 0}


# â”€â”€ API helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
    """Transition an issue to Done. Tries multiple paths."""
    transitions = get_transitions(key)
    done_id = (transitions.get("done")
               or transitions.get("mark as done")
               or transitions.get("resolve issue"))
    if done_id:
        code, _ = api("POST", f"/rest/api/3/issue/{key}/transitions",
                       {"transition": {"id": done_id}})
        return code in (200, 204)

    # If no Done available, try In Progress first then Done
    ip_id = transitions.get("in progress") or transitions.get("start progress")
    if ip_id:
        api("POST", f"/rest/api/3/issue/{key}/transitions",
            {"transition": {"id": ip_id}})
        time.sleep(0.2)
        transitions = get_transitions(key)
        done_id = (transitions.get("done")
                   or transitions.get("mark as done")
                   or transitions.get("resolve issue"))
        if done_id:
            code, _ = api("POST", f"/rest/api/3/issue/{key}/transitions",
                           {"transition": {"id": done_id}})
            return code in (200, 204)
    print(f"    WARN: no Done transition for {key}, available: {list(transitions.keys())}")
    return False


def check_issue_status(key):
    """Get current status of an issue. Returns status name or None."""
    code, data = api("GET", f"/rest/api/3/issue/{key}?fields=status")
    if code == 200:
        return data.get("fields", {}).get("status", {}).get("name", "Unknown")
    return None


def close_sprint(sprint_id, name):
    """Transition sprint: future â†’ active â†’ closed."""
    if DRY_RUN:
        print(f"  DRY    close sprint {sprint_id} ({name})")
        stats["ok"] += 1
        return True

    code, data = api("GET", f"/rest/agile/1.0/sprint/{sprint_id}")
    if code != 200:
        print(f"  FAIL   get sprint {sprint_id}: HTTP {code}")
        stats["fail"] += 1
        return False

    state = data.get("state", "unknown")
    if state == "closed":
        print(f"  SKIP   sprint {sprint_id} ({name}) already closed")
        stats["skip"] += 1
        return True

    if state == "future":
        code, _ = api("POST", f"/rest/agile/1.0/sprint/{sprint_id}",
                       {"state": "active"})
        if code not in (200, 204):
            code, _ = api("PUT", f"/rest/agile/1.0/sprint/{sprint_id}",
                          {"state": "active"})
        if code not in (200, 204):
            print(f"  FAIL   activate sprint {sprint_id}: HTTP {code}")
            stats["fail"] += 1
            return False
        print(f"  ACTIVE sprint {sprint_id} ({name})")
        time.sleep(0.3)

    # Close
    code, _ = api("PUT", f"/rest/agile/1.0/sprint/{sprint_id}",
                   {"state": "closed"})
    if code not in (200, 204):
        code, _ = api("POST", f"/rest/agile/1.0/sprint/{sprint_id}",
                       {"state": "closed"})
    if code in (200, 204):
        print(f"  CLOSED sprint {sprint_id} ({name})")
        stats["ok"] += 1
        return True
    print(f"  FAIL   close sprint {sprint_id}: HTTP {code}")
    stats["fail"] += 1
    return False


def banner(num, title):
    print(f"\n{'=' * 65}")
    print(f"  {num}. {title}  [{MODE}]")
    print(f"{'=' * 65}")


def search_issues(jql, fields="key,status,summary", max_results=200):
    """Search for issues using JQL."""
    code, data = api("POST", "/rest/api/3/search/jql", {
        "jql": jql,
        "fields": fields.split(","),
        "maxResults": max_results,
    })
    if code == 200:
        return data.get("issues", [])
    print(f"  WARN  JQL search failed (HTTP {code}): {jql[:80]}")
    return []


print(f"\n{'#' * 65}")
print(f"  BreadCost â€” Jira Alignment Script  [{MODE}]")
print(f"{'#' * 65}")
print(f"  Project: {JIRA_PROJECT}")
print(f"  Base URL: {JIRA_BASE_URL}")

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  1. DELETE DUPLICATE EPICS (BC-137..BC-160)
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(1, "DELETE DUPLICATE EPICS BC-137..BC-160")

DUPLICATE_EPICS = [f"BC-{n}" for n in range(137, 161)]
for key in DUPLICATE_EPICS:
    if DRY_RUN:
        print(f"  DRY    delete {key}")
        stats["ok"] += 1
    else:
        code, _ = api("DELETE", f"/rest/api/3/issue/{key}")
        if code in (200, 204):
            print(f"  DEL    {key}")
            stats["ok"] += 1
        elif code == 404:
            print(f"  SKIP   {key} (not found)")
            stats["skip"] += 1
        else:
            print(f"  FAIL   {key} (HTTP {code})")
            stats["fail"] += 1
    time.sleep(0.1)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  2. DELETE DUPLICATE VERSION
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(2, "DELETE DUPLICATE VERSION (R2 id=10036)")

if DRY_RUN:
    print(f"  DRY    delete version 10036")
    stats["ok"] += 1
else:
    # moveFixIssuesTo: move any linked issues to the real R2 version (10001)
    code, _ = api("DELETE", f"/rest/api/3/version/10036?moveFixIssuesTo=10001")
    if code in (200, 204):
        print(f"  DEL    version 10036")
        stats["ok"] += 1
    elif code == 404:
        print(f"  SKIP   version 10036 (not found)")
        stats["skip"] += 1
    else:
        print(f"  FAIL   delete version 10036 (HTTP {code})")
        stats["fail"] += 1

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  3. DELETE ORPHAN SPRINT
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(3, "DELETE ORPHAN SPRINT (BC Sprint 1, id=3)")

if DRY_RUN:
    print(f"  DRY    delete sprint 3")
    stats["ok"] += 1
else:
    code, _ = api("DELETE", f"/rest/agile/1.0/sprint/3")
    if code in (200, 204):
        print(f"  DEL    sprint 3")
        stats["ok"] += 1
    elif code == 404:
        print(f"  SKIP   sprint 3 (not found)")
        stats["skip"] += 1
    else:
        print(f"  FAIL   delete sprint 3 (HTTP {code})")
        stats["fail"] += 1

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  4. TRANSITION ORIGINAL EPICS BC-1..BC-24 â†’ Done
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(4, "TRANSITION EPICS BC-1..BC-24 â†’ Done")

ORIGINAL_EPICS = [f"BC-{n}" for n in range(1, 25)]
for key in ORIGINAL_EPICS:
    status = check_issue_status(key) if not DRY_RUN else "To Do"
    if not DRY_RUN and status == "Done":
        print(f"  SKIP   {key} (already Done)")
        stats["skip"] += 1
        continue
    if DRY_RUN:
        print(f"  DRY    {key} â†’ Done")
        stats["ok"] += 1
    else:
        if transition_to_done(key):
            print(f"  DONE   {key}")
            stats["ok"] += 1
        else:
            print(f"  FAIL   {key}")
            stats["fail"] += 1
    time.sleep(0.15)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  5. TRANSITION R1.5 EPICS BC-218..BC-221 â†’ Done
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(5, "TRANSITION R1.5 EPICS BC-218..BC-221 â†’ Done")

R15_EPICS = [f"BC-{n}" for n in range(218, 222)]
for key in R15_EPICS:
    status = check_issue_status(key) if not DRY_RUN else "To Do"
    if not DRY_RUN and status == "Done":
        print(f"  SKIP   {key} (already Done)")
        stats["skip"] += 1
        continue
    if DRY_RUN:
        print(f"  DRY    {key} â†’ Done")
        stats["ok"] += 1
    else:
        if transition_to_done(key):
            print(f"  DONE   {key}")
            stats["ok"] += 1
        else:
            print(f"  FAIL   {key}")
            stats["fail"] += 1
    time.sleep(0.15)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  6. TRANSITION R3 STORIES BC-122..BC-136 â†’ Done
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(6, "TRANSITION R3 STORIES BC-122..BC-136 â†’ Done")

R3_STORIES = [f"BC-{n}" for n in range(122, 137)]
for key in R3_STORIES:
    status = check_issue_status(key) if not DRY_RUN else "To Do"
    if not DRY_RUN and status == "Done":
        print(f"  SKIP   {key} (already Done)")
        stats["skip"] += 1
        continue
    if DRY_RUN:
        print(f"  DRY    {key} â†’ Done")
        stats["ok"] += 1
    else:
        if transition_to_done(key):
            print(f"  DONE   {key}")
            stats["ok"] += 1
        else:
            print(f"  FAIL   {key}")
            stats["fail"] += 1
    time.sleep(0.15)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  7. FIND & TRANSITION R1.5 FE STORIES â†’ Done
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(7, "TRANSITION R1.5 FE STORIES â†’ Done")

# R1.5 stories are under fixVersion "R1.5" or in R1.5 sprints or child of R1.5 epics.
# Search by parent epics BC-218..BC-221 and by version.
r15_stories = search_issues(
    f'project = {JIRA_PROJECT} AND issuetype = Story AND '
    f'status != Done AND '
    f'(fixVersion = "R1.5 â€” Frontend E2E Completion" '
    f'OR parent in (BC-218, BC-219, BC-220, BC-221) '
    f'OR sprint in (40, 41, 42, 43))',
    max_results=50
)

if r15_stories:
    print(f"  Found {len(r15_stories)} R1.5 stories not Done:")
    for issue in r15_stories:
        key = issue["key"]
        summary = issue.get("fields", {}).get("summary", "?")
        if DRY_RUN:
            print(f"  DRY    {key} â†’ Done  ({summary[:60]})")
            stats["ok"] += 1
        else:
            if transition_to_done(key):
                print(f"  DONE   {key}  ({summary[:60]})")
                stats["ok"] += 1
            else:
                print(f"  FAIL   {key}")
                stats["fail"] += 1
        time.sleep(0.15)
else:
    print("  No R1.5 stories found that need transitioning (may already be Done)")
    # Also try a broader search for any remaining To Do stories
    remaining = search_issues(
        f'project = {JIRA_PROJECT} AND issuetype = Story AND '
        f'status = "To Do" AND key >= BC-218',
        max_results=50
    )
    if remaining:
        print(f"  Found {len(remaining)} additional To Do stories >= BC-218:")
        for issue in remaining:
            key = issue["key"]
            summary = issue.get("fields", {}).get("summary", "?")
            if DRY_RUN:
                print(f"  DRY    {key} â†’ Done  ({summary[:60]})")
                stats["ok"] += 1
            else:
                if transition_to_done(key):
                    print(f"  DONE   {key}  ({summary[:60]})")
                    stats["ok"] += 1
                else:
                    print(f"  FAIL   {key}")
                    stats["fail"] += 1
            time.sleep(0.15)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  8. SET fixVersion ON STORIES
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(8, "SET fixVersion ON ALL STORIES")

# R1 version id=10000, R2=10001, R3=10002, R1.5=10035
VERSION_ASSIGNMENTS = {
    "10000": {  # R1
        "jql": (f'project = {JIRA_PROJECT} AND issuetype = Story '
                f'AND key >= BC-25 AND key <= BC-84 '
                f'AND (fixVersion is EMPTY OR NOT fixVersion = "R1 â€” Core MVP")'),
        "name": "R1 stories BC-25..BC-84",
    },
    "10001": {  # R2
        "jql": (f'project = {JIRA_PROJECT} AND issuetype = Story '
                f'AND key >= BC-85 AND key <= BC-121 '
                f'AND (fixVersion is EMPTY OR NOT fixVersion = "R2 â€” Growth")'),
        "name": "R2 stories BC-85..BC-121",
    },
    "10002": {  # R3
        "jql": (f'project = {JIRA_PROJECT} AND issuetype = Story '
                f'AND key >= BC-122 AND key <= BC-136 '
                f'AND (fixVersion is EMPTY OR NOT fixVersion = "R3 â€” AI + Mobile")'),
        "name": "R3 stories BC-122..BC-136",
    },
}

for vid, info in VERSION_ASSIGNMENTS.items():
    issues = search_issues(info["jql"], max_results=100)
    if issues:
        print(f"  {info['name']}: {len(issues)} need fixVersion")
        for issue in issues:
            key = issue["key"]
            if DRY_RUN:
                print(f"  DRY    {key} â†’ fixVersion {vid}")
                stats["ok"] += 1
            else:
                code, _ = api("PUT", f"/rest/api/3/issue/{key}",
                              {"fields": {"fixVersions": [{"id": vid}]}})
                if code in (200, 204):
                    stats["ok"] += 1
                else:
                    print(f"  FAIL   {key} fixVersion (HTTP {code})")
                    stats["fail"] += 1
            time.sleep(0.1)
    else:
        print(f"  {info['name']}: all OK (or none found)")

# Also fix epic fixVersions
EPIC_VERSION_MAP = {
    "10000": [f"BC-{n}" for n in range(1, 12)],   # R1 epics: E00..E10 â†’ BC-1..11
    "10001": [f"BC-{n}" for n in range(12, 19)],   # R2 epics: E11..E17 â†’ BC-12..18
    "10002": [f"BC-{n}" for n in range(19, 25)],    # R3 epics: E18..E23 â†’ BC-19..24
    "10035": [f"BC-{n}" for n in range(218, 222)],  # R1.5 epics
}

print()
print("  -- Epic fixVersions --")
for vid, keys in EPIC_VERSION_MAP.items():
    for key in keys:
        if DRY_RUN:
            print(f"  DRY    {key} â†’ fixVersion {vid}")
            stats["ok"] += 1
        else:
            code, _ = api("PUT", f"/rest/api/3/issue/{key}",
                          {"fields": {"fixVersions": [{"id": vid}]}})
            if code in (200, 204):
                stats["ok"] += 1
            elif code == 404:
                print(f"  SKIP   {key} (not found)")
                stats["skip"] += 1
            else:
                print(f"  FAIL   {key} fixVersion (HTTP {code})")
                stats["fail"] += 1
        time.sleep(0.1)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  9. CLOSE ALL OPEN SPRINTS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(9, "CLOSE ALL OPEN SPRINTS")

ALL_SPRINTS = {
    5:  "Sprint 2 â€” R2 Growth",
    6:  "Sprint 3 â€” R3 AI+Mobile",
    40: "R1.5 Sprint 4 Inventory",
    41: "R1.5 Sprint 5 POS",
    42: "R1.5 Sprint 6 Admin",
    43: "R1.5 Sprint 7 Reports",
    44: "R2 Sprint 8 Portal",
    45: "R2 Sprint 9 Loyalty",
    46: "R2 Sprint 10 Suppliers",
    47: "R2 Sprint 11 Delivery",
    48: "R2 Sprint 12 Finance",
    49: "R2 Sprint 13 Reports",
}

for sid, name in ALL_SPRINTS.items():
    close_sprint(sid, name)
    time.sleep(0.2)

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
# 10. RELEASE VERSIONS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(10, "RELEASE VERSIONS")

VERSIONS_TO_RELEASE = {
    "10035": "R1.5 â€” Frontend E2E Completion",
    "10001": "R2 â€” Growth",
    "10002": "R3 â€” AI + Mobile",
}

for vid, vname in VERSIONS_TO_RELEASE.items():
    if DRY_RUN:
        print(f"  DRY    release {vname} (id={vid})")
        stats["ok"] += 1
    else:
        code, data = api("PUT", f"/rest/api/3/version/{vid}",
                         {"released": True, "releaseDate": "2026-03-14"})
        if code in (200, 204):
            print(f"  RELEASED {vname}")
            stats["ok"] += 1
        else:
            print(f"  FAIL   release {vname} (HTTP {code})")
            stats["fail"] += 1

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
# 11. CREATE R4 + R5 VERSIONS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
banner(11, "CREATE R4 & R5 VERSIONS")

NEW_VERSIONS = [
    {"name": "R4 â€” Customer Portal & Visual Refresh", "released": True,
     "releaseDate": "2026-03-14", "description": "Customer portal FE, security hardening, design system, visual rework"},
    {"name": "R5 â€” ARCMAP Gap Closure", "released": True,
     "releaseDate": "2026-03-14", "description": "POS inventory deduction, WO material checks, push notifications, stock alerts, yield tracking, invoice disputes, subscription expiry, supplier mapping"},
]

for v in NEW_VERSIONS:
    if DRY_RUN:
        print(f"  DRY    create version: {v['name']}")
        stats["ok"] += 1
    else:
        code, data = api("POST", "/rest/api/3/version", {
            "name": v["name"],
            "projectId": None,  # will be set below
            "released": v["released"],
            "releaseDate": v["releaseDate"],
            "description": v.get("description", ""),
        })
        # Need to get projectId first
        pcode, pdata = api("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
        if pcode == 200:
            project_id = pdata["id"]
            code, data = api("POST", "/rest/api/3/version", {
                "name": v["name"],
                "projectId": project_id,
                "released": v["released"],
                "releaseDate": v["releaseDate"],
                "description": v.get("description", ""),
            })
            if code in (200, 201):
                print(f"  CREATE {v['name']} (id={data.get('id', '?')})")
                stats["ok"] += 1
            elif code == 400 and "already exists" in str(data).lower():
                print(f"  SKIP   {v['name']} (already exists)")
                stats["skip"] += 1
            else:
                print(f"  FAIL   {v['name']} (HTTP {code}: {data})")
                stats["fail"] += 1
        else:
            print(f"  FAIL   cannot get project ID")
            stats["fail"] += 1

# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  SUMMARY
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
print(f"\n{'#' * 65}")
print(f"  SUMMARY  [{MODE}]")
print(f"{'#' * 65}")
print(f"  OK:   {stats['ok']}")
print(f"  SKIP: {stats['skip']}")
print(f"  FAIL: {stats['fail']}")

if DRY_RUN:
    print(f"\n  âš  This was a DRY RUN. To apply changes, re-run with --run")
    print(f"    python align_jira.py --run")
print()

