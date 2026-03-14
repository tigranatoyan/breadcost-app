#!/usr/bin/env python3
"""deep_audit.py — Full Jira state audit for cleanup planning."""
import json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json", "Content-Type": "application/json"}

def get(path):
    req = urllib.request.Request(JIRA_BASE_URL + path, headers=_HDRS)
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        return {"error": e.code, "body": e.read().decode()[:200]}

def search(jql, fields, max_results=100):
    body = json.dumps({"jql": jql, "maxResults": max_results, "fields": fields}).encode()
    req = urllib.request.Request(JIRA_BASE_URL + "/rest/api/3/search/jql", data=body, headers=_HDRS, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read()).get("issues", [])
    except urllib.error.HTTPError as e:
        print(f"  SEARCH ERROR: {e.code} {e.read().decode()[:200]}")
        return []

# ══════════════════════════════════════════════════════════════
print("=" * 70)
print("  DEEP JIRA AUDIT")
print("=" * 70)

# ── 1. VERSIONS ──────────────────────────────────────────────
print("\n══ 1. RELEASES (Fix Versions) ══")
versions = get(f"/rest/api/3/project/{JIRA_PROJECT}/versions")
for v in versions:
    released = "RELEASED" if v.get("released") else "unreleased"
    date = v.get("releaseDate", "no date")
    # Count issues using this version
    issues = search(f'project={JIRA_PROJECT} AND fixVersion="{v["name"]}"', ["key"], 200)
    print(f"  id={v['id']:6s}  {released:11s}  {date:12s}  stories={len(issues):3d}  {v['name']}")

# ── 2. SPRINTS + STORIES IN EACH ─────────────────────────────
print("\n══ 2. SPRINTS (with story counts & details) ══")
sprints = get(f"/rest/agile/1.0/board/2/sprint?maxResults=50")
for sp in sprints.get("values", []):
    sid = sp["id"]
    state = sp["state"]
    name = sp["name"]
    # Get stories in this sprint
    issues = search(
        f"project={JIRA_PROJECT} AND sprint={sid}",
        ["summary", "status", "fixVersions", "issuetype", "labels"],
        50
    )
    print(f"\n  sprint id={sid:4d}  {state:8s}  {name}")
    print(f"  {'─'*60}")
    if not issues:
        print(f"    (empty)")
    for iss in issues:
        f = iss["fields"]
        itype = f.get("issuetype", {}).get("name", "?")
        status = f.get("status", {}).get("name", "?")
        fv = ", ".join(v["name"] for v in f.get("fixVersions", [])) or "NONE"
        labels = ", ".join(f.get("labels", [])) or ""
        summ = f.get("summary", "")[:55]
        print(f"    {iss['key']:8s} {itype:6s} {status:12s} fv=[{fv}]  {summ}")

# ── 3. EPICS ─────────────────────────────────────────────────
print("\n══ 3. EPICS ══")
epics = search(f"project={JIRA_PROJECT} AND issuetype=Epic ORDER BY key ASC", 
               ["summary", "status", "fixVersions", "labels"], 50)
for ep in epics:
    f = ep["fields"]
    status = f.get("status", {}).get("name", "?")
    fv = ", ".join(v["name"] for v in f.get("fixVersions", [])) or "NONE"
    labels = ", ".join(f.get("labels", [])) or ""
    print(f"  {ep['key']:8s} {status:12s} fv=[{fv}]  labels=[{labels}]  {f['summary'][:50]}")

# ── 4. ORPHAN STORIES (no sprint) ────────────────────────────
print("\n══ 4. STORIES WITHOUT SPRINT ══")
orphans = search(f"project={JIRA_PROJECT} AND issuetype=Story AND sprint is EMPTY", 
                 ["summary", "status", "fixVersions"], 100)
if not orphans:
    print("  (none)")
for o in orphans:
    f = o["fields"]
    status = f.get("status", {}).get("name", "?")
    fv = ", ".join(v["name"] for v in f.get("fixVersions", [])) or "NONE"
    print(f"  {o['key']:8s} {status:12s} fv=[{fv}]  {f['summary'][:55]}")

# ── 5. STORIES WITHOUT FIX VERSION ───────────────────────────
print("\n══ 5. STORIES WITHOUT FIX VERSION ══")
no_fv = search(f"project={JIRA_PROJECT} AND issuetype=Story AND fixVersion is EMPTY",
               ["summary", "status", "sprint"], 100)
if not no_fv:
    print("  (none)")
for o in no_fv:
    f = o["fields"]
    status = f.get("status", {}).get("name", "?")
    sprint_name = ""
    # sprint field might be a list
    sp = f.get("sprint")
    if sp:
        sprint_name = sp.get("name", "?")
    print(f"  {o['key']:8s} {status:12s} sprint=[{sprint_name}]  {f['summary'][:55]}")

print("\n" + "=" * 70)
print("  AUDIT COMPLETE")
print("=" * 70)
