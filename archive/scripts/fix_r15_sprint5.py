#!/usr/bin/env python3
"""Mark R1.5 Sprint 5 stories as Done in Jira (create if missing)."""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json", "Content-Type": "application/json"}

SPRINT5_ID = 41
VERSION_ID = "10035"
EPIC_KEY = "BC-219"  # [BC-E26] R1.5 — POS & Sales FE
DONE_ID = "31"

STORIES = [
    {"id": "BC-1601", "title": "FE /pos — Receipt modal after sale with print", "priority": "Highest",
     "ac": ["After POST /v1/pos/sales success, show receipt modal with line items, totals, payment, cashier, timestamp",
            "Print button triggers browser print dialog", "'New Sale' resets transaction and closes receipt"]},
    {"id": "BC-1602", "title": "FE /pos — Card payment terminal reference field", "priority": "High",
     "ac": ["When CARD, Terminal Reference text input appears (required)", "Reference sent as cardReference",
            "CASH mode hides terminal field, shows received amount + change"]},
    {"id": "BC-1603", "title": "FE /pos — End-of-day reconciliation view", "priority": "Highest",
     "ac": ["'End of Day' button visible on POS screen", "Calls POST /v1/pos/reconcile",
            "Shows summary: cash total, card total, refunds, net sales, expected cash in drawer", "Summary is printable"]},
    {"id": "BC-1604", "title": "FE /dashboard — Revenue widget (Today/Week/Month)", "priority": "High",
     "ac": ["Dashboard Revenue widget shows Today, This Week, This Month", "Data from GET /v1/reports/revenue-summary",
            "Currency displayed alongside values", "Widget uses same 60s auto-refresh"]},
]

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

def main():
    print("=" * 60)
    print(f"  {'DRY RUN' if DRY_RUN else 'LIVE RUN'} — R1.5 Sprint 5 → Done")
    print("=" * 60)

    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    types = {it["name"].lower(): it["id"] for it in data.get("issueTypes", [])}
    story_type_id = types.get("story")

    print(f"\n── Step 1: Create Sprint 5 stories ──")
    created_keys = []
    for story in STORIES:
        summary = f"[{story['id']}] {story['title']}"
        ac_text = "\n".join(f"• {ac}" for ac in story["ac"])
        desc = {"type":"doc","version":1,"content":[
            {"type":"paragraph","content":[{"type":"text","text":f"Epic: BC-E26 | Sprint: Sprint 5 | Release: R1.5 | Priority: {story['priority']}"}]},
            {"type":"heading","attrs":{"level":3},"content":[{"type":"text","text":"Acceptance Criteria"}]},
            {"type":"paragraph","content":[{"type":"text","text":ac_text}]},
        ]}
        body = {"fields":{"project":{"key":JIRA_PROJECT},"summary":summary,"issuetype":{"id":story_type_id},
                "description":desc,"priority":{"name":story["priority"]},"fixVersions":[{"id":VERSION_ID}],
                "customfield_10014":EPIC_KEY}}
        if DRY_RUN:
            print(f"  DRY  create: {summary[:60]}")
            created_keys.append(f"DRY-{story['id']}")
        else:
            s, d = jira("POST", "/rest/api/3/issue", body)
            if s == 201:
                created_keys.append(d["key"])
                print(f"  OK   {d['key']} ← {summary[:55]}")
            else:
                print(f"  FAIL ({s}): {json.dumps(d)[:120]}")

    real_keys = [k for k in created_keys if not k.startswith("DRY")]

    print(f"\n── Step 2: Assign to Sprint 5 (id={SPRINT5_ID}) ──")
    if DRY_RUN:
        print(f"  DRY  would assign {len(created_keys)} stories")
    elif real_keys:
        s, d = jira("POST", f"/rest/agile/1.0/sprint/{SPRINT5_ID}/issue", {"issues": real_keys})
        print(f"  {'OK' if s in (200,204) else 'FAIL'} assigned {len(real_keys)} stories")

    print(f"\n── Step 3: Transition to Done ──")
    all_keys = real_keys + [EPIC_KEY]
    for key in all_keys:
        if DRY_RUN:
            print(f"  DRY  {key} → Done")
        else:
            s, d = jira("POST", f"/rest/api/3/issue/{key}/transitions", {"transition":{"id":DONE_ID}})
            print(f"  {'OK' if s in (200,204) else 'FAIL'} {key} → Done")

    print(f"\n{'='*60}\n  COMPLETE{' (dry run)' if DRY_RUN else ''}\n{'='*60}")

if __name__ == "__main__":
    main()
