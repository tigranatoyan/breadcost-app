#!/usr/bin/env python3
"""Mark R1.5 Sprint 4 stories + epic as Done in JIRA."""
import urllib.request, urllib.error, json, base64, sys
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv

_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {
    "Authorization": "Basic " + _TOKEN,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

def jira(method, path, body=None):
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=_HDRS, method=method)
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

# Sprint 4 story titles (unique substrings to match R1.5 not R2)
SPRINT4_TITLES = [
    "FE /inventory — Adjustment modal",
    "FE /inventory — Lot detail expand",
    "FE /inventory — Department/site filter",
    "FE /inventory — Receive Lot: currency",
    "FE /dashboard — Stock alert widget",
]
SPRINT4_EPIC_TITLE = "[BC-E25] R1.5"

print("=" * 60)
print("  BreadCost — Mark Sprint 4 as Done")
print(f"  Mode: {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
print("=" * 60)

# Find all project issues
issues = []
next_page = None
while True:
    body = {
        "jql": f"project={JIRA_PROJECT} AND (issuetype=Story OR issuetype=Epic)",
        "maxResults": 100,
        "fields": ["summary", "status"],
    }
    if next_page:
        body["nextPageToken"] = next_page
    s, data = jira("POST", "/rest/api/3/search/jql", body)
    if s != 200:
        print(f"Search failed: {s}")
        sys.exit(1)
    issues.extend(data.get("issues", []))
    next_page = data.get("nextPageToken")
    if not next_page:
        break

print(f"\nFound {len(issues)} issues total")

# Get Done transition ID
sample = issues[0]["key"]
_, trans_data = jira("GET", f"/rest/api/3/issue/{sample}/transitions")
transitions = trans_data.get("transitions", [])
done_id = next((t["id"] for t in transitions if "done" in t["name"].lower()), None)
if not done_id:
    print("ERROR: no Done transition found")
    sys.exit(1)
print(f"Done transition id={done_id}\n")

# Match and transition
updated = 0
for issue in issues:
    key = issue["key"]
    summary = issue["fields"]["summary"]
    status = issue["fields"]["status"]["name"]

    matched = any(t in summary for t in SPRINT4_TITLES) or summary.startswith(SPRINT4_EPIC_TITLE)
    if not matched:
        continue

    if status.lower() == "done":
        print(f"  SKIP  {key}  (already Done)  {summary[:55]}")
        continue

    if DRY_RUN:
        print(f"  DRY   {key}  {status} → Done  {summary[:55]}")
        updated += 1
        continue

    s2, _ = jira("POST", f"/rest/api/3/issue/{key}/transitions", {"transition": {"id": done_id}})
    if s2 in (200, 204):
        print(f"  DONE  {key}  ✓  {summary[:55]}")
        updated += 1
    else:
        print(f"  FAIL  {key}  HTTP {s2}  {summary[:55]}")

print(f"\n{'Would update' if DRY_RUN else 'Updated'}: {updated} issues")
if DRY_RUN:
    print("⚠  Dry run. Use --run to apply.")
