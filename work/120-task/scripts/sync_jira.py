#!/usr/bin/env python3
"""
BreadCost — JIRA Sync Script
Creates all epics and stories from data.py into your JIRA project.

Usage:
    python sync_jira.py            # dry run (config.py DRY_RUN=True)
    python sync_jira.py --run      # actually create issues
    python sync_jira.py --epics    # only create epics
    python sync_jira.py --stories  # only create stories (epics must exist first)

Before running:
    1. Fill in config.py with your credentials
    2. pip install -r requirements.txt
"""

import sys
import json
import time
import base64
import argparse
import urllib.request
import urllib.error
from data import EPICS, STORIES, PRIORITY_MAP, STATUS_MAP

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT, DRY_RUN
except ImportError:
    print("ERROR: config.py not found. Copy config.py and fill in your credentials.")
    sys.exit(1)


# ── Auth ─────────────────────────────────────────────────────
def _auth_header() -> str:
    token = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
    return f"Basic {token}"


def _request(method: str, path: str, body: dict | None = None) -> dict:
    url = f"{JIRA_BASE_URL}/rest/api/3{path}"
    headers = {
        "Authorization": _auth_header(),
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body_text = e.read().decode()
        print(f"  HTTP {e.code} {method} {path}")
        print(f"  Response: {body_text[:500]}")
        raise


# ── Ensure project exists (create if missing) ────────────────
def get_my_account_id() -> str:
    """Returns the accountId of the authenticated user."""
    result = _request("GET", "/myself")
    return result["accountId"]


def ensure_project(dry_run: bool) -> None:
    """Creates the JIRA project if it does not already exist.
    If a project named BreadCost already exists with a different key, prints its key and exits.
    """
    try:
        _request("GET", f"/project/{JIRA_PROJECT}")
        print(f"  Project '{JIRA_PROJECT}' already exists — skipping creation.")
        return
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise

    # Project key not found — check if a BreadCost project exists with a different key
    print(f"  Project key '{JIRA_PROJECT}' not found — searching for existing BreadCost project...")
    try:
        all_projects = _request("GET", "/project/search?maxResults=100")
        for project in all_projects.get("values", []):
            if "breadcost" in project.get("name", "").lower():
                existing_key = project["key"]
                print(f"\n  Found existing project: '{project['name']}' with key '{existing_key}'")
                print(f"  Update config.py:  JIRA_PROJECT = \"{existing_key}\"")
                print(f"  Then re-run: python sync_jira.py --run")
                sys.exit(0)
    except Exception:
        pass

    # No existing project found — create it
    if dry_run:
        print(f"  DRY RUN | Would create project: {JIRA_PROJECT} — BreadCost")
        return

    print(f"  Creating project '{JIRA_PROJECT}'...")
    account_id = get_my_account_id()
    body = {
        "key": JIRA_PROJECT,
        "name": "BreadCost",
        "projectTypeKey": "software",
        "projectTemplateKey": "com.pyxis.greenhopper.jira:gh-scrum-template",
        "description": "BreadCost — Bakery Management System",
        "leadAccountId": account_id,
        "assigneeType": "UNASSIGNED",
    }
    try:
        result = _request("POST", "/project", body)
        print(f"  Created project: {result['key']} — {result.get('name', '')}")
    except urllib.error.HTTPError as e:
        print(f"  Scrum template failed ({e.code}), trying team-managed project...")
        body["projectTemplateKey"] = "com.atlassian.jira-core-project-templates:jira-core-simplified-project-management"
        body["projectTypeKey"] = "business"
        try:
            result = _request("POST", "/project", body)
            print(f"  Created project (team-managed): {result['key']} — {result.get('name', '')}")
        except urllib.error.HTTPError as e2:
            print(f"  FAILED to create project: {e2.code} — {e2.read().decode()[:300]}")
            print(f"\n  Go to: https://tigranatoyan80.atlassian.net/jira/software/projects")
            print(f"  Find your BreadCost project, check its key, and set JIRA_PROJECT to that key in config.py")
            sys.exit(1)


# ── Fetch project metadata ────────────────────────────────────
def get_issue_type_ids() -> dict:
    """Returns {name_lower: id} for available issue types in the project."""
    result = _request("GET", f"/project/{JIRA_PROJECT}")
    issue_types = {}
    for it in result.get("issueTypes", []):
        issue_types[it["name"].lower()] = it["id"]
    return issue_types


def get_priority_ids() -> dict:
    """Returns {name_lower: id} for available priorities."""
    result = _request("GET", "/priority")
    return {p["name"].lower(): p["id"] for p in result}


# ── Build JIRA issue body ─────────────────────────────────────
def build_epic_body(epic: dict, issue_type_id: str) -> dict:
    ac_text = f"{epic['goal']}\n\nRequirements: {epic['requirements']}\nRelease: {epic['release']}"
    return {
        "fields": {
            "project": {"key": JIRA_PROJECT},
            "issuetype": {"id": issue_type_id},
            "summary": f"[{epic['id']}] {epic['title']}",
            "description": {
                "type": "doc",
                "version": 1,
                "content": [
                    {
                        "type": "paragraph",
                        "content": [{"type": "text", "text": ac_text}],
                    }
                ],
            },
            "labels": epic.get("labels", []),
        }
    }


def build_story_body(
    story: dict,
    issue_type_id: str,
    epic_jira_key: str | None,
    priority_id: str | None,
) -> dict:
    ac_lines = "\n".join(f"- {ac}" for ac in story["acceptance_criteria"])
    description_text = f"Acceptance Criteria:\n{ac_lines}"

    fields = {
        "project": {"key": JIRA_PROJECT},
        "issuetype": {"id": issue_type_id},
        "summary": f"[{story['id']}] {story['title']}",
        "description": {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [{"type": "text", "text": description_text}],
                }
            ],
        },
        "labels": story.get("labels", []),
    }

    if priority_id:
        fields["priority"] = {"id": priority_id}

    # Link to parent epic — try `parent` (next-gen) then customfield_10014 (classic)
    if epic_jira_key:
        fields["parent"] = {"key": epic_jira_key}

    return {"fields": fields}


