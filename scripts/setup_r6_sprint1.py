"""
Set up R6 Sprint 1 in Jira:
1. Create sprint on Board 2
2. Move P0 (Highest) + P1 (High) tickets into it
3. Start the sprint

DRY RUN by default. Pass --run to execute.
"""
import urllib.request, urllib.error, base64, json, sys, datetime

sys.path.insert(0, ".")
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
MODE = "DRY RUN" if DRY_RUN else "LIVE"
print("=" * 60)
print("  R6 Sprint 1 Setup  [" + MODE + "]")
print("=" * 60)

creds = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
headers_get = {"Authorization": "Basic " + creds, "Accept": "application/json"}
headers_post = {
    "Authorization": "Basic " + creds,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

BOARD_ID = 2
R6_VERSION_ID = "10109"


def api_get(path):
    req = urllib.request.Request(JIRA_BASE_URL + path, headers=headers_get)
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


def api_post(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=headers_post, method="POST")
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


def api_put(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=headers_post, method="PUT")
    urllib.request.urlopen(req, timeout=15)


# ────────────────────────────────────────────────────────────
# 1. CREATE SPRINT
# ────────────────────────────────────────────────────────────
print("\n--- CREATE SPRINT ---")
start_date = "2026-03-14"
end_date = "2026-03-28"  # 2-week sprint
sprint_name = "R6 Sprint 1 — P0+P1 Crash & Core Bugs"

if DRY_RUN:
    print("  [DRY] Would create sprint: " + sprint_name)
    print("  [DRY] Start: %s  End: %s" % (start_date, end_date))
    sprint_id = "DRY"
else:
    sprint_body = {
        "name": sprint_name,
        "goal": "Fix all P0 crashes (reports, supplier 500, driver raw keys) and P1 core workflow bugs (line totals, duplicate plans, yield, plan->order sync, auth guard)",
        "originBoardId": BOARD_ID,
        "startDate": start_date + "T09:00:00.000Z",
        "endDate": end_date + "T18:00:00.000Z",
    }
    resp = api_post("/rest/agile/1.0/sprint", sprint_body)
    sprint_id = resp["id"]
    print("  CREATED sprint: id=%s  name=%s" % (sprint_id, resp["name"]))

# ────────────────────────────────────────────────────────────
# 2. GET P0+P1 TICKETS (Highest + High priority in R6)
# ────────────────────────────────────────────────────────────
print("\n--- R6 HIGH-PRIORITY TICKETS ---")
issues = api_get("/rest/agile/1.0/board/2/issue?maxResults=100&jql=fixVersion%3D" + R6_VERSION_ID)
sprint_tickets = []
for iss in issues.get("issues", []):
    p = iss["fields"].get("priority", {}).get("name", "None")
    if p in ("Highest", "High"):
        sprint_tickets.append(iss)
        print("  %s  %-8s  %s" % (iss["key"], p, iss["fields"]["summary"][:65]))

print("\n  Total tickets for Sprint 1: %d" % len(sprint_tickets))

# ────────────────────────────────────────────────────────────
# 3. MOVE TICKETS INTO SPRINT
# ────────────────────────────────────────────────────────────
print("\n--- MOVE TICKETS TO SPRINT ---")
ticket_keys = [t["key"] for t in sprint_tickets]
issue_ids = [t["id"] for t in sprint_tickets]

if DRY_RUN:
    print("  [DRY] Would move %d tickets: %s" % (len(ticket_keys), ", ".join(ticket_keys)))
else:
    # Agile API: move issues to sprint
    move_body = {"issues": issue_ids}
    api_post("/rest/agile/1.0/sprint/%s/issue" % sprint_id, move_body)
    print("  Moved %d tickets to sprint %s" % (len(ticket_keys), sprint_id))

# ────────────────────────────────────────────────────────────
# 4. START THE SPRINT
# ────────────────────────────────────────────────────────────
print("\n--- START SPRINT ---")
if DRY_RUN:
    print("  [DRY] Would start sprint: " + sprint_name)
else:
    # Sprints are created in 'future' state, need to move to 'active'
    start_body = {
        "state": "active",
        "startDate": start_date + "T09:00:00.000Z",
        "endDate": end_date + "T18:00:00.000Z",
    }
    api_put("/rest/agile/1.0/sprint/%s" % sprint_id, start_body)
    print("  Sprint STARTED: %s" % sprint_name)

# ────────────────────────────────────────────────────────────
# SUMMARY
# ────────────────────────────────────────────────────────────
print("\n" + "=" * 60)
if DRY_RUN:
    print("DRY RUN complete.")
    print("Sprint '%s' would be created with %d tickets." % (sprint_name, len(sprint_tickets)))
    print("Run with --run to execute.")
else:
    print("Sprint '%s' is ACTIVE with %d tickets." % (sprint_name, len(sprint_tickets)))
    print("Tickets: %s" % ", ".join(ticket_keys))
print("=" * 60)
