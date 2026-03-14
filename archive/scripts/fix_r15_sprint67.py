#!/usr/bin/env python3
"""
fix_r15_sprint67.py — Fix Sprint 6 & 7: create missing R1.5 stories,
remove misassigned R2 stories from sprints.
"""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json", "Content-Type": "application/json"}

VERSION_ID = "10035"  # R1.5 — Frontend E2E Completion

# Sprint 6 — missing stories (BC-1701, BC-1702 were matched to R2 subscription stories BC-120, BC-121)
SPRINT6_ID = 42
EPIC6_KEY = "BC-220"  # [BC-E27] R1.5 — Admin, Config & Catalog FE
SPRINT6_MISSING = [
    {"id": "BC-1701", "title": "FE /admin — User edit (roles, department, display name)", "priority": "Highest",
     "ac": ["Click Edit on user row opens edit modal with display name, role(s), department select",
            "Calls PUT /v1/users/{id}?tenantId= on save",
            "Changes reflected in user table without page reload",
            "Only Admin role can see edit controls"]},
    {"id": "BC-1702", "title": "FE /admin — Password reset UI", "priority": "High",
     "ac": ["'Reset Password' button on each user row",
            "Opens modal with new password + confirm fields",
            "Calls POST /v1/users/{id}/reset-password?tenantId=",
            "Success message shown"]},
]
# R2 stories wrongly in Sprint 6
R2_WRONG_S6 = ["BC-120", "BC-121"]

# Sprint 7 — missing stories (BC-1801..1804 matched R2 AI stories BC-122..125)
SPRINT7_ID = 43
EPIC7_KEY = "BC-221"  # [BC-E28] R1.5 — Reports, Dashboard & Production Polish FE
SPRINT7_MISSING = [
    {"id": "BC-1801", "title": "FE /reports — Date range picker + department filter", "priority": "Highest",
     "ac": ["All report tabs have date range picker (start, end) as common control",
            "Department dropdown on all report tabs",
            "Filters affect data displayed", "Clear filters button"]},
    {"id": "BC-1802", "title": "FE /reports — CSV export for all report tabs", "priority": "High",
     "ac": ["Export CSV button on each report tab",
            "CSV includes all currently visible data (respecting filters)",
            "Filename includes report name + date range"]},
    {"id": "BC-1803", "title": "FE /reports — Material consumption report (planned vs actual)", "priority": "High",
     "ac": ["New report tab: Material Consumption",
            "Per-batch: planned ingredient qty vs actual issued qty",
            "Variance column highlighted if > 5%"]},
    {"id": "BC-1804", "title": "FE /reports — Cost per batch report", "priority": "High",
     "ac": ["New report tab: Cost per Batch",
            "Per-batch: FIFO material cost total, yield, cost per unit produced"]},
]
R2_WRONG_S7 = ["BC-122", "BC-123", "BC-124", "BC-125"]


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


def create_stories(stories, epic_key, sprint_id, sprint_label, story_type_id):
    print(f"\n── Create {sprint_label} stories ──")
    created = []
    for story in stories:
        summary = f"[{story['id']}] {story['title']}"
        ac_text = "\n".join(f"• {ac}" for ac in story["ac"])
        desc = {"type": "doc", "version": 1, "content": [
            {"type": "paragraph", "content": [{"type": "text", "text": f"Epic: {epic_key} | {sprint_label} | Release: R1.5 | Priority: {story['priority']}"}]},
            {"type": "heading", "attrs": {"level": 3}, "content": [{"type": "text", "text": "Acceptance Criteria"}]},
            {"type": "paragraph", "content": [{"type": "text", "text": ac_text}]},
        ]}
        body = {"fields": {"project": {"key": JIRA_PROJECT}, "summary": summary,
                "issuetype": {"id": story_type_id}, "description": desc,
                "priority": {"name": story["priority"]},
                "fixVersions": [{"id": VERSION_ID}],
                "customfield_10014": epic_key}}
        if DRY_RUN:
            print(f"  DRY  create: {summary[:65]}")
            created.append(f"DRY-{story['id']}")
        else:
            s, d = jira("POST", "/rest/api/3/issue", body)
            if s == 201:
                created.append(d["key"])
                print(f"  OK   {d['key']} ← {summary[:60]}")
            else:
                print(f"  FAIL ({s}): {json.dumps(d)[:120]}")
    return created


def assign_to_sprint(keys, sprint_id, sprint_label):
    real = [k for k in keys if not k.startswith("DRY")]
    print(f"\n── Assign to {sprint_label} (id={sprint_id}) ──")
    if DRY_RUN:
        print(f"  DRY  would assign {len(keys)} stories")
    elif real:
        s, d = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}/issue", {"issues": real})
        print(f"  {'OK' if s in (200,204) else 'FAIL'} assigned {len(real)} stories")


def remove_from_sprint(keys, sprint_label):
    print(f"\n── Remove R2 stories from {sprint_label} → backlog ──")
    for key in keys:
        if DRY_RUN:
            print(f"  DRY  remove {key}")
        else:
            # Move to backlog removes from sprint
            s, d = jira("POST", "/rest/agile/1.0/backlog", {"issues": [key]})
            if s in (200, 204):
                print(f"  OK   {key} → backlog")
            else:
                print(f"  WARN {key} ({s})")


def main():
    print("=" * 60)
    print(f"  {'DRY RUN' if DRY_RUN else 'LIVE RUN'} — Fix R1.5 Sprint 6 & 7")
    print("=" * 60)

    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    types = {it["name"].lower(): it["id"] for it in data.get("issueTypes", [])}
    story_type_id = types.get("story")
    print(f"Story type id = {story_type_id}")

    # Sprint 6
    remove_from_sprint(R2_WRONG_S6, "Sprint 6")
    s6_keys = create_stories(SPRINT6_MISSING, EPIC6_KEY, SPRINT6_ID, "Sprint 6", story_type_id)
    assign_to_sprint(s6_keys, SPRINT6_ID, "Sprint 6")

    # Sprint 7
    remove_from_sprint(R2_WRONG_S7, "Sprint 7")
    s7_keys = create_stories(SPRINT7_MISSING, EPIC7_KEY, SPRINT7_ID, "Sprint 7", story_type_id)
    assign_to_sprint(s7_keys, SPRINT7_ID, "Sprint 7")

    print(f"\n{'='*60}\n  COMPLETE{' (dry run)' if DRY_RUN else ''}\n{'='*60}")


if __name__ == "__main__":
    main()
