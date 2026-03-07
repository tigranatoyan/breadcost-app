#!/usr/bin/env python3
"""
setup_r15_sprints.py
====================
Push R1.5 — Frontend E2E Completion to JIRA:
  1. Create Fix Version: R1.5 — Frontend E2E Completion
  2. Create Sprints 4–7 (vertical slices)
  3. Create R1.5 Epics (BC-E25..E28) — one per sprint
  4. Create R1.5 Stories (BC-1501..1807) under their epics
  5. Assign stories → sprints + fix version + epic link

Run:
    python setup_r15_sprints.py --run    # live
    python setup_r15_sprints.py          # dry-run

Requires config.py with JIRA credentials.
"""

import sys
import json
import base64
import urllib.request
import urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
except ImportError:
    print("ERROR: config.py not found. Create it with your JIRA credentials.")
    print("  JIRA_BASE_URL = 'https://yourorg.atlassian.net'")
    print("  JIRA_EMAIL = 'your@email.com'")
    print("  JIRA_API_TOKEN = 'your-api-token'")
    print("  JIRA_PROJECT = 'BC'")
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


def jira_search(jql, fields, page_size=100):
    """Paginate POST /search/jql and return all issues."""
    issues, next_page = [], None
    while True:
        body = {"jql": jql, "maxResults": page_size, "fields": fields}
        if next_page:
            body["nextPageToken"] = next_page
        s, data = jira("POST", "/rest/api/3/search/jql", body)
        if s != 200:
            print(f"  WARN search failed ({s}) — {data}")
            break
        page = data.get("issues", [])
        issues.extend(page)
        next_page = data.get("nextPageToken")
        if not next_page or not page:
            break
    return issues


# ═════════════════════════════════════════════════════════
#  1. FIX VERSION
# ═════════════════════════════════════════════════════════
VERSION_DEF = {
    "name": "R1.5 — Frontend E2E Completion",
    "description": "Complete all R1 frontend screens to enable full end-to-end testing. 4 vertical-slice sprints: Inventory, POS, Admin/Catalog, Reports/Dashboard.",
    "released": False,
    "releaseDate": "2026-05-01",
}

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


# ═════════════════════════════════════════════════════════
#  2. BOARD + SPRINTS
# ═════════════════════════════════════════════════════════
SPRINTS_DEF = [
    {
        "name": "R1.5 Sprint 4 Inventory",
        "goal": "Complete /inventory FE: adjustment modal, FIFO lot expand, dept filter, FX in receive. Dashboard stock alert widget + 60s auto-refresh. E2E: warehouse receives → adjusts → transfers → manager sees alerts.",
        "startDate": "2026-03-05T00:00:00.000Z",
        "endDate": "2026-03-19T23:59:59.000Z",
    },
    {
        "name": "R1.5 Sprint 5 POS",
        "goal": "Complete /pos FE: receipt modal + print, card terminal ref, end-of-day reconciliation. Dashboard revenue widget. E2E: cashier sale → receipt → EoD; manager sees revenue.",
        "startDate": "2026-03-19T00:00:00.000Z",
        "endDate": "2026-04-02T23:59:59.000Z",
    },
    {
        "name": "R1.5 Sprint 6 Admin",
        "goal": "Complete /admin (user edit, pwd reset, config, tabs), /departments (edit+counts), /products (edit+recipe link+filters), /recipes (dept, activate confirm, unit mode). E2E: admin manages users → configures tenant → manages catalog.",
        "startDate": "2026-04-02T00:00:00.000Z",
        "endDate": "2026-04-16T23:59:59.000Z",
    },
    {
        "name": "R1.5 Sprint 7 Reports",
        "goal": "Complete /reports (date range, dept filter, CSV, material consumption, cost/batch), /technologist, /dashboard spec-exact widgets, /production-plans approve dialog + yield input. E2E: full production cycle → reports → export.",
        "startDate": "2026-04-16T00:00:00.000Z",
        "endDate": "2026-04-30T23:59:59.000Z",
    },
]

# Sprint name → list of story IDs assigned to that sprint
SPRINT_STORY_MAP = {
    "Sprint 4": ["BC-1501", "BC-1502", "BC-1503", "BC-1504", "BC-1505"],
    "Sprint 5": ["BC-1601", "BC-1602", "BC-1603", "BC-1604"],
    "Sprint 6": ["BC-1701", "BC-1702", "BC-1703", "BC-1704", "BC-1705", "BC-1706", "BC-1707"],
    "Sprint 7": ["BC-1801", "BC-1802", "BC-1803", "BC-1804", "BC-1805", "BC-1806", "BC-1807"],
}

def get_board_id():
    s, data = jira("GET", f"/rest/agile/1.0/board?projectKeyOrId={JIRA_PROJECT}")
    if s != 200 or not data.get("values"):
        print(f"  WARN: no board found for {JIRA_PROJECT} ({s}) — sprints will be skipped")
        return None
    board_id = data["values"][0]["id"]
    print(f"  Board id={board_id}  name={data['values'][0]['name']}")
    return board_id


