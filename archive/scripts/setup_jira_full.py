"""
setup_jira_full.py
==================
Full JIRA project setup:
  1. Create Fix Versions  : R1 (released), R2 (planned), R3 (planned)
  2. Create Sprints       : Sprint 1Â.R1 (closed), Sprint 2Â.R2, Sprint 3Â.R3
  3. Create R2/R3 stories in JIRA
  4. Assign every story   â†’ sprint + fix version + epic link
  5. Assign R1 sprint â†’ closed (history preserved)

Run:
    python setup_jira_full.py --run
    python setup_jira_full.py          (dry-run, shows plan only)
"""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
from data import EPICS, STORIES

DRY_RUN = "--run" not in sys.argv

# â”€â”€ auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {
    "Authorization": "Basic " + _TOKEN,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

def jira(method, path, body=None, base=None):
    url = (base or JIRA_BASE_URL) + path
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
            print(f"  WARN search failed ({s}) â€” {data}")
            break
        page = data.get("issues", [])
        issues.extend(page)
        next_page = data.get("nextPageToken")
        if not next_page or not page:
            break
    return issues


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  1. GET PROJECT INFO
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def get_project():
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    if s != 200:
        print(f"ERROR: project {JIRA_PROJECT} not found â€” {data}")
        sys.exit(1)
    return data  # .id, .key, .name


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  2. FIX VERSIONS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
VERSIONS = [
    {"name": "R1 â€” Core MVP",      "description": "Core bakery management: orders, production, inventory, POS, reports, auth", "released": True,  "releaseDate": "2026-02-28"},
    {"name": "R2 â€” Growth",        "description": "Customer portal, loyalty, supplier management, delivery, B2B invoicing, advanced reports", "released": False, "releaseDate": "2026-06-30"},
    {"name": "R3 â€” AI + Mobile",   "description": "Full AI layer, driver app, exchange rate & supplier APIs, mobile customer app", "released": False, "releaseDate": "2026-10-31"},
]

def ensure_versions(project_id):
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}/versions")
    existing = {v["name"]: v["id"] for v in (data if s == 200 else [])}
    version_ids = {}
    for v in VERSIONS:
        if v["name"] in existing:
            version_ids[v["name"]] = existing[v["name"]]
            print(f"  SKIP version: {v['name']} (exists)")
            continue
        if DRY_RUN:
            print(f"  DRY  version: {v['name']}")
            version_ids[v["name"]] = f"DRY_{v['name']}"
            continue
        body = {"projectId": int(project_id), **v}
        s2, d2 = jira("POST", "/rest/api/3/version", body)
        if s2 == 201:
            version_ids[v["name"]] = d2["id"]
            print(f"  CREATE version: {v['name']} â†’ id={d2['id']}")
        else:
            print(f"  FAIL version: {v['name']} ({s2}) â€” {d2}")
    return version_ids


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  3. BOARD + SPRINTS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
SPRINTS_DEF = [
    {"name": "Sprint 1 Â. R1 â€” Core MVP",    "goal": "Deliver core bakery management: auth, orders, production, inventory, POS, reports",      "startDate": "2025-10-01T00:00:00.000Z", "endDate": "2026-02-28T23:59:59.000Z", "state_target": "closed", "release": "R1"},
    {"name": "Sprint 2 Â. R2 â€” Growth",      "goal": "Customer portal, loyalty, supplier workflow, delivery, B2B invoicing, advanced reports",  "startDate": "2026-03-05T00:00:00.000Z", "endDate": "2026-06-30T23:59:59.000Z", "state_target": "active", "release": "R2"},
    {"name": "Sprint 3 Â. R3 â€” AI + Mobile", "goal": "Full AI module, driver app, exchange rate & supplier APIs, mobile customer app",           "startDate": "2026-07-01T00:00:00.000Z", "endDate": "2026-10-31T23:59:59.000Z", "state_target": "future", "release": "R3"},
]