# ── Main sync logic ───────────────────────────────────────────
def sync_epics(issue_type_ids: dict, dry_run: bool) -> dict:
    """Creates all epics. Returns {epic_id -> jira_key} mapping."""
    epic_type_id = issue_type_ids.get("epic")
    if not epic_type_id:
        print("WARNING: 'Epic' issue type not found. Using 'Story' as fallback.")
        epic_type_id = issue_type_ids.get("story") or list(issue_type_ids.values())[0]

    existing = load_existing_epics() if not dry_run else {}
    created = dict(existing)  # pre-seed with already-known epics

    to_create = [e for e in EPICS if e["id"] not in existing]
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Creating {len(to_create)} new epics (skipping {len(existing)} existing)...")

    for epic in to_create:
        body = build_epic_body(epic, epic_type_id)
        summary = body["fields"]["summary"]

        if dry_run:
            print(f"  DRY RUN | Would create epic: {summary}")
            created[epic["id"]] = f"BC-DRY-{epic['id']}"
        else:
            try:
                result = _request("POST", "/issue", body)
                jira_key = result["key"]
                print(f"  Created  epic: {jira_key}  <- {summary}")
                created[epic["id"]] = jira_key
                time.sleep(0.3)  # be gentle with the API
            except Exception as e:
                print(f"  FAILED   epic: {summary} -- {e}")

    return created


def sync_stories(
    issue_type_ids: dict,
    priority_ids: dict,
    epic_key_map: dict,
    dry_run: bool,
    only_epic: str | None = None,
) -> None:
    """Creates all stories, linked to their parent epic."""
    story_type_id = issue_type_ids.get("story")
    if not story_type_id:
        print("WARNING: 'Story' issue type not found. Using first available type.")
        story_type_id = list(issue_type_ids.values())[0]

    existing_stories = load_existing_stories() if not dry_run else set()
    all_stories = STORIES if not only_epic else [s for s in STORIES if s["epic_id"] == only_epic]
    stories = [s for s in all_stories if s["id"] not in existing_stories]
    skipped = len(all_stories) - len(stories)
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Creating {len(stories)} new stories (skipping {skipped} existing)...")

    for story in stories:
        epic_jira_key = epic_key_map.get(story["epic_id"])
        p_name = PRIORITY_MAP.get(story["priority"], "Medium").lower()
        priority_id = priority_ids.get(p_name)

        body = build_story_body(story, story_type_id, epic_jira_key, priority_id)
        summary = body["fields"]["summary"]

        if dry_run:
            print(f"  DRY RUN | Would create story: {summary}  (epic: {epic_jira_key})")
        else:
            try:
                result = _request("POST", "/issue", body)
                jira_key = result["key"]
                print(f"  Created  story: {jira_key}  <- {summary}")
                time.sleep(0.3)
            except Exception as e:
                print(f"  FAILED   story: {summary} -- {e}")


