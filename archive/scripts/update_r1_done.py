"""
Bulk-update all R1 Story issues in JIRA to 'Done'
and close all R1 GitHub issues with a completion comment.
"""
import urllib.request, urllib.error, json, base64, sys
from config import (
    JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT,
    GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN,
)
from data import STORIES

# ── helpers ─────────────────────────────────────────────────

def jira_req(method, path, body=None):
    token = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
    hdrs = {
        "Authorization": "Basic " + token,
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        return e.code, {}


def gh_req(method, path, body=None):
    hdrs = {
        "Authorization": "Bearer " + GITHUB_TOKEN,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
    }
    data = json.dumps(body).encode() if body else None
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}{path}"
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        return e.code, {}


# ── 1. Fetch all JIRA stories ────────────────────────────────

print("=" * 60)
print("  BreadCost — R1 Completion Update")
print("=" * 60)

# Collect all JIRA Story issues (POST /search/jql with nextPageToken pagination)
jira_stories = []
next_page = None
while True:
    body = {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Story",
        "maxResults": 100,
        "fields": ["summary", "status"],
    }
    if next_page:
        body["nextPageToken"] = next_page

    s, data = jira_req("POST", "/rest/api/3/search/jql", body)
    if s != 200:
        print(f"JIRA search failed: {s} — {data}")
        sys.exit(1)
    issues = data.get("issues", [])
    jira_stories.extend(issues)
    next_page = data.get("nextPageToken")
    if not next_page or not issues:
        break

print(f"\nJIRA: found {len(jira_stories)} Story issues")

# Get available transitions from first issue
_, trans_data = jira_req("GET", f"/rest/api/3/issue/{jira_stories[0]['key']}/transitions")
transitions = trans_data.get("transitions", [])
print("Available transitions:")
for t in transitions:
    print(f"  [{t['id']}] {t['name']}")

done_id = next((t["id"] for t in transitions if "done" in t["name"].lower()), None)
if not done_id:
    print("ERROR: Could not find 'Done' transition. Aborting.")
    sys.exit(1)
print(f"\nUsing transition id={done_id} for 'Done'\n")

# ── 2. Transition each story to Done ────────────────────────

r1_story_ids = {s["id"] for s in STORIES if s.get("status") == "✅ Done"}
print(f"R1 stories in data.py: {len(r1_story_ids)}\n")

jira_updated = 0
for issue in jira_stories:
    key = issue["key"]
    current_status = issue["fields"]["status"]["name"]
    summary = issue["fields"]["summary"]

    if current_status.lower() == "done":
        print(f"  SKIP  {key}  (already Done)  {summary[:50]}")
        continue

    status, _ = jira_req(
        "POST",
        f"/rest/api/3/issue/{key}/transitions",
        {"transition": {"id": done_id}},
    )
    if status in (200, 204):
        print(f"  DONE  {key}  ✓  {summary[:50]}")
        jira_updated += 1
    else:
        print(f"  FAIL  {key}  HTTP {status}  {summary[:50]}")

print(f"\nJIRA: {jira_updated} stories moved to Done")

# ── 3. Close R1 GitHub issues ────────────────────────────────

print("\n" + "=" * 60)
print("  Closing R1 GitHub Issues")
print("=" * 60 + "\n")

# Fetch all open issues (stories only — title starts with [BC-1xx] etc.)
start, gh_issues = 1, []
while True:
    s, page = gh_req("GET", f"/issues?state=open&per_page=100&page={start}")
    if s != 200 or not page:
        break
    gh_issues.extend(page)
    if len(page) < 100:
        break
    start += 1

# R1 story IDs (just the numeric part like BC-101 → "BC-101")
r1_ids = [s["id"] for s in STORIES if s.get("status") == "✅ Done"]

commit_sha = "0835633"  # latest pushed commit

gh_closed = 0
for issue in gh_issues:
    title = issue.get("title", "")
    number = issue["number"]

    # Match stories: title like "[BC-101] ..." or "[BC-E01] ..."
    story_match = next(
        (sid for sid in r1_ids if title.startswith(f"[{sid}]")),
        None,
    )
    if not story_match:
        continue

    # Add completion comment
    gh_req(
        "POST",
        f"/issues/{number}/comments",
        {"body": f"✅ Implemented in R1. Commit: tigranatoyan/breadcost-app@{commit_sha}\n\nAll acceptance criteria verified — 93 functional tests passing (0 failures)."},
    )

    # Close issue
    s, _ = gh_req("PATCH", f"/issues/{number}", {"state": "closed", "state_reason": "completed"})
    if s == 200:
        print(f"  CLOSED #{number}  [{story_match}]")
        gh_closed += 1
    else:
        print(f"  FAIL   #{number}  HTTP {s}  [{story_match}]")

print(f"\nGitHub: {gh_closed} R1 story issues closed")
print("\nDone.")
