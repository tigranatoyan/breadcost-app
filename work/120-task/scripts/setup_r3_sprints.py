#!/usr/bin/env python3
"""
setup_r3_sprints.py — Push R3 vertical-slice sprints to JIRA
=============================================================
R3 — AI + Mobile: 6 epics, 15 stories, 3 sprints.

Sprint 14 — AI WhatsApp + Exchange Rate API (BC-E18 + BC-E22)
Sprint 15 — AI Forecasting + Driver App (BC-E19 + BC-E21)
Sprint 16 — AI Pricing + Mobile Customer App (BC-E20 + BC-E23)

Run:
    python setup_r3_sprints.py          # dry-run
    python setup_r3_sprints.py --run    # live

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

# ── R3 Sprint Definitions ────────────────────────────────────
SPRINTS_DEF = [
    {
        "name": "R3 S14 WhatsApp + FX",
        "goal": "AI WhatsApp Order Intake (BC-E18): AI-driven end-to-end WhatsApp order conversation with intent extraction, 2-way confirmation, upsell, and human escalation. Plus Exchange Rate & Supplier API integration (BC-E22). E2E testable: WhatsApp message → AI parses → draft order → operator reviews → confirms; exchange rates pulled from API.",
        "startDate": "2026-08-01T00:00:00.000Z",
        "endDate": "2026-08-21T23:59:59.000Z",
    },
    {
        "name": "R3 S15 Forecast + Driver",
        "goal": "AI Replenishment & Forecasting (BC-E19): replenishment hints, production planning suggestions, demand forecasting. Plus Driver Mobile App (BC-E21): real-time delivery tracking, packaging confirmation, on-spot payment. E2E testable: AI suggests replenishment → manager reviews; driver app receives manifest → confirms packaging → marks delivered → collects payment.",
        "startDate": "2026-08-21T00:00:00.000Z",
        "endDate": "2026-09-11T23:59:59.000Z",
    },
    {
        "name": "R3 S16 Pricing + Mobile",
        "goal": "AI Pricing & Anomaly Alerts (BC-E20): pricing adjustment suggestions, report anomaly detection. Plus Mobile Customer App (BC-E23): iOS/Android native app for customer portal features. E2E testable: AI flags pricing anomaly → manager reviews; customer uses mobile app to browse → order → track → loyalty dashboard.",
        "startDate": "2026-09-11T00:00:00.000Z",
        "endDate": "2026-10-02T23:59:59.000Z",
    },
]

# Sprint label → story IDs (from data.py)
SPRINT_STORY_MAP = {
    "Sprint 14": [
        "BC-1801", "BC-1802", "BC-1803", "BC-1804",  # AI WhatsApp (E18)
        "BC-2201", "BC-2202",                          # Exchange Rate + Supplier API (E22)
    ],
    "Sprint 15": [
        "BC-1901", "BC-1902", "BC-1903",              # AI Replenishment & Forecasting (E19)
        "BC-2101", "BC-2102", "BC-2103",              # Driver Mobile App (E21)
    ],
    "Sprint 16": [
        "BC-2001", "BC-2002",                          # AI Pricing & Anomaly (E20)
        "BC-2301",                                     # Mobile Customer App (E23)
    ],
}

# Filter R3 epics & stories
R3_EPICS = [e for e in EPICS if e.get("release") == "R3"]
R3_STORIES = [s for s in STORIES if s.get("release") == "R3"]

# R3 version id from JIRA (existing: id=10002)
R3_VERSION_NAME = "R3 — AI + Mobile"


def get_board_id():
    s, data = jira("GET", f"/rest/agile/1.0/board?projectKeyOrId={JIRA_PROJECT}")
    if s != 200 or not data.get("values"):
        print("  WARN: no board found")
        return None
    board_id = data["values"][0]["id"]
    print(f"  Board id={board_id}  name={data['values'][0]['name']}")
    return board_id


def get_version_id():
    """Get the existing R3 version ID."""
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}/versions")
    if s != 200:
        return None
    for v in (data if isinstance(data, list) else []):
        if "R3" in v["name"]:
            print(f"  Version: {v['name']} (id={v['id']})")
            return v["id"]
    return None


def ensure_sprints(board_id):
    if not board_id:
        return {}
    s, data = jira("GET", f"/rest/agile/1.0/board/{board_id}/sprint?maxResults=50")
    existing = {sp["name"]: sp for sp in (data.get("values", []) if s == 200 else [])}
    sprint_map = {}
    for i, sp_def in enumerate(SPRINTS_DEF):
        name = sp_def["name"]
        sprint_label = f"Sprint {14 + i}"
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
    result = {}
    for category in data:
        for t in category.get("issueTypes", []) if isinstance(category, dict) else []:
            result[t["name"].lower()] = t["id"]
    # Fallback: try the direct issuetype endpoint
    if not result:
        s2, data2 = jira("GET", f"/rest/api/3/issuetype")
        if s2 == 200:
            result = {t["name"].lower(): t["id"] for t in data2}
    return result


def ensure_epics(issue_type_ids, version_id):
    """Ensure R3 epics exist — use the original BC-1..24 epics if they match."""
    # Get all existing epics
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Epic",
        "fields": ["summary"],
        "maxResults": 300,
    })
    # Map [BC-Exx] → Jira key
    existing = {}
    for iss in (issues.get("issues", []) if s == 200 else []):
        summary = iss["fields"]["summary"]
        if "[" in summary and "]" in summary:
            epic_code = summary.split("]")[0].strip("[")
            existing[epic_code] = iss["key"]

    epic_map = {}
    for epic in R3_EPICS:
        epic_id = epic["id"]
        if epic_id in existing:
            jira_key = existing[epic_id]
            epic_map[epic_id] = jira_key
            print(f"  SKIP epic: {epic_id} (exists: {jira_key})")
            # Assign version if needed
            if version_id and not DRY_RUN:
                jira("PUT", f"/rest/api/3/issue/{jira_key}", {
                    "fields": {"fixVersions": [{"id": version_id}]}
                })
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
                    "content": [{"type": "paragraph", "content": [{"type": "text", "text": epic["goal"]}]}],
                },
                "fixVersions": [{"id": version_id}] if version_id else [],
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
    """Ensure R3 stories exist. They already exist (BC-122..136) — link to epics/version."""
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Story",
        "fields": ["summary"],
        "maxResults": 500,
    })
    existing = {}
    for iss in (issues.get("issues", []) if s == 200 else []):
        summary = iss["fields"]["summary"]
        if "[" in summary and "]" in summary:
            story_code = summary.split("]")[0].strip("[")
            existing[story_code] = iss["key"]

    for story in R3_STORIES:
        story_id = story["id"]
        if story_id in existing:
            jira_key = existing[story_id]
            print(f"  EXISTS story: {story_id} → {jira_key}")
            # Update epic link and version
            if not DRY_RUN:
                epic_jira_key = epic_map.get(story["epic_id"])
                update_fields = {}
                if version_id:
                    update_fields["fixVersions"] = [{"id": version_id}]
                if epic_jira_key and not epic_jira_key.startswith("DRY_"):
                    update_fields["parent"] = {"key": epic_jira_key}
                if update_fields:
                    s2, _ = jira("PUT", f"/rest/api/3/issue/{jira_key}", {"fields": update_fields})
                    if s2 in (200, 204):
                        print(f"    → updated version + epic link")
                    else:
                        print(f"    → update failed ({s2})")
            continue
        # Create if missing
        if DRY_RUN:
            print(f"  DRY  story: [{story_id}] {story['title'][:50]}")
            continue
        epic_jira_key = epic_map.get(story["epic_id"])
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
                "fixVersions": [{"id": version_id}] if version_id else [],
            }
        }
        if epic_jira_key and not epic_jira_key.startswith("DRY_"):
            body["fields"]["parent"] = {"key": epic_jira_key}
        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            print(f"  CREATE story: {d2['key']} ← [{story_id}] {story['title'][:45]}")
        else:
            print(f"  FAIL story: [{story_id}] ({s2}) — {d2}")


def assign_stories_to_sprints(sprint_map):
    """Assign R3 stories to their target sprints."""
    # Get all stories
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND issuetype=Story",
        "fields": ["summary"],
        "maxResults": 500,
    })
    story_key_map = {}
    for iss in (issues.get("issues", []) if s == 200 else []):
        summary = iss["fields"]["summary"]
        if "[" in summary and "]" in summary:
            story_code = summary.split("]")[0].strip("[")
            story_key_map[story_code] = iss["key"]

    for sprint_label, story_ids in SPRINT_STORY_MAP.items():
        sprint_id = sprint_map.get(sprint_label)
        if not sprint_id or (isinstance(sprint_id, str) and sprint_id.startswith("DRY_")):
            story_names = [sid for sid in story_ids]
            print(f"  DRY  {sprint_label}: would assign {story_names}")
            continue
        jira_keys = [story_key_map[sid] for sid in story_ids if sid in story_key_map]
        if not jira_keys:
            print(f"  SKIP {sprint_label}: no matching stories found")
            continue
        s2, _ = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}/issue", {"issues": jira_keys})
        if s2 in (200, 204):
            print(f"  {sprint_label} ({sprint_id}): assigned {len(jira_keys)} stories")
        else:
            print(f"  FAIL {sprint_label}: {s2}")


def main():
    print("=" * 64)
    print("  BreadCost — R3 AI + Mobile Sprint Setup")
    print(f"  Project : {JIRA_PROJECT}")
    print(f"  Mode    : {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
    print(f"  Epics   : {len(R3_EPICS)}")
    print(f"  Stories : {len(R3_STORIES)}")
    print("=" * 64)

    if not DRY_RUN:
        confirm = input("\nType YES to proceed: ").strip()
        if confirm != "YES":
            print("Aborted.")
            sys.exit(0)

    # 1. Project
    print("\n── Project ────────────────────────────────────────")
    s, project = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    if s != 200:
        print(f"ERROR: project {JIRA_PROJECT} not found")
        sys.exit(1)
    project_id = project["id"]
    print(f"  {project['key']} · {project['name']} · id={project_id}")

    # 2. Version
    print("\n── Version ────────────────────────────────────────")
    version_id = get_version_id()
    if not version_id:
        print(f"  WARN: R3 version not found")

    # 3. Board & Sprints
    print("\n── Board & Sprints ────────────────────────────────")
    board_id = get_board_id()
    sprint_map = ensure_sprints(board_id)

    # 4. Issue Types
    print("\n── Issue Types ────────────────────────────────────")
    issue_type_ids = get_issue_type_ids()
    print(f"  Available: {list(issue_type_ids.keys())}")

    # 5. Epics
    print(f"\n── Ensuring {len(R3_EPICS)} R3 Epics ───────────────────")
    epic_map = ensure_epics(issue_type_ids, version_id)

    # 6. Stories
    print(f"\n── Ensuring {len(R3_STORIES)} R3 Stories ─────────────────")
    ensure_stories(issue_type_ids, version_id, epic_map)

    # 7. Sprint Assignments
    print("\n── Sprint Assignments ─────────────────────────────")
    assign_stories_to_sprints(sprint_map)

    # Summary
    print("\n" + "=" * 64)
    print("  R3 PLAN SUMMARY")
    print("=" * 64)
    for i, sp_def in enumerate(SPRINTS_DEF):
        sprint_label = f"Sprint {14 + i}"
        stories = SPRINT_STORY_MAP.get(sprint_label, [])
        print(f"\n  {sp_def['name']}")
        print(f"    Dates: {sp_def['startDate'][:10]} → {sp_def['endDate'][:10]}")
        print(f"    Stories ({len(stories)}):")
        # Look up story titles from data.py
        story_lookup = {s["id"]: s["title"] for s in R3_STORIES}
        for sid in stories:
            title = story_lookup.get(sid, "?")
            print(f"      [{sid}] {title}")

    print(f"\n  Total: {len(R3_EPICS)} epics, {len(R3_STORIES)} stories, {len(SPRINTS_DEF)} sprints")
    if DRY_RUN:
        print("\n  ⚠  DRY RUN. Run with --run to apply changes.")


if __name__ == "__main__":
    main()