def _search_all(jql: str, fields: list[str]) -> list[dict]:
    """Paginate through all JIRA search results using nextPageToken."""
    import urllib.parse
    all_issues = []
    body: dict = {
        "jql": jql,
        "maxResults": 100,
        "fields": fields,
        "nextPageToken": None,
    }
    while True:
        if body["nextPageToken"] is None:
            del body["nextPageToken"]
        result = _request("POST", "/search/jql", body)
        issues = result.get("issues", [])
        all_issues.extend(issues)
        token = result.get("nextPageToken")
        if not token or not issues:
            break
        body["nextPageToken"] = token
    return all_issues


def load_existing_epics() -> dict:
    """Returns {BC-E01: 'BC-2', ...} for all non-duplicate epics in JIRA."""
    print("  Fetching existing epics from JIRA...")
    issues = _search_all(
        f'project = {JIRA_PROJECT} AND issuetype = Epic AND labels != duplicate ORDER BY created ASC',
        ["summary", "key"]
    )
    mapping = {}
    for issue in issues:
        summary = issue["fields"]["summary"]
        if summary.startswith("[BC-E"):
            bc_id = summary[1:summary.index("]")]
            mapping[bc_id] = issue["key"]
    print(f"  Found {len(mapping)} existing epics.")
    return mapping


def load_existing_stories() -> set:
    """Returns a set of BC story IDs already present in JIRA (non-duplicate)."""
    print("  Fetching existing stories from JIRA...")
    issues = _search_all(
        f'project = {JIRA_PROJECT} AND issuetype = Story AND labels != duplicate ORDER BY created ASC',
        ["summary"]
    )
    existing = set()
    for issue in issues:
        summary = issue["fields"]["summary"]
        if summary.startswith("["):
            try:
                bc_id = summary[1:summary.index("]")]
                if bc_id.startswith("BC-"):
                    existing.add(bc_id)
            except ValueError:
                pass
    print(f"  Found {len(existing)} existing stories.")
    return existing


# ── Entry point ───────────────────────────────────────────────
def main():
    import urllib.parse  # needed for load_existing_epics

    parser = argparse.ArgumentParser(description="Sync BreadCost JIRA issues")
    parser.add_argument("--run",     action="store_true", help="Actually create issues (overrides DRY_RUN in config)")
    parser.add_argument("--epics",   action="store_true", help="Only create epics")
    parser.add_argument("--stories", action="store_true", help="Only create stories (epics must exist)")
    args = parser.parse_args()

    dry_run = DRY_RUN and not args.run

    print("=" * 60)
    print(f"  BreadCost JIRA Sync")
    print(f"  Project  : {JIRA_PROJECT}")
    print(f"  JIRA URL : {JIRA_BASE_URL}")
    print(f"  Mode     : {'DRY RUN — nothing will be created' if dry_run else '*** LIVE — issues will be created ***'}")
    print("=" * 60)

    if not dry_run:
        confirm = input("\nType YES to proceed with live creation: ")
        if confirm.strip().upper() != "YES":
            print("Aborted.")
            sys.exit(0)

    # Ensure project exists
    print("\nChecking project...")
    ensure_project(dry_run)

    # Fetch metadata
    print("\nFetching project metadata...")
    issue_type_ids = get_issue_type_ids() if not dry_run else {"epic": "10000", "story": "10001"}
    priority_ids   = get_priority_ids()   if not dry_run else {"highest": "1", "high": "2", "medium": "3", "low": "4"}
    print(f"  Issue types available: {list(issue_type_ids.keys())}")

    only_stories = args.stories and not args.epics
    only_epics   = args.epics   and not args.stories

    # ── Create epics (or load existing ones for stories-only mode)
    if not only_stories:
        epic_key_map = sync_epics(issue_type_ids, dry_run)
    else:
        epic_key_map = load_existing_epics() if not dry_run else {}

    # ── Create stories
    if not only_epics:
        sync_stories(issue_type_ids, priority_ids, epic_key_map, dry_run)

    print("\nDone.")


if __name__ == "__main__":
    import urllib.parse
    main()
