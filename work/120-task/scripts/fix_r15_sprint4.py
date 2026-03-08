#!/usr/bin/env python3
"""
fix_r15_sprint4.py — Fix R1.5 Sprint 4 Jira State
====================================================
Problem: setup_r15_sprints.py matched R2 invoicing stories (BC-111..115)
whose summaries also start with [BC-1501]..[BC-1505], and assigned them
to Sprint 4 instead of creating the actual R1.5 stories.

This script:
  1. Creates the 5 real R1.5 Sprint 4 stories
  2. Links them to epic BC-218, fixVersion R1.5, Sprint 4
  3. Transitions them to Done
  4. Removes the R2 invoicing stories (BC-111..115) from Sprint 4

Usage:
    python fix_r15_sprint4.py          # dry-run
    python fix_r15_sprint4.py --run    # live
"""
import sys, json, base64, urllib.request, urllib.error

from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {
    "Authorization": "Basic " + _TOKEN,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

# Known IDs from previous setup
SPRINT4_ID = 40
VERSION_ID = "10035"  # R1.5 — Frontend E2E Completion
EPIC_KEY = "BC-218"   # [BC-E25] R1.5 — Inventory & Warehouse FE

# R2 invoicing stories mistakenly placed in Sprint 4
R2_WRONG_KEYS = ["BC-111", "BC-112", "BC-113", "BC-114", "BC-115"]

# The real R1.5 Sprint 4 stories to create
STORIES = [
    {
        "id": "BC-1501",
        "title": "FE /inventory — Adjustment modal (waste/spoilage/correction)",
        "priority": "Highest",
        "ac": [
            "Adjustment button opens modal with: Item, Qty (+/-), Reason Code (WASTE/SPOILAGE/COUNT_CORRECTION/OTHER), Notes",
            "Calls POST /v1/inventory/adjust?tenantId=",
            "Stock table updates after adjustment without full page reload",
            "Only Admin, Warehouse roles can see button",
        ],
    },
    {
        "id": "BC-1502",
        "title": "FE /inventory — Lot detail expand with FIFO cost layers",
        "priority": "High",
        "ac": [
            "Clicking a stock row expands to show FIFO lots: lotId, received date, original qty, remaining qty, unit cost",
            "Data sourced from existing inventory positions or new lot endpoint",
            "Cost layers ordered by receipt date (oldest first)",
        ],
    },
    {
        "id": "BC-1503",
        "title": "FE /inventory — Department/site filter + last receipt date",
        "priority": "High",
        "ac": [
            "Department dropdown filter on stock level table",
            "Last Receipt Date column shown in stock table",
            "Filters applied client-side from existing data",
        ],
    },
    {
        "id": "BC-1504",
        "title": "FE /inventory — Receive Lot: currency + exchange rate fields",
        "priority": "Medium",
        "ac": [
            "Receive Stock modal includes Currency select + Exchange Rate to Main Currency field",
            "Exchange rate field shown only when currency != main currency",
            "Cost stored in original + converted currency",
        ],
    },
    {
        "id": "BC-1505",
        "title": "FE /dashboard — Stock alert widget + 60s auto-refresh",
        "priority": "High",
        "ac": [
            "Dashboard shows dedicated 'Stock Alerts' count widget with severity breakdown",
            "All dashboard widgets auto-refresh every 60 seconds",
            "Manual Refresh button also available",
            "Top 5 Products widget shows quantity ordered this week",
        ],
    },
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
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode()[:300]}


def main():
    print("=" * 60)
    if DRY_RUN:
        print("  DRY RUN — no changes will be made")
    else:
        print("  LIVE RUN — changes WILL be made")
    print("=" * 60)

    # Get issue type IDs
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    types = {it["name"].lower(): it["id"] for it in data.get("issueTypes", [])}
    story_type_id = types.get("story")
    print(f"\nStory issue type id = {story_type_id}")

    # ── Step 1: Remove R2 stories from Sprint 4 ─────────────
    print(f"\n── Step 1: Remove R2 stories from Sprint 4 (id={SPRINT4_ID}) ──")
    for key in R2_WRONG_KEYS:
        if DRY_RUN:
            print(f"  DRY  remove {key} from Sprint 4")
        else:
            # Move to backlog (removes from sprint)
            s, d = jira("POST", "/rest/agile/1.0/backlog", {"issues": [key]})
            if s in (200, 204):
                print(f"  OK   {key} → backlog")
            else:
                print(f"  FAIL {key} ({s}): {d}")

    # ── Step 2: Create R1.5 Sprint 4 stories ────────────────
    print(f"\n── Step 2: Create R1.5 Sprint 4 stories ──")
    created_keys = []
    for story in STORIES:
        summary = f"[{story['id']}] {story['title']}"
        ac_text = "\n".join(f"• {ac}" for ac in story["ac"])
        description = {
            "type": "doc", "version": 1,
            "content": [
                {"type": "paragraph", "content": [{"type": "text", "text": f"Epic: BC-E25 | Sprint: Sprint 4 | Release: R1.5 | Priority: {story['priority']}"}]},
                {"type": "heading", "attrs": {"level": 3}, "content": [{"type": "text", "text": "Acceptance Criteria"}]},
                {"type": "paragraph", "content": [{"type": "text", "text": ac_text}]},
            ],
        }
        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "summary": summary,
                "issuetype": {"id": story_type_id},
                "description": description,
                "priority": {"name": story["priority"]},
                "fixVersions": [{"id": VERSION_ID}],
                "customfield_10014": EPIC_KEY,  # Epic link
            }
        }
        if DRY_RUN:
            print(f"  DRY  create: {summary[:60]}")
            created_keys.append(f"DRY-{story['id']}")
        else:
            s, d = jira("POST", "/rest/api/3/issue", body)
            if s == 201:
                key = d["key"]
                created_keys.append(key)
                print(f"  OK   {key} ← {summary[:55]}")
            else:
                print(f"  FAIL ({s}): {json.dumps(d)[:120]}")

    # ── Step 3: Assign to Sprint 4 ──────────────────────────
    real_keys = [k for k in created_keys if not k.startswith("DRY")]
    print(f"\n── Step 3: Assign {len(real_keys)} stories to Sprint 4 ──")
    if DRY_RUN:
        print(f"  DRY  would assign {len(created_keys)} stories to sprint {SPRINT4_ID}")
    elif real_keys:
        s, d = jira("POST", f"/rest/agile/1.0/sprint/{SPRINT4_ID}/issue", {"issues": real_keys})
        if s in (200, 204):
            print(f"  OK   assigned {len(real_keys)} stories to Sprint 4")
        else:
            print(f"  FAIL ({s}): {d}")

    # ── Step 4: Transition to Done ───────────────────────────
    print(f"\n── Step 4: Transition stories + epic to Done ──")
    all_keys = real_keys + [EPIC_KEY]
    done_id = "31"  # Known from previous runs
    for key in all_keys:
        if DRY_RUN:
            print(f"  DRY  {key} → Done (transition {done_id})")
        else:
            s, d = jira("POST", f"/rest/api/3/issue/{key}/transitions",
                         {"transition": {"id": done_id}})
            if s in (200, 204):
                print(f"  OK   {key} → Done")
            else:
                print(f"  FAIL {key} ({s}): {d}")

    print("\n" + "=" * 60)
    print("  COMPLETE" + (" (dry run)" if DRY_RUN else ""))
    print("=" * 60)


if __name__ == "__main__":
    main()
