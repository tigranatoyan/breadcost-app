#!/usr/bin/env python3
"""
setup_r2_sprints.py — Push R2 vertical-slice sprints to JIRA
=============================================================
Each sprint delivers BE + FE for an end-to-end user journey.

Run:
    python setup_r2_sprints.py          # dry-run
    python setup_r2_sprints.py --run    # live

Requires config.py with JIRA credentials.
"""
import sys, json, base64, urllib.request, urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

from data import EPICS, STORIES

DRY_RUN = "--run" not in sys.argv

# ── Auth ─────────────────────────────────────────────────
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {
    "Authorization": "Basic " + _TOKEN,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

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

# ── R2 Version & Sprints ────────────────────────────────
VERSION_DEF = {
    "name": "R2 — Growth",
    "description": "Customer portal, loyalty program, supplier/PO workflow, delivery management, B2B invoicing, advanced reports. 6 vertical-slice sprints delivering BE+FE for E2E testing.",
    "released": False,
    "releaseDate": "2026-07-31",
}

SPRINTS_DEF = [
    {
        "name": "R2 Sprint 8 Portal",
        "goal": "Customer Portal Foundation (BE+FE): customer registration, login, catalog, order placement. E2E testable: customer registers → logs in → browses catalog → places order → sees status.",
        "startDate": "2026-05-01T00:00:00.000Z",
        "endDate": "2026-05-15T23:59:59.000Z",
    },
    {
        "name": "R2 Sprint 9 Loyalty",
        "goal": "Loyalty Program (BE+FE): points earning, tier configuration, redemption, points dashboard. E2E testable: customer completes order → earns points → advances tier → redeems points on next order.",
        "startDate": "2026-05-15T00:00:00.000Z",
        "endDate": "2026-05-29T23:59:59.000Z",
    },
    {
        "name": "R2 Sprint 10 Suppliers",
        "goal": "Supplier Management & PO Workflow (BE+FE): supplier catalog, PO suggestion, approval, Excel export, receipt matching. E2E testable: stock low → PO suggested → manager approves → warehouse receives → matched against PO.",
        "startDate": "2026-05-29T00:00:00.000Z",
        "endDate": "2026-06-12T23:59:59.000Z",
    },
    {
        "name": "R2 Sprint 11 Delivery",
        "goal": "Delivery Management (BE+FE): delivery run assignment, manifest generation, completion tracking, failed delivery handling, courier charge mgmt. E2E testable: orders ready → planner creates runs → manifest printed → driver marks delivered.",
        "startDate": "2026-06-12T00:00:00.000Z",
        "endDate": "2026-06-26T23:59:59.000Z",
    },
    {
        "name": "R2 Sprint 12 Finance",
        "goal": "B2B Invoicing & Credit Control + Subscription Tiers (BE+FE): invoice generation, payment tracking, credit limits, subscription mgmt. E2E testable: order delivered → invoice → payment recorded → credit enforced on next order; admin manages subscription tiers.",
        "startDate": "2026-06-26T00:00:00.000Z",
        "endDate": "2026-07-10T23:59:59.000Z",
    },
    {
        "name": "R2 Sprint 13 Reports",
        "goal": "Advanced Report Constructor + WhatsApp Integration (BE+FE): drag-drop report builder, KPI blocks, saved reports, export; WhatsApp order intake. E2E testable: user builds custom report → saves → exports; WhatsApp message → draft order reviewed.",
        "startDate": "2026-07-10T00:00:00.000Z",
        "endDate": "2026-07-24T23:59:59.000Z",
    },
]

# Sprint label → story IDs
SPRINT_STORY_MAP = {
    "Sprint 8":  ["BC-1101", "BC-1102", "BC-1103", "BC-1104", "BC-1105"],  # Customer Portal
    "Sprint 9":  ["BC-1201", "BC-1202", "BC-1203", "BC-1204", "BC-1205", "BC-1206"],  # Loyalty
    "Sprint 10": ["BC-1301", "BC-1302", "BC-1303", "BC-1304", "BC-1305", "BC-1306"],  # Suppliers
    "Sprint 11": ["BC-1401", "BC-1402", "BC-1403", "BC-1404", "BC-1405", "BC-1406"],  # Delivery
    "Sprint 12": ["BC-1501", "BC-1502", "BC-1503", "BC-1504", "BC-1505", "BC-1701", "BC-1702"],  # Finance + Subscription
    "Sprint 13": ["BC-1601", "BC-1602", "BC-1603", "BC-1604", "BC-209", "BC-210", "BC-211"],  # Reports + WhatsApp
}

# Filter R2 epics & stories
R2_EPICS = [e for e in EPICS if e.get("release") == "R2"]
R2_STORIES = [s for s in STORIES if s.get("release") == "R2"]

def ensure_version(project_id):
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}/versions")
    existing = {v["name"]: v["id"] for v in (data if s == 200 else [])}
    name = VERSION_DEF["name"]
    if name in existing:
        print(f"  SKIP version: {name} (id={existing[name]})")
        return existing[name]
    if DRY_RUN:
        print(f"  DRY  version: {name}")
        return "DRY_VERSION"
    body = {"projectId": int(project_id), **VERSION_DEF}
    s2, d2 = jira("POST", "/rest/api/3/version", body)
    if s2 == 201:
        print(f"  CREATE version: {name} → id={d2['id']}")
        return d2["id"]
    else:
        print(f"  FAIL version: {name} ({s2}) — {d2}")
        return None