def ensure_sprints(board_id):
    if not board_id:
        return {}
    s, data = jira("GET", f"/rest/agile/1.0/board/{board_id}/sprint?maxResults=50")
    existing = {}
    if s == 200:
        for sp in data.get("values", []):
            existing[sp["name"]] = sp
    sprint_map = {}  # "Sprint 4" → sprint_id
    for i, sp_def in enumerate(SPRINTS_DEF):
        name = sp_def["name"]
        sprint_label = f"Sprint {4 + i}"
        if name in existing:
            sprint_map[sprint_label] = existing[name]["id"]
            print(f"  SKIP sprint: {name} (id={existing[name]['id']}, state={existing[name]['state']})")
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


# ═════════════════════════════════════════════════════════
#  3. ISSUE TYPE IDS
# ═════════════════════════════════════════════════════════
def get_issue_type_ids():
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    types = {it["name"].lower(): it["id"] for it in data.get("issueTypes", [])}
    return types


# ═════════════════════════════════════════════════════════
#  4. CREATE EPICS
# ═════════════════════════════════════════════════════════
R15_EPICS = [e for e in EPICS if e.get("release") == "R1.5"]

def load_existing_epic_keys():
    issues = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Epic",
        ["summary"],
    )
    epic_map = {}
    for iss in issues:
        summary = iss["fields"]["summary"]
        for ep in EPICS:
            if summary.startswith(f"[{ep['id']}]"):
                epic_map[ep["id"]] = iss["key"]
                break
    return epic_map


def create_epics(issue_type_ids, version_id):
    epic_type_id = issue_type_ids.get("epic")
    existing_map = load_existing_epic_keys()
    created = {}

    for ep in R15_EPICS:
        if ep["id"] in existing_map:
            created[ep["id"]] = existing_map[ep["id"]]
            print(f"  SKIP epic: {ep['id']} (exists: {existing_map[ep['id']]})")
            continue
        if DRY_RUN:
            print(f"  DRY  epic: [{ep['id']}] {ep['title'][:50]}")
            created[ep["id"]] = f"DRY_{ep['id']}"
            continue

        description = {
            "type": "doc", "version": 1,
            "content": [
                {"type": "paragraph", "content": [{"type": "text", "text": f"Goal: {ep['goal']}"}]},
                {"type": "paragraph", "content": [{"type": "text", "text": f"Requirements: {ep['requirements']}"}]},
                {"type": "paragraph", "content": [{"type": "text", "text": f"Release: {ep['release']}"}]},
            ],
        }
        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "summary": f"[{ep['id']}] {ep['title']}",
                "issuetype": {"id": epic_type_id},
                "description": description,
            }
        }
        if version_id and not str(version_id).startswith("DRY"):
            body["fields"]["fixVersions"] = [{"id": str(version_id)}]

        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            created[ep["id"]] = d2["key"]
            print(f"  CREATE epic: {d2['key']} ← [{ep['id']}] {ep['title'][:50]}")
        else:
            print(f"  FAIL epic: [{ep['id']}] ({s2}) — {json.dumps(d2)[:120]}")

    return created


# ═════════════════════════════════════════════════════════
#  5. CREATE STORIES
# ═════════════════════════════════════════════════════════
R15_STORIES = [s for s in STORIES if s.get("release") == "R1.5"]

def create_stories(issue_type_ids, epic_key_map, version_id):
    # Fetch existing to avoid duplication
    existing = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Story",
        ["summary"],
    )
    existing_ids = set()
    for iss in existing:
        summary = iss["fields"]["summary"]
        for st in R15_STORIES:
            if summary.startswith(f"[{st['id']}]"):
                existing_ids.add(st["id"])

    story_type_id = issue_type_ids.get("story")
    created = {}

    for story in R15_STORIES:
        if story["id"] in existing_ids:
            print(f"  SKIP story: {story['id']} (exists)")
            created[story["id"]] = "EXISTING"
            continue
        if DRY_RUN:
            print(f"  DRY  story: [{story['id']}] {story['title'][:55]}")
            created[story["id"]] = f"DRY_{story['id']}"
            continue

        ac_text = "\n".join(f"• {ac}" for ac in story.get("acceptance_criteria", []))
        description = {
            "type": "doc", "version": 1,
            "content": [
                {"type": "paragraph", "content": [{"type": "text", "text": f"Epic: {story['epic_id']}  |  Sprint: {story.get('sprint', '?')}  |  Release: R1.5  |  Priority: {story['priority']}"}]},
                {"type": "heading", "attrs": {"level": 3}, "content": [{"type": "text", "text": "Acceptance Criteria"}]},
                {"type": "paragraph", "content": [{"type": "text", "text": ac_text}]},
            ],
        }

        epic_key = epic_key_map.get(story["epic_id"])
        priority_name = {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low"}.get(story["priority"], "Medium")

        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "summary": f"[{story['id']}] {story['title']}",
                "issuetype": {"id": story_type_id},
                "description": description,
                "priority": {"name": priority_name},
            }
        }
        if version_id and not str(version_id).startswith("DRY"):
            body["fields"]["fixVersions"] = [{"id": str(version_id)}]
        if epic_key and not str(epic_key).startswith("DRY"):
            body["fields"]["customfield_10014"] = epic_key  # Epic Link

        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            created[story["id"]] = d2["key"]
            print(f"  CREATE story: {d2['key']} ← [{story['id']}] {story['title'][:45]}")
        else:
            print(f"  FAIL story: [{story['id']}] ({s2}) — {json.dumps(d2)[:120]}")

    return created


