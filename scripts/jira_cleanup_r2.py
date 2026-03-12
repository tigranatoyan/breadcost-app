#!/usr/bin/env python3
"""
jira_cleanup_r2.py — Jira cleanup: transition R2 stories + epics to Done,
delete duplicate version, transition R1.5 epics to Done.

Actions:
  1. Transition BC-88..BC-121 (34 R2 stories currently "To Do") → Done
  2. Transition R2 epics (BC-12..BC-24) → Done
  3. Transition R1.5 epics (BC-220, BC-221) → Done
  4. Delete duplicate version "R2 — Growth (DUPLICATE — archived)" id=10036

Usage:
    python jira_cleanup_r2.py          # dry run
    python jira_cleanup_r2.py --run    # live
"""
import sys, json, base64, urllib.request, urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

DRY_RUN = "--run" not in sys.argv

_TOKEN = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
_HDRS = {
    "Authorization": f"Basic {_TOKEN}",
    "Accept": "application/json",
    "Content-Type": "application/json",
}


def api(method, path, body=None):
    url = JIRA_BASE_URL + "/rest/api/3" + path
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
    """Return {name_lower: id} map of available transitions for an issue."""
    code, data = api("GET", f"/issue/{key}/transitions")
    if code != 200:
        print(f"    WARN: cannot get transitions for {key}: HTTP {code}")
        return {}
    return {t["name"].lower(): t["id"] for t in data.get("transitions", [])}


def transition_to_done(key):
    """Transition issue to Done. Handles two-step workflow (To Do → In Progress → Done)."""
    transitions = get_transitions(key)
    done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")

    if done_id:
        code, _ = api("POST", f"/issue/{key}/transitions", {"transition": {"id": done_id}})
        return code == 204 or code == 200

    # Try two-step: To Do → In Progress → Done
    ip_id = transitions.get("in progress") or transitions.get("start progress")
    if ip_id:
        api("POST", f"/issue/{key}/transitions", {"transition": {"id": ip_id}})
        transitions = get_transitions(key)
        done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
        if done_id:
            code, _ = api("POST", f"/issue/{key}/transitions", {"transition": {"id": done_id}})
            return code == 204 or code == 200

    print(f"    WARN: no Done transition found for {key}, available: {list(transitions.keys())}")
    return False


# ═══════════════════════════════════════════════════════════
#  1. Transition R2 stories BC-88..BC-121 to Done
# ═══════════════════════════════════════════════════════════
story_keys = [f"BC-{n}" for n in range(88, 122)]
mode = "DRY RUN" if DRY_RUN else "LIVE"
print(f"\n{'=' * 60}")
print(f"1. TRANSITION R2 STORIES TO DONE  [{mode}]")
print(f"   {story_keys[0]}..{story_keys[-1]}  ({len(story_keys)} stories)")
print(f"{'=' * 60}")

ok, fail = 0, 0
for key in story_keys:
    if DRY_RUN:
        print(f"  DRY  {key} → Done")
        ok += 1
    else:
        if transition_to_done(key):
            print(f"  DONE  {key}")
            ok += 1
        else:
            print(f"  FAIL  {key}")
            fail += 1
print(f"  Result: {ok} ok, {fail} failed\n")

# ═══════════════════════════════════════════════════════════
#  2. Transition R2 epics BC-12..BC-24 to Done
# ═══════════════════════════════════════════════════════════
epic_keys = [f"BC-{n}" for n in range(12, 25)]
print(f"{'=' * 60}")
print(f"2. TRANSITION R2 EPICS TO DONE  [{mode}]")
print(f"   {epic_keys[0]}..{epic_keys[-1]}  ({len(epic_keys)} epics)")
print(f"{'=' * 60}")

ok, fail = 0, 0
for key in epic_keys:
    if DRY_RUN:
        print(f"  DRY  {key} → Done")
        ok += 1
    else:
        if transition_to_done(key):
            print(f"  DONE  {key}")
            ok += 1
        else:
            print(f"  FAIL  {key}")
            fail += 1
print(f"  Result: {ok} ok, {fail} failed\n")

# ═══════════════════════════════════════════════════════════
#  3. Transition R1.5 epics BC-220, BC-221 to Done
# ═══════════════════════════════════════════════════════════
r15_keys = ["BC-220", "BC-221"]
print(f"{'=' * 60}")
print(f"3. TRANSITION R1.5 EPICS TO DONE  [{mode}]")
print(f"   {', '.join(r15_keys)}")
print(f"{'=' * 60}")

ok, fail = 0, 0
for key in r15_keys:
    if DRY_RUN:
        print(f"  DRY  {key} → Done")
        ok += 1
    else:
        if transition_to_done(key):
            print(f"  DONE  {key}")
            ok += 1
        else:
            print(f"  FAIL  {key}")
            fail += 1
print(f"  Result: {ok} ok, {fail} failed\n")

# ═══════════════════════════════════════════════════════════
#  4. Delete duplicate version id=10036
# ═══════════════════════════════════════════════════════════
DUP_VERSION_ID = "10036"
print(f"{'=' * 60}")
print(f"4. DELETE DUPLICATE VERSION  [{mode}]")
print(f"   Version id={DUP_VERSION_ID}")
print(f"{'=' * 60}")

if DRY_RUN:
    print(f"  DRY  DELETE version {DUP_VERSION_ID}")
else:
    code, data = api("DELETE", f"/version/{DUP_VERSION_ID}")
    if code in (200, 204):
        print(f"  DELETED  version {DUP_VERSION_ID}")
    else:
        print(f"  FAIL  HTTP {code}: {data}")

print(f"\n{'=' * 60}")
print(f"CLEANUP COMPLETE  [{mode}]")
print(f"{'=' * 60}\n")

if DRY_RUN:
    print("  Re-run with --run to execute changes.")