def get_board_id():
    s, data = jira("GET", f"/rest/agile/1.0/board?projectKeyOrId={JIRA_PROJECT}")
    if s != 200 or not data.get("values"):
        print(f"  WARN: no board found for {JIRA_PROJECT} ({s}) â€” sprints will be skipped")
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
    sprint_map = {}  # release â†’ sprint_id
    for sp_def in SPRINTS_DEF:
        name = sp_def["name"]
        if name in existing:
            sprint_map[sp_def["release"]] = existing[name]["id"]
            print(f"  SKIP sprint: {name} (id={existing[name]['id']}, state={existing[name]['state']})")
            continue
        if DRY_RUN:
            print(f"  DRY  sprint: {name} â†’ state_target={sp_def['state_target']}")
            sprint_map[sp_def["release"]] = f"DRY_{name}"
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
            sprint_map[sp_def["release"]] = sprint_id
            print(f"  CREATE sprint: {name} â†’ id={sprint_id}")
            # transition state
            target = sp_def["state_target"]
            if target in ("active", "closed"):
                s3, d3 = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}", {"state": "active", "startDate": sp_def["startDate"], "endDate": sp_def["endDate"]})
                if target == "closed" and s3 in (200, 204):
                    s4, d4 = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}", {"state": "closed"})
                    if s4 in (200, 204):
                        print(f"    â†’ closed (R1 history)")
                    else:
                        print(f"    â†’ close attempt: {s4} {d4}")
        else:
            print(f"  FAIL sprint: {name} ({s2}) â€” {d2}")
    return sprint_map


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  4. LOAD EXISTING EPICS (key map: BC-E01 â†’ BC-2 etc.)
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def load_epic_key_map():
    """Returns {BC-E01: 'BC-2', ...} mapping our IDs to JIRA issue keys."""
    issues = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Epic",
        ["summary", "key"],
    )
    epic_map = {}
    for iss in issues:
        summary = iss["fields"]["summary"]
        # summary format: "[BC-E01] Authentication & Authorization"
        for ep in EPICS:
            if summary.startswith(f"[{ep['id']}]"):
                epic_map[ep["id"]] = iss["key"]
                break
    return epic_map


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  5. GET ISSUE TYPE IDs
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def get_issue_type_ids():
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}")
    types = {it["name"].lower(): it["id"] for it in data.get("issueTypes", [])}
    return types


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  6. CREATE R2/R3 STORIES IN JIRA
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def create_stories(new_stories, issue_type_ids, epic_key_map, version_ids):
    """Create stories that don't yet exist in JIRA. Returns {story_id: jira_key}."""
    # Fetch existing stories to avoid duplication
    existing = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Story",
        ["summary"],
    )
    existing_ids = set()
    for iss in existing:
        summary = iss["fields"]["summary"]
        for s in new_stories:
            if summary.startswith(f"[{s['id']}]"):
                existing_ids.add(s["id"])

    story_type_id = issue_type_ids.get("story")
    created = {}

    for story in new_stories:
        if story["id"] in existing_ids:
            print(f"  SKIP  story {story['id']} (exists)")
            continue
        if DRY_RUN:
            print(f"  DRY   story {story['id']}: {story['title'][:50]}")
            continue

        release = story.get("release") or next(
            (e["release"] for e in EPICS if e["id"] == story["epic_id"]), "R2"
        )
        version_name = next((v["name"] for v in VERSIONS if v["name"].startswith(release)), None)
        fix_version_id = version_ids.get(version_name) if version_name else None
        epic_key = epic_key_map.get(story["epic_id"])

        ac_text = "\n".join(f"â€¢ {ac}" for ac in story.get("acceptance_criteria", []))
        description = {
            "type": "doc", "version": 1,
            "content": [
                {"type": "paragraph", "content": [{"type": "text", "text": f"Epic: {story['epic_id']}  |  Release: {release}  |  Priority: {story['priority']}"}]},
                {"type": "heading", "attrs": {"level": 3}, "content": [{"type": "text", "text": "Acceptance Criteria"}]},
                {"type": "paragraph", "content": [{"type": "text", "text": ac_text}]},
            ],
        }

        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "summary": f"[{story['id']}] {story['title']}",
                "issuetype": {"id": story_type_id},
                "description": description,
                "priority": {"name": {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low"}.get(story["priority"], "Medium")},
            }
        }
        if fix_version_id and not str(fix_version_id).startswith("DRY"):
            body["fields"]["fixVersions"] = [{"id": str(fix_version_id)}]
        if epic_key:
            body["fields"]["customfield_10014"] = epic_key  # Epic Link

        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            jira_key = d2["key"]
            created[story["id"]] = jira_key
            print(f"  CREATE story: {jira_key} â† [{story['id']}] {story['title'][:45]}")
        else:
            print(f"  FAIL  story: [{story['id']}] ({s2}) â€” {json.dumps(d2)[:120]}")

    return created


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  7. ASSIGN FIX VERSIONS TO EXISTING R1 STORIES
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def assign_r1_versions(version_ids):
    r1_version_name = "R1 â€” Core MVP"
    r1_ver_id = version_ids.get(r1_version_name)
    if not r1_ver_id or str(r1_ver_id).startswith("DRY"):
        print("  SKIP R1 version assignment (no version id)")
        return
    existing = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Story AND fixVersion is EMPTY",
        ["summary", "fixVersions"],
    )
    r1_story_ids = {s["id"] for s in STORIES if not s.get("release") or s.get("release") == "R1"}
    updated = 0
    for iss in existing:
        summary = iss["fields"]["summary"]
        is_r1 = any(summary.startswith(f"[{sid}]") for sid in r1_story_ids)
        if not is_r1:
            continue
        if DRY_RUN:
            print(f"  DRY  fix version R1 â†’ {iss['key']}")
            continue
        s2, d2 = jira("PUT", f"/rest/api/3/issue/{iss['key']}", {"fields": {"fixVersions": [{"id": str(r1_ver_id)}]}})
        if s2 in (200, 204):
            updated += 1
        else:
            print(f"  FAIL fix version {iss['key']}: {s2}")
    if not DRY_RUN:
        print(f"  R1 fix version assigned to {updated} stories")


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  8. ASSIGN STORIES TO SPRINTS
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def assign_stories_to_sprints(sprint_map, created_story_keys):
    """Group JIRA issue keys by sprint and bulk-add them."""
    if not sprint_map or DRY_RUN:
        if DRY_RUN:
            print("  DRY  sprint assignments (skipped in dry-run)")
        return

    # Fetch all stories with their summaries
    all_issues = jira_search(
        f"project={JIRA_PROJECT} AND issuetype=Story",
        ["summary", "sprint"],
    )

    # Build lookup: story_id â†’ jira_key
    story_release_map = {}
    for story in STORIES:
        release = story.get("release") or next(
            (e["release"] for e in EPICS if e["id"] == story["epic_id"]), "R1"
        )
        # Only use base release (R1/R2/R3), strip plan variants
        base = release.split(",")[0].strip()
        story_release_map[story["id"]] = base

    # Map release â†’ list of JIRA keys
    release_keys = {"R1": [], "R2": [], "R3": []}
    for iss in all_issues:
        summary = iss["fields"]["summary"]
        for story in STORIES:
            if summary.startswith(f"[{story['id']}]"):
                r = story_release_map.get(story["id"], "R1")
                release_keys.get(r, release_keys["R1"]).append(iss["key"])
                break

    for release, keys in release_keys.items():
        sprint_id = sprint_map.get(release)
        if not sprint_id or str(sprint_id).startswith("DRY") or not keys:
            continue
        # Bulk add in batches of 50
        for i in range(0, len(keys), 50):
            batch = keys[i:i+50]
            s2, d2 = jira("POST", f"/rest/agile/1.0/sprint/{sprint_id}/issue", {"issues": batch})
            if s2 in (200, 204):
                print(f"  Sprint {release} ({sprint_id}): assigned {len(batch)} issues")
            else:
                print(f"  FAIL sprint assign {release}: {s2} â€” {d2}")


# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
#  MAIN
# â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
def main():
    print("=" * 60)
    print("  BreadCost â€” Full JIRA Setup")
    print(f"  Project : {JIRA_PROJECT}")
    print(f"  Mode    : {'DRY RUN â€” nothing will be created' if DRY_RUN else '*** LIVE ***'}")
    print("=" * 60)

    if not DRY_RUN:
        print("\nThis will create versions, sprints, and ~52 new JIRA stories.")
        confirm = input("Type YES to proceed: ").strip()
        if confirm != "YES":
            print("Aborted.")
            sys.exit(0)

    # 1. Project info
    print("\nâ”€â”€ Project info â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    project = get_project()
    project_id = project["id"]
    print(f"  {project['key']} Â. {project['name']} Â. id={project_id}")

    # 2. Fix Versions
    print("\nâ”€â”€ Fix Versions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    version_ids = ensure_versions(project_id)

    # 3. Board + Sprints
    print("\nâ”€â”€ Board & Sprints â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    board_id = get_board_id()
    sprint_map = ensure_sprints(board_id)
    print(f"  Sprint map: {sprint_map}")

    # 4. Issue type IDs
    print("\nâ”€â”€ Issue Types â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    issue_type_ids = get_issue_type_ids()
    print(f"  Available: {list(issue_type_ids.keys())}")

    # 5. Epic key map
    print("\nâ”€â”€ Epic Key Map â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    epic_key_map = load_epic_key_map()
    print(f"  Mapped {len(epic_key_map)} epics")
    if len(epic_key_map) < 5:
        for k, v in epic_key_map.items():
            print(f"    {k} â†’ {v}")

    # 6. Create R2/R3 stories
    new_stories = [s for s in STORIES if s.get("release") in ("R2", "R3")]
    print(f"\nâ”€â”€ Creating {len(new_stories)} R2/R3 stories â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    created_keys = create_stories(new_stories, issue_type_ids, epic_key_map, version_ids)
    print(f"  Created {len(created_keys)} new stories in JIRA")

    # 7. Assign R1 fix version to existing stories
    print("\nâ”€â”€ Fix Versions â†’ R1 stories â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    assign_r1_versions(version_ids)

    # 8. Assign stories to sprints
    print("\nâ”€â”€ Sprint Assignments â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
    assign_stories_to_sprints(sprint_map, created_keys)

    # Summary
    r1 = sum(1 for s in STORIES if not s.get("release") or s.get("release") == "R1")
    r2 = sum(1 for s in STORIES if s.get("release") == "R2")
    r3 = sum(1 for s in STORIES if s.get("release") == "R3")
    print("\n" + "=" * 52)
    print("  JIRA Setup Complete")
    print("  " + "-" * 48)
    print("  Versions  : R1 (released) / R2 (planned) / R3 (planned)")
    print("  Sprints   : Sprint 1-R1 (closed) / Sprint 2-R2 / Sprint 3-R3")
    print(f"  Stories   : R1={r1} (Done) / R2={r2} (To Do) / R3={r3} (To Do)")
    print(f"  Board URL : {JIRA_BASE_URL}/jira/software/projects/{JIRA_PROJECT}/boards")
    print("=" * 52)

if __name__ == "__main__":
    main()