def get_board_id():
    s, data = jira("GET", f"/rest/agile/1.0/board?projectKeyOrId={JIRA_PROJECT}")
    if s != 200 or not data.get("values"):
        print(f"  WARN: no board found")
        return None
    board_id = data["values"][0]["id"]
    print(f"  Board id={board_id}  name={data['values'][0]['name']}")
    return board_id

def ensure_sprints(board_id):
    if not board_id:
        return {}
    s, data = jira("GET", f"/rest/agile/1.0/board/{board_id}/sprint?maxResults=50")
    existing = {sp["name"]: sp for sp in (data.get("values", []) if s == 200 else [])}
    sprint_map = {}
    for i, sp_def in enumerate(SPRINTS_DEF):
        name = sp_def["name"]
        sprint_label = f"Sprint {8 + i}"
        if name in existing:
            sprint_map[sprint_label] = existing[name]["id"]
            print(f"  SKIP sprint: {name} (id={existing[name]['id']})")
            continue
        if DRY_RUN:
            print(f"  DRY  sprint: {name}")
            sprint_map[sprint_label] = f"DRY_{sprint_label}"
            continue
        body = {
            "name": name,
            "goal": sp_def["goal"],
            "startDate": sp_def["startDate"],
            "endDate": sp_def["endDate"],
            "originBoardId": board_id,
        }
        s2, d2 = jira("POST", "/rest/agile/1.0/sprint", body)
        if s2 in (200, 201):
            sprint_id = d2["id"]
            sprint_map[sprint_label] = sprint_id
            print(f"  CREATE sprint: {name} → id={sprint_id}")
        else:
            print(f"  FAIL sprint: {name} ({s2}) — {d2}")
    return sprint_map

def get_issue_type_ids():
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}/statuses")
    if s != 200:
        return {}
    return {t["name"].lower(): t["id"] for status_category in data for t in status_category.get("issueTypes", [])}

def ensure_epics(issue_type_ids, version_id):
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Epic",
        "fields": ["summary"],
        "maxResults": 200,
    })
    existing_epic_ids = {iss["fields"]["summary"].split("]")[0].strip("["): iss["key"] for iss in (issues.get("issues", []) if s == 200 else [])}
    
    epic_map = {}
    for epic in R2_EPICS:
        epic_id = epic["id"]
        if epic_id in existing_epic_ids:
            jira_key = existing_epic_ids[epic_id]
            epic_map[epic_id] = jira_key
            print(f"  SKIP epic: {epic_id} (exists: {jira_key})")
            continue
        if DRY_RUN:
            print(f"  DRY  epic: [{epic_id}] {epic['title'][:50]}")
            epic_map[epic_id] = f"DRY_{epic_id}"
            continue
        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "issuetype": {"id": issue_type_ids.get("epic", "10000")},
                "summary": f"[{epic_id}] {epic['title']}",
                "description": {
                    "type": "doc",
                    "version": 1,
                    "content": [{"type": "paragraph", "content": [{"type": "text", "text": epic['goal']}]}]
                },
                "fixVersions": [{"id": version_id}] if version_id and version_id != "DRY_VERSION" else [],
            }
        }
        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            jira_key = d2["key"]
            epic_map[epic_id] = jira_key
            print(f"  CREATE epic: {jira_key} ← [{epic_id}] {epic['title'][:40]}")
        else:
            print(f"  FAIL epic: [{epic_id}] ({s2}) — {d2}")
    return epic_map

