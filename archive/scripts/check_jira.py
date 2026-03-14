"""Check Jira project state: versions, sprints, epics, story counts."""
import urllib.request, urllib.parse, json, base64, ssl
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

ctx = ssl.create_default_context()
auth = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
HDR = {"Authorization": f"Basic {auth}", "Content-Type": "application/json"}


def api_get(path):
    req = urllib.request.Request(f"{JIRA_BASE_URL}{path}", headers=HDR)
    return json.loads(urllib.request.urlopen(req, context=ctx).read())


def jira_search(jql, fields="key,summary,status", max_results=200):
    if max_results < 1:
        max_results = 1
    body = json.dumps({"jql": jql, "maxResults": max_results, "fields": fields.split(",")}).encode()
    req = urllib.request.Request(f"{JIRA_BASE_URL}/rest/api/3/search/jql", data=body, headers=HDR, method="POST")
    try:
        return json.loads(urllib.request.urlopen(req, context=ctx).read())
    except urllib.error.HTTPError as e:
        print(f"  [ERROR] {e.code} for JQL: {jql}")
        try:
            print(f"  [BODY] {e.read().decode()[:300]}")
        except Exception:
            pass
        return {"issues": [], "total": 0}


# ── Versions ─────────────────────────────────────────────
versions = api_get(f"/rest/api/3/project/{JIRA_PROJECT}/versions")
print("=" * 60)
print("VERSIONS")
print("=" * 60)
for v in versions:
    rel = "RELEASED" if v.get("released") else "unreleased"
    print(f"  {v['name']:<40} id={v['id']:<6} {rel}")

# ── Board + Sprints ──────────────────────────────────────
boards = api_get(f"/rest/agile/1.0/board?projectKeyOrId={JIRA_PROJECT}")
board_id = boards["values"][0]["id"]
sprints = api_get(f"/rest/agile/1.0/board/{board_id}/sprint?maxResults=50")
print()
print("=" * 60)
print(f"SPRINTS  (board {board_id})")
print("=" * 60)
for s in sprints["values"]:
    print(f"  {s['name']:<35} id={s['id']:<4} state={s['state']}")

# ── Epics ────────────────────────────────────────────────
jql = f"project={JIRA_PROJECT} AND issuetype=Epic ORDER BY key ASC"
epics = jira_search(jql, "key,summary,status,fixVersions", 100)
print()
print("=" * 60)
print("EPICS")
print("=" * 60)
for issue in epics.get("issues", []):
    key = issue["key"]
    summary = issue["fields"]["summary"]
    status = issue["fields"]["status"]["name"]
    fv = ", ".join(v["name"] for v in issue["fields"].get("fixVersions", []))
    print(f"  {key:<10} {status:<15} {fv:<30} {summary}")

# ── Story counts by version ──────────────────────────────
print()
print("=" * 60)
print("STORY COUNTS BY VERSION")
print("=" * 60)
for v in versions:
    vname = v["name"]
    vid = v["id"]
    jql2 = f'project={JIRA_PROJECT} AND issuetype=Story AND fixVersion={vid}'
    result = jira_search(jql2, "key", 1)
    total = result.get("total", 0)
    print(f"  {vname:<40} {total} stories")

# ── Story counts by status (overall) ────────────────────
jql3 = f"project={JIRA_PROJECT} AND issuetype=Story"
all_stories = jira_search(jql3, "key", 1)
print()
print(f"TOTAL STORIES: {all_stories.get('total', 0)}")

print()
print("=" * 60)
print("STORIES BY STATUS")
print("=" * 60)
for status_name in ["To Do", "In Progress", "Done"]:
    jql4 = f'project={JIRA_PROJECT} AND issuetype=Story AND status="{status_name}"'
    r = jira_search(jql4, "key", 1)
    print(f"  {status_name:<20} {r.get('total', 0)}")

# ── R1 stories detail ───────────────────────────────────
print()
print("=" * 60)
print("R1 STORIES (original version) — STATUS CHECK")
print("=" * 60)
for v in versions:
    vname = v["name"]
    vid = v["id"]
    if "R1" not in vname or "R1.5" in vname:
        continue
    jql5 = f'project={JIRA_PROJECT} AND issuetype=Story AND fixVersion={vid}'
    r = jira_search(jql5, "key,summary,status", 200)
    done_count = sum(1 for i in r.get("issues", []) if i["fields"]["status"]["name"] == "Done")
    total = r.get("total", 0)
    not_done = [i for i in r.get("issues", []) if i["fields"]["status"]["name"] != "Done"]
    print(f"  {vname}: {done_count}/{total} Done")
    for nd in not_done:
        print(f"    NOT DONE: {nd['key']} [{nd['fields']['status']['name']}] {nd['fields']['summary']}")

# ── R1.5 stories detail ──────────────────────────────────
print()
print("=" * 60)
print("R1.5 STORIES — STATUS CHECK")
print("=" * 60)
for v in versions:
    vname = v["name"]
    if "R1.5" not in vname:
        continue
    jql_r15 = f'project={JIRA_PROJECT} AND issuetype=Story AND fixVersion={v["id"]}'
    r = jira_search(jql_r15, "key,summary,status,sprint", 200)
    total = r.get("total", 0)
    by_status = {}
    for i in r.get("issues", []):
        st = i["fields"]["status"]["name"]
        by_status[st] = by_status.get(st, 0) + 1
    print(f"  {vname}: {total} stories — {by_status}")

# ── R2 stories detail ───────────────────────────────────
print()
print("=" * 60)
print("R2 STORIES — STATUS CHECK")
print("=" * 60)
for v in versions:
    vname = v["name"]
    if "R2" not in vname:
        continue
    jql6 = f'project={JIRA_PROJECT} AND issuetype=Story AND fixVersion={v["id"]}'
    r = jira_search(jql6, "key,summary,status,sprint", 200)
    total = r.get("total", 0)
    by_status = {}
    for i in r.get("issues", []):
        st = i["fields"]["status"]["name"]
        by_status[st] = by_status.get(st, 0) + 1
    print(f"  {vname}: {total} stories — {by_status}")
    for i in r.get("issues", []):
        sprint = i["fields"].get("sprint")
        sname = sprint["name"] if sprint else "NO SPRINT"
        print(f"    {i['key']:<10} [{i['fields']['status']['name']:<12}] sprint={sname:<30} {i['fields']['summary']}")

# ── R3 check ─────────────────────────────────────────────
print()
print("=" * 60)
print("R3 — CHECK")
print("=" * 60)
r3_versions = [v for v in versions if "R3" in v["name"]]
for vobj in r3_versions:
    vname = vobj["name"]
    jql7 = f'project={JIRA_PROJECT} AND fixVersion={vobj["id"]}'
    r = jira_search(jql7, "key,summary,status,issuetype", 200)
    print(f"  {vname}: {r.get('total', 0)} issues")
    for i in r.get("issues", []):
        print(f"    {i['key']:<10} [{i['fields']['issuetype']['name']:<8}] [{i['fields']['status']['name']:<12}] {i['fields']['summary']}")
if not r3_versions:
    print("  No R3 version found.")
