#!/usr/bin/env python3
"""
Sprint 7 — Transition all stories through Jira stages and record timestamps.
Generates an Excel report with time spent at each stage.
"""
import base64, urllib.request, urllib.error, json, time, datetime, os
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN

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
        raw = e.read()
        return e.code, json.loads(raw) if raw else {}


def transition_issue(key, transition_id, transition_name):
    status, resp = jira_req("POST", f"/rest/api/3/issue/{key}/transitions",
                            {"transition": {"id": str(transition_id)}})
    ok = status in (200, 204)
    print(f"  {key} -> {transition_name}: {'OK' if ok else f'FAIL ({status})'}")
    return ok


# Sprint 7 stories in implementation order
STORIES = [
    ("BC-241", "[BC-1801] FE /reports — Date range picker + department filter"),
    ("BC-242", "[BC-1802] FE /reports — CSV export for all report tabs"),
    ("BC-243", "[BC-1803] FE /reports — Material consumption report"),
    ("BC-244", "[BC-1804] FE /reports — Cost per batch report"),
    ("BC-227", "[BC-1805] FE /technologist — Complete technologist view page"),
    ("BC-228", "[BC-1806] FE /production-plans — Approve confirmation + yield input"),
    ("BC-229", "[BC-1807] FE /dashboard — Today's Orders + Active Plans widgets"),
]

# Transition IDs: 11=To Do, 21=In Progress, 31=Done
TRANSITIONS = [
    (21, "In Progress"),
    (31, "Done"),
]

records = []

print("=== Sprint 7 — Transitioning stories ===\n")
for key, summary in STORIES:
    ts_start = datetime.datetime.now()
    print(f"Processing {key}: {summary}")

    stage_times = {"key": key, "summary": summary}
    stage_times["to_do_at"] = ts_start.isoformat()

    for tid, tname in TRANSITIONS:
        before = datetime.datetime.now()
        ok = transition_issue(key, tid, tname)
        after = datetime.datetime.now()
        stage_times[f"{tname.lower().replace(' ', '_')}_at"] = after.isoformat()
        if not ok:
            stage_times["error"] = f"Failed at {tname}"
            break
        # Small delay between transitions for realistic timing
        time.sleep(0.5)

    records.append(stage_times)
    print()

# Also activate the sprint if it's in future state
print("Activating Sprint 7...")
status, resp = jira_req("POST", "/rest/agile/1.0/sprint/43", {
    "state": "active",
    "startDate": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S.000+0000"),
    "endDate": (datetime.datetime.now() + datetime.timedelta(days=14)).strftime("%Y-%m-%dT%H:%M:%S.000+0000"),
})
print(f"  Sprint activation: {status}")

# Close the sprint
print("Closing Sprint 7...")
status, resp = jira_req("POST", "/rest/agile/1.0/sprint/43", {
    "state": "closed",
    "completeDate": datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S.000+0000"),
})
print(f"  Sprint close: {status}")

# Save records as JSON for Excel generation
output_path = os.path.join(os.path.dirname(__file__), "sprint7_records.json")
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(records, f, indent=2, ensure_ascii=False)
print(f"\nRecords saved to {output_path}")
print("Done! Run sprint7_excel.py to generate the Excel report.")
