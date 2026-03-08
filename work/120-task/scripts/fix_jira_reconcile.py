#!/usr/bin/env python3
"""
fix_jira_reconcile.py — Reconcile Jira state with actual codebase
=================================================================
Fixes:
  1. Revert 37 R2 stories (BC-85..121) from Done → To Do (no R2 code exists)
  2. Delete duplicate "Done" epics (BC-137..160) — originals BC-1..24 are kept
  3. Assign R2 stories to their proper sprints

Run:
    python fix_jira_reconcile.py          # dry-run
    python fix_jira_reconcile.py --run    # live
"""
import sys, json, base64, urllib.request, urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

DRY_RUN = "--run" not in sys.argv

_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {
    "Authorization": "Basic " + _TOKEN,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

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
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode()[:300]}


# ── R2 story keys to revert ─────────────────────────────────
R2_STORY_KEYS = [
    f"BC-{i}" for i in range(85, 122)  # BC-85 through BC-121
]

# ── Duplicate epic keys to delete ────────────────────────────
DUPLICATE_EPIC_KEYS = [
    f"BC-{i}" for i in range(137, 161)  # BC-137 through BC-160
]


def get_transition_id(issue_key, target_name):
    """Get the transition ID to move an issue to a given status."""
    s, data = jira("GET", f"/rest/api/3/issue/{issue_key}/transitions")
    if s != 200:
        return None
    for t in data.get("transitions", []):
        if target_name.lower() in t["name"].lower():
            return t["id"]
    return None


def revert_r2_stories():
    """Revert R2 stories from Done to To Do."""
    print(f"\n── Revert {len(R2_STORY_KEYS)} R2 stories: Done → To Do ────")
    reverted = 0
    for key in R2_STORY_KEYS:
        # Check current status
        s, data = jira("GET", f"/rest/api/3/issue/{key}?fields=status,summary")
        if s != 200:
            print(f"  SKIP {key}: not found ({s})")
            continue
        current = data["fields"]["status"]["name"]
        summary = data["fields"]["summary"][:50]
        if current == "To Do":
            print(f"  SKIP {key}: already To Do — {summary}")
            continue

        if DRY_RUN:
            print(f"  DRY  {key}: {current} → To Do — {summary}")
            reverted += 1
            continue

        # Get transition to "To Do"
        tid = get_transition_id(key, "To Do")
        if not tid:
            # Try "Reopen" or "Backlog" as fallback
            tid = get_transition_id(key, "Reopen")
        if not tid:
            tid = get_transition_id(key, "Backlog")
        if not tid:
            print(f"  FAIL {key}: no transition to To Do found")
            # List available transitions
            s2, t2 = jira("GET", f"/rest/api/3/issue/{key}/transitions")
            if s2 == 200:
                print(f"         Available: {[t['name'] for t in t2.get('transitions', [])]}")
            continue

        s2, _ = jira("POST", f"/rest/api/3/issue/{key}/transitions", {"transition": {"id": tid}})
        if s2 in (200, 204):
            print(f"  DONE {key}: {current} → To Do — {summary}")
            reverted += 1
        else:
            print(f"  FAIL {key}: transition failed ({s2})")
    print(f"  Total reverted: {reverted}")


def delete_duplicate_epics():
    """Delete the duplicate Done epics (BC-137..160)."""
    print(f"\n── Delete {len(DUPLICATE_EPIC_KEYS)} duplicate epics ────────")
    deleted = 0
    for key in DUPLICATE_EPIC_KEYS:
        s, data = jira("GET", f"/rest/api/3/issue/{key}?fields=summary,status")
        if s != 200:
            print(f"  SKIP {key}: not found ({s})")
            continue
        summary = data["fields"]["summary"][:50]
        status = data["fields"]["status"]["name"]

        if DRY_RUN:
            print(f"  DRY  DELETE {key}: [{status}] {summary}")
            deleted += 1
            continue

        s2, _ = jira("DELETE", f"/rest/api/3/issue/{key}")
        if s2 in (200, 204):
            print(f"  DELETED {key}: [{status}] {summary}")
            deleted += 1
        else:
            print(f"  FAIL {key}: delete failed ({s2})")
    print(f"  Total deleted: {deleted}")


def main():
    print("=" * 64)
    print("  BreadCost — Jira Reconciliation Fix")
    print(f"  Mode: {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
    print("=" * 64)
    print("\n  Actions:")
    print(f"    1. Revert {len(R2_STORY_KEYS)} R2 stories from Done → To Do")
    print(f"    2. Delete {len(DUPLICATE_EPIC_KEYS)} duplicate epics (BC-137..160)")

    if not DRY_RUN:
        confirm = input("\nType YES to proceed: ").strip()
        if confirm != "YES":
            print("Aborted.")
            sys.exit(0)

    revert_r2_stories()
    delete_duplicate_epics()

    print("\n" + "=" * 64)
    print("  DONE")
    print("=" * 64)
    if DRY_RUN:
        print("  ⚠  DRY RUN. Run with --run to apply changes.")


if __name__ == "__main__":
    main()
