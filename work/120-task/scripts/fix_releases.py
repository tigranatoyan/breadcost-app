#!/usr/bin/env python3
"""fix_releases.py — Fix Jira release configuration:
1. Close Sprint 4 & 5 (all stories Done)
2. Start Sprint 6 (next active R1.5 sprint)
3. Verify fixVersion on Sprint 6/7 stories
4. Set Sprint 4/5 epics fixVersion if missing
"""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json", "Content-Type": "application/json"}

VERSION_ID = "10035"  # R1.5 — Frontend E2E Completion

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

def jira_search(jql, fields, max_results=50):
    body = {"jql": jql, "maxResults": max_results, "fields": fields}
    s, data = jira("POST", "/rest/api/3/search/jql", body)
    return data.get("issues", []) if s == 200 else []

def main():
    print("=" * 60)
    print(f"  {'DRY RUN' if DRY_RUN else 'LIVE RUN'} — Fix Jira Releases & Sprints")
    print("=" * 60)

    # ── 1. Close Sprint 4 (id=40) and Sprint 5 (id=41) ──────
    print("\n── Step 1: Close completed sprints ──")
    for sprint_id, name in [(40, "R1.5 Sprint 4 Inventory"), (41, "R1.5 Sprint 5 POS")]:
        if DRY_RUN:
            print(f"  DRY  close sprint {sprint_id} ({name})")
        else:
            # Must transition: future → active → closed
            # First activate
            s, d = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}", {"state": "active"})
            if s in (200, 204):
                print(f"  OK   activated sprint {sprint_id}")
            else:
                print(f"  WARN activate {sprint_id}: {s} — {str(d)[:80]}")
            # Then close
            s, d = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}", {"state": "closed"})
            if s in (200, 204):
                print(f"  OK   closed sprint {sprint_id} ({name})")
            else:
                print(f"  WARN close {sprint_id}: {s} — {str(d)[:80]}")

    # ── 2. Start Sprint 6 (id=42) ───────────────────────────
    print("\n── Step 2: Activate Sprint 6 ──")
    if DRY_RUN:
        print(f"  DRY  activate sprint 42 (R1.5 Sprint 6 Admin)")
    else:
        s, d = jira("POST", "/rest/agile/1.0/sprint/42", {"state": "active"})
        if s in (200, 204):
            print(f"  OK   activated sprint 42")
        else:
            print(f"  WARN activate 42: {s} — {str(d)[:80]}")

    # ── 3. Ensure fixVersion on Sprint 6/7 stories ──────────
    print("\n── Step 3: Fix missing fixVersions on Sprint 6/7 stories ──")
    for sprint_id, label in [(42, "Sprint 6"), (43, "Sprint 7")]:
        issues = jira_search(
            f"project={JIRA_PROJECT} AND sprint={sprint_id} AND fixVersion is EMPTY AND issuetype=Story",
            ["summary", "fixVersions"]
        )
        if not issues:
            print(f"  {label}: all stories have fixVersion ✓")
        for iss in issues:
            key = iss["key"]
            if DRY_RUN:
                print(f"  DRY  set fixVersion on {key}: {iss['fields']['summary'][:50]}")
            else:
                s, d = jira("PUT", f"/rest/api/3/issue/{key}", {"fields": {"fixVersions": [{"id": VERSION_ID}]}})
                if s in (200, 204):
                    print(f"  OK   {key} fixVersion set")
                else:
                    print(f"  FAIL {key}: {s}")

    # ── 4. Ensure fixVersion on epics ────────────────────────
    print("\n── Step 4: Fix fixVersion on R1.5 epics ──")
    epics = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Epic AND summary ~ 'R1.5'",
        ["summary", "fixVersions"]
    )
    for ep in epics:
        key = ep["key"]
        fv = ep["fields"].get("fixVersions", [])
        if any(v.get("id") == VERSION_ID or v.get("name", "").startswith("R1.5") for v in fv):
            print(f"  {key}: already has R1.5 fixVersion ✓")
        else:
            if DRY_RUN:
                print(f"  DRY  set fixVersion on {key}: {ep['fields']['summary'][:50]}")
            else:
                s, d = jira("PUT", f"/rest/api/3/issue/{key}", {"fields": {"fixVersions": [{"id": VERSION_ID}]}})
                if s in (200, 204):
                    print(f"  OK   {key} fixVersion set")
                else:
                    print(f"  FAIL {key}: {s}")

    print(f"\n{'='*60}\n  COMPLETE{' (dry run)' if DRY_RUN else ''}\n{'='*60}")

if __name__ == "__main__":
    main()
