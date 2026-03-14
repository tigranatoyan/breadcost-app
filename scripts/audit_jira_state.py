"""Audit current Jira state: releases, sprints, statuses, transitions."""
import urllib.request, base64, json, sys

sys.path.insert(0, ".")
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

creds = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
headers = {"Authorization": "Basic " + creds, "Accept": "application/json"}


def api_get(path):
    req = urllib.request.Request(JIRA_BASE_URL + path, headers=headers)
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


# 1. Releases
print("=== RELEASES ===")
versions = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/versions")
for v in versions:
    rel = "RELEASED" if v.get("released") else "UNRELEASED"
    print("  %-40s  %-12s  id=%s" % (v["name"], rel, v["id"]))

# 2. Board
print("\n=== BOARD ===")
board = api_get("/rest/agile/1.0/board/2/configuration")
print("  Board: %s  Type: %s" % (board["name"], board.get("type", "?")))

# 3. Active/future sprints
print("\n=== SPRINTS (active/future) ===")
sprints = api_get("/rest/agile/1.0/board/2/sprint?state=active,future")
if sprints.get("values"):
    for s in sprints["values"]:
        print("  %-30s  state=%-10s  id=%s" % (s["name"], s["state"], s["id"]))
else:
    print("  (none)")

# 4. Closed sprints
sprints_closed = api_get("/rest/agile/1.0/board/2/sprint?state=closed&maxResults=50")
print("  Closed sprints: %d" % len(sprints_closed.get("values", [])))

# 5. Workflow statuses
print("\n=== WORKFLOW STATUSES ===")
statuses = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/statuses")
for issuetype in statuses:
    print("  Issue type: %s" % issuetype["name"])
    for s in issuetype.get("statuses", []):
        cat = s.get("statusCategory", {}).get("name", "?")
        print("    %-25s  id=%-6s  cat=%s" % (s["name"], s["id"], cat))
    break  # just first type — they share statuses

# 6. Transitions for sample ticket
print("\n=== TRANSITIONS (BC-254) ===")
try:
    trans = api_get("/rest/api/3/issue/BC-254/transitions")
    for t in trans.get("transitions", []):
        print("  %-25s  id=%-6s  -> %s" % (t["name"], t["id"], t["to"]["name"]))
except Exception as e:
    print("  Error: %s" % e)

# 7. R6 ticket priorities
print("\n=== R6 TICKETS BY PRIORITY ===")
try:
    issues = api_get("/rest/agile/1.0/board/2/issue?maxResults=100&jql=fixVersion%3D10109")
    prio_count = {}
    for iss in issues.get("issues", []):
        p = iss["fields"].get("priority", {}).get("name", "None")
        prio_count[p] = prio_count.get(p, 0) + 1
    for p in ["Highest", "High", "Medium", "Low", "Lowest"]:
        if p in prio_count:
            print("  %-10s  %d" % (p, prio_count[p]))
    total = sum(prio_count.values())
    print("  TOTAL      %d" % total)
except Exception as e:
    print("  Error: %s" % e)

# 8. R6 P0/P1 tickets (will go into Sprint 1)
print("\n=== R6 HIGH-PRIORITY TICKETS (Sprint 1 candidates) ===")
try:
    issues = api_get("/rest/agile/1.0/board/2/issue?maxResults=100&jql=fixVersion%3D10109")
    for iss in issues.get("issues", []):
        p = iss["fields"].get("priority", {}).get("name", "None")
        if p in ("Highest", "High"):
            key = iss["key"]
            summary = iss["fields"]["summary"][:70]
            status = iss["fields"]["status"]["name"]
            print("  %s  %-8s  %-10s  %s" % (key, p, status, summary))
except Exception as e:
    print("  Error: %s" % e)