def ensure_stories(issue_type_ids, version_id, epic_map):
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Story",
        "fields": ["summary"],
        "maxResults": 300,
    })
    existing_story_ids = {iss["fields"]["summary"].split("]")[0].strip("["): iss["key"] for iss in (issues.get("issues", []) if s == 200 else [])}
    
    for story in R2_STORIES:
        story_id = story["id"]
        if story_id in existing_story_ids:
            print(f"  SKIP story: {story_id} (exists)")
            continue
        if DRY_RUN:
            print(f"  DRY  story: [{story_id}] {story['title'][:50]}")
            continue
        epic_jira_key = epic_map.get(story["epic_id"])
        # Convert AC to Atlassian Document Format (ADF)
        ac_nodes = [{
            "type": "listItem",
            "content": [{"type": "paragraph", "content": [{"type": "text", "text": ac}]}]
        } for ac in story.get("acceptance_criteria", [])]
        desc_adf = {
            "type": "doc",
            "version": 1,
            "content": [
                {"type": "bulletList", "content": ac_nodes}
            ] if ac_nodes else [{"type": "paragraph", "content": []}]
        }
        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "issuetype": {"id": issue_type_ids.get("story", "10001")},
                "summary": f"[{story_id}] {story['title']}",
                "description": desc_adf,
                "priority": {"name": {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low"}.get(story["priority"], "Medium")},
                "fixVersions": [{"id": version_id}] if version_id and version_id != "DRY_VERSION" else [],
            }
        }
        if epic_jira_key and epic_jira_key != f"DRY_{story['epic_id']}":
            body["fields"]["parent"] = {"key": epic_jira_key}
        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            print(f"  CREATE story: {d2['key']} ← [{story_id}] {story['title'][:45]}")
        else:
            print(f"  FAIL story: [{story_id}] ({s2}) — {d2}")

def assign_stories_to_sprints(sprint_map):
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Story AND fixVersion='R2 — Growth'",
        "fields": ["summary"],
        "maxResults": 300,
    })
    story_key_map = {iss["fields"]["summary"].split("]")[0].strip("["): iss["key"] for iss in (issues.get("issues", []) if s == 200 else [])}
    
    for sprint_label, story_ids in SPRINT_STORY_MAP.items():
        sprint_id = sprint_map.get(sprint_label)
        if not sprint_id or (isinstance(sprint_id, str) and sprint_id.startswith("DRY_")):
            print(f"  DRY  {sprint_label}: would assign {len(story_ids)} stories")
            continue
        jira_keys = [story_key_map[sid] for sid in story_ids if sid in story_key_map]
        if not jira_keys:
            continue
        s2, _ = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}/issue", {"issues": jira_keys})
        if s2 in (200, 204):
            print(f"  {sprint_label} ({sprint_id}): assigned {len(jira_keys)} stories")
        else:
            print(f"  FAIL {sprint_label}: {s2}")

def main():
    print("=" * 64)
    print("  BreadCost — R2 Vertical-Slice Sprint Setup")
    print(f"  Project : {JIRA_PROJECT}")
    print(f"  Mode    : {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
    print(f"  Epics   : {len(R2_EPICS)}")
    print(f"  Stories : {len(R2_STORIES)}")
    print("=" * 64)

    if not DRY_RUN:
        confirm = input("\nType YES to proceed: ").strip()
        if confirm != "YES":
            sys.exit(0)

    # 1. Project
    print("\n── Project ────────────────────────────────────────")
    s, project = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    if s != 200:
        print(f"ERROR: project {JIRA_PROJECT} not found")
        sys.exit(1)
    project_id = project["id"]
    print(f"  {project['key']} · {project['name']} · id={project_id}")

    # 2. Fix Version
    print("\n── Fix Version ────────────────────────────────────")
    version_id = ensure_version(project_id)

    # 3. Sprints
    print("\n── Board & Sprints ────────────────────────────────")
    board_id = get_board_id()
    sprint_map = ensure_sprints(board_id)

    # 4. Issue Types
    print("\n── Issue Types ────────────────────────────────────")
    issue_type_ids = get_issue_type_ids()
    print(f"  Available: {list(issue_type_ids.keys())}")

    # 5. Epics
    print(f"\n── Creating {len(R2_EPICS)} R2 Epics ─────────────────────")
    epic_map = ensure_epics(issue_type_ids, version_id)

    # 6. Stories
    print(f"\n── Creating {len(R2_STORIES)} R2 Stories ───────────────────")
    ensure_stories(issue_type_ids, version_id, epic_map)

    # 7. Sprint Assignments
    print("\n── Sprint Assignments ─────────────────────────────")
    assign_stories_to_sprints(sprint_map)

    print("\n" + "=" * 64)
    print("  SUMMARY")
    print("=" * 64)
    print(f"  Version: {VERSION_DEF['name']}")
    print(f"  Sprints: {len(SPRINTS_DEF)}")
    for i, sp_def in enumerate(SPRINTS_DEF):
        sprint_label = f"Sprint {8 + i}"
        story_count = len(SPRINT_STORY_MAP.get(sprint_label, []))
        print(f"    {sp_def['name']}")
        print(f"      → {story_count} stories, dates: {sp_def['startDate'][:10]} → {sp_def['endDate'][:10]}")
    print(f"  Epics: {len(R2_EPICS)}")
    print(f"  Stories: {len(R2_STORIES)}")
    if DRY_RUN:
        print("\n  ⚠  This was a DRY RUN. Run with --run to create in JIRA.")

if __name__ == "__main__":
    main()