# ═════════════════════════════════════════════════════════
#  6. ASSIGN STORIES TO SPRINTS
# ═════════════════════════════════════════════════════════
def assign_to_sprints(sprint_map, created_story_keys):
    if DRY_RUN:
        for sprint_label, story_ids in SPRINT_STORY_MAP.items():
            print(f"  DRY  {sprint_label}: would assign {len(story_ids)} stories")
        return

    # Fetch all project stories to find the JIRA keys for our story IDs
    all_issues = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Story",
        ["summary"],
    )

    # Build story_id → jira_key lookup
    story_to_jira = {}
    for iss in all_issues:
        summary = iss["fields"]["summary"]
        for story in R15_STORIES:
            if summary.startswith(f"[{story['id']}]"):
                story_to_jira[story["id"]] = iss["key"]
                break

    for sprint_label, story_ids in SPRINT_STORY_MAP.items():
        sprint_id = sprint_map.get(sprint_label)
        if not sprint_id or str(sprint_id).startswith("DRY"):
            continue
        keys = [story_to_jira[sid] for sid in story_ids if sid in story_to_jira]
        if not keys:
            print(f"  SKIP {sprint_label}: no stories found in JIRA")
            continue
        s2, d2 = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}/issue", {"issues": keys})
        if s2 in (200, 204):
            print(f"  {sprint_label} ({sprint_id}): assigned {len(keys)} stories")
        else:
            print(f"  FAIL sprint assign {sprint_label}: {s2} — {d2}")


# ═════════════════════════════════════════════════════════
#  MAIN
# ═════════════════════════════════════════════════════════
def main():
    print("=" * 64)
    print("  BreadCost — R1.5 Frontend E2E Sprint Setup")
    print(f"  Project : {JIRA_PROJECT}")
    print(f"  Mode    : {'DRY RUN — nothing will be created' if DRY_RUN else '*** LIVE ***'}")
    print(f"  Epics   : {len(R15_EPICS)}")
    print(f"  Stories : {len(R15_STORIES)}")
    print("=" * 64)

    if not DRY_RUN:
        print("\nThis will create 1 version, 4 sprints, 4 epics, and 21 stories in JIRA.")
        confirm = input("Type YES to proceed: ").strip()
        if confirm != "YES":
            print("Aborted.")
            sys.exit(0)

    # 1. Project info
    print("\n── Project ────────────────────────────────────────")
    s, project = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    if s != 200:
        print(f"ERROR: project {JIRA_PROJECT} not found — {project}")
        sys.exit(1)
    project_id = project["id"]
    print(f"  {project['key']} · {project['name']} · id={project_id}")

    # 2. Fix Version
    print("\n── Fix Version ────────────────────────────────────")
    version_id = ensure_version(project_id)

    # 3. Board + Sprints
    print("\n── Board & Sprints ────────────────────────────────")
    board_id = get_board_id()
    sprint_map = ensure_sprints(board_id)
    print(f"  Sprint map: {sprint_map}")

    # 4. Issue type IDs
    print("\n── Issue Types ────────────────────────────────────")
    issue_type_ids = get_issue_type_ids()
    print(f"  Available: {list(issue_type_ids.keys())}")

    # 5. Create Epics
    print(f"\n── Creating {len(R15_EPICS)} R1.5 Epics ─────────────────────")
    epic_key_map = create_epics(issue_type_ids, version_id)

    # 6. Create Stories
    print(f"\n── Creating {len(R15_STORIES)} R1.5 Stories ───────────────────")
    created_keys = create_stories(issue_type_ids, epic_key_map, version_id)

    # 7. Assign stories to sprints
    print("\n── Sprint Assignments ─────────────────────────────")
    assign_to_sprints(sprint_map, created_keys)

    # 8. Summary
    print("\n" + "=" * 64)
    print("  SUMMARY")
    print("=" * 64)
    print(f"  Version: {VERSION_DEF['name']}")
    print(f"  Sprints: {len(SPRINTS_DEF)}")
    for i, sp in enumerate(SPRINTS_DEF):
        label = f"Sprint {4+i}"
        n = len(SPRINT_STORY_MAP.get(label, []))
        print(f"    {sp['name']}")
        print(f"      → {n} stories, dates: {sp['startDate'][:10]} → {sp['endDate'][:10]}")
    print(f"  Epics created: {sum(1 for v in epic_key_map.values() if not str(v).startswith('DRY'))}")
    print(f"  Stories created: {sum(1 for v in created_keys.values() if not str(v).startswith('DRY'))}")
    print()
    if DRY_RUN:
        print("  ⚠  This was a DRY RUN. Run with --run to create in JIRA.")


if __name__ == "__main__":
    main()
