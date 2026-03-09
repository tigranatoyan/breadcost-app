#!/usr/bin/env python3
"""
setup_r3fe_sprints.py — Push R3-FE stories to JIRA and mark them Done.
======================================================================
R3-FE — Frontend for R3: 2 epics, 7 stories, 2 sprints.

Sprint 8 — AI Dashboards & Exchange Rates (BC-E30)
Sprint 9 — Driver, Mobile Admin & Supplier API (BC-E31)

Run:
    python setup_r3fe_sprints.py          # dry-run
    python setup_r3fe_sprints.py --run    # live
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

# Filter R3-FE epics & stories
R3FE_EPICS = [e for e in EPICS if e.get("release") == "R3-FE"]
R3FE_STORIES = [s for s in STORIES if s.get("release") == "R3-FE"]

def get_issue_type_ids():
    s, data = jira("GET", f"/rest/api/3/issuetype")
    if s == 200:
        return {t["name"].lower(): t["id"] for t in data}
    return {}

def get_version_id():
    s, data = jira("GET", f"/rest/api/3/project/{JIRA_PROJECT}/versions")
    if s != 200:
        return None
    for v in (data if isinstance(data, list) else []):
        if "R3" in v["name"]:
            return v["id"]
    return None

def get_transitions(issue_key):
    s, data = jira("GET", f"/rest/api/3/issue/{issue_key}/transitions")
    if s != 200:
        return {}
    return {t["name"].lower(): t["id"] for t in data.get("transitions", [])}

def main():
    mode = "DRY-RUN" if DRY_RUN else "LIVE"
    print(f"\n  R3-FE JIRA Setup ({mode})")
    print(f"  Epics: {len(R3FE_EPICS)}, Stories: {len(R3FE_STORIES)}\n")

    issue_types = get_issue_type_ids()
    version_id = get_version_id()
    print(f"  Version id: {version_id}")

    # Check existing issues
    s, issues = jira("POST", "/rest/api/3/search/jql", {
        "jql": f"project={JIRA_PROJECT} AND (issuetype=Epic OR issuetype=Story)",
        "fields": ["summary"],
        "maxResults": 500,
    })
    existing = {}
    for iss in (issues.get("issues", []) if s == 200 else []):
        summary = iss["fields"]["summary"]
        if "[" in summary and "]" in summary:
            code = summary.split("]")[0].strip("[")
            existing[code] = iss["key"]

    # Create epics
    epic_map = {}
    for epic in R3FE_EPICS:
        eid = epic["id"]
        if eid in existing:
            epic_map[eid] = existing[eid]
            print(f"  SKIP epic: {eid} ({existing[eid]})")
            continue
        if DRY_RUN:
            print(f"  DRY  epic: [{eid}] {epic['title'][:50]}")
            epic_map[eid] = f"DRY_{eid}"
            continue
        body = {
            "fields": {
                "project": {"key": JIRA_PROJECT},
                "issuetype": {"id": issue_types.get("epic", "10000")},
                "summary": f"[{eid}] {epic['title']}",
                "description": {
                    "type": "doc", "version": 1,
                    "content": [{"type": "paragraph", "content": [{"type": "text", "text": epic.get("goal", epic["title"])}]}],
                },
                "fixVersions": [{"id": version_id}] if version_id else [],
            }
        }
        s2, d2 = jira("POST", "/rest/api/3/issue", body)
        if s2 == 201:
            epic_map[eid] = d2["key"]
            print(f"  CREATE epic: {d2['key']} ← [{eid}]")
        else:
            print(f"  FAIL epic: [{eid}] ({s2})")

    # Create stories + mark Done
    for story in R3FE_STORIES:
        sid = story["id"]
        if sid in existing:
            jira_key = existing[sid]
            print(f"  EXISTS story: {sid} → {jira_key}")
        elif DRY_RUN:
            print(f"  DRY  story: [{sid}] {story['title'][:50]}")
            continue
        else:
            ac_text = "\n".join(f"- {ac}" for ac in story.get("acceptance_criteria", []))
            epic_jira_key = epic_map.get(story["epic_id"])
            fields = {
                "project": {"key": JIRA_PROJECT},
                "issuetype": {"id": issue_types.get("story", "10001")},
                "summary": f"[{sid}] {story['title']}",
                "description": {
                    "type": "doc", "version": 1,
                    "content": [{"type": "paragraph", "content": [{"type": "text", "text": ac_text}]}],
                },
                "labels": story.get("labels", []),
                "fixVersions": [{"id": version_id}] if version_id else [],
            }
            if epic_jira_key and not epic_jira_key.startswith("DRY_"):
                fields["parent"] = {"key": epic_jira_key}
            s2, d2 = jira("POST", "/rest/api/3/issue", {"fields": fields})
            if s2 == 201:
                jira_key = d2["key"]
                print(f"  CREATE story: {jira_key} ← [{sid}]")
            else:
                print(f"  FAIL story: [{sid}] ({s2}) — {d2}")
                continue

        # Transition to Done
        if not DRY_RUN:
            transitions = get_transitions(jira_key)
            done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
            if done_id:
                jira("POST", f"/rest/api/3/issue/{jira_key}/transitions", {"transition": {"id": done_id}})
                print(f"    → Done ✓")
            else:
                # May need to go through In Progress first
                ip_id = transitions.get("in progress") or transitions.get("start progress")
                if ip_id:
                    jira("POST", f"/rest/api/3/issue/{jira_key}/transitions", {"transition": {"id": ip_id}})
                    transitions = get_transitions(jira_key)
                    done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
                    if done_id:
                        jira("POST", f"/rest/api/3/issue/{jira_key}/transitions", {"transition": {"id": done_id}})
                        print(f"    → In Progress → Done ✓")
                    else:
                        print(f"    → transitions: {list(transitions.keys())}")
                else:
                    print(f"    → transitions: {list(transitions.keys())}")

    print("\nDone.\n")

if __name__ == "__main__":
    main()
