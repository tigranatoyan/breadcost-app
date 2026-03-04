#!/usr/bin/env python3
"""
workflow.py — Story-by-story development workflow helper.

Usage:
    python workflow.py start  BC-201  # Creates branch, moves JIRA → In Progress
    python workflow.py done   BC-201  # Moves JIRA → Done, closes GitHub issue
    python workflow.py list   r1      # Lists all stories in a release with status
    python workflow.py list   BC-E02  # Lists all stories in an epic
    python workflow.py status         # Shows overall R1/R2/R3 progress
"""
import sys, os, subprocess, urllib.request, urllib.error, json, base64
from config import (
    JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT,
    GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN,
)
from data import STORIES, EPICS

# ── helpers ─────────────────────────────────────────────────

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


def gh_req(method, path, body=None):
    hdrs = {
        "Authorization": "Bearer " + GITHUB_TOKEN,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
    }
    data = json.dumps(body).encode() if body else None
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}{path}"
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, json.loads(raw) if raw else {}


def git(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True, cwd=os.path.join(os.path.dirname(__file__), "../../.."))
    return result.returncode, result.stdout.strip(), result.stderr.strip()


def slug(title):
    """Convert title to git branch slug."""
    import re
    s = title.lower()
    s = re.sub(r"[^a-z0-9\s-]", "", s)
    s = re.sub(r"\s+", "-", s.strip())
    s = re.sub(r"-+", "-", s)
    return s[:40]


def find_jira_issue(story_id):
    """Find the JIRA issue key for a BC story ID like 'BC-201'.

    Note: JQL ~ operator treats [ ] as Lucene range syntax, so we search for
    the plain ID and then verify the summary in Python.
    """
    s, data = jira_req(
        "POST",
        "/rest/api/3/search/jql",
        {
            "jql": f'project={JIRA_PROJECT} AND issuetype=Story AND summary ~ "{story_id}" ORDER BY created ASC',
            "maxResults": 20,
            "fields": ["summary", "status", "labels"],
        },
    )
    if s == 200:
        for issue in data.get("issues", []):
            labels = issue["fields"].get("labels", [])
            if "duplicate" in labels:
                continue
            if issue["fields"]["summary"].startswith(f"[{story_id}]"):
                return issue
    return None


def get_transitions(jira_key):
    _, data = jira_req("GET", f"/rest/api/3/issue/{jira_key}/transitions")
    return {t["name"].lower(): t["id"] for t in data.get("transitions", [])}


def find_gh_issue(story_id):
    """Find open GitHub issue for a story."""
    _, issues = gh_req("GET", f"/issues?state=open&per_page=100")
    for iss in (issues or []):
        if iss.get("title", "").startswith(f"[{story_id}]"):
            return iss
    return None


# ── commands ────────────────────────────────────────────────

def cmd_start(story_id):
    """Start a story: create branch, update JIRA to In Progress."""
    story = next((s for s in STORIES if s["id"] == story_id), None)
    if not story:
        print(f"ERROR: Story {story_id} not found in data.py")
        sys.exit(1)

    branch = f"feature/{story_id.lower()}-{slug(story['title'])}"
    print(f"\n▶  Starting {story_id}: {story['title']}")
    print(f"   Branch  : {branch}")

    # Git: create and push branch
    rc, out, err = git(f"git checkout -b {branch}")
    if rc != 0 and "already exists" not in err:
        print(f"   Git error: {err}")
        sys.exit(1)
    rc, out, err = git(f"git push breadcost {branch}")
    if rc != 0:
        print(f"   Git push warning: {err}")

    # JIRA: move to In Progress
    jira_issue = find_jira_issue(story_id)
    if jira_issue:
        jira_key = jira_issue["key"]
        transitions = get_transitions(jira_key)
        in_prog_id = transitions.get("in progress") or transitions.get("start progress")
        if in_prog_id:
            jira_req("POST", f"/rest/api/3/issue/{jira_key}/transitions", {"transition": {"id": in_prog_id}})
            print(f"   JIRA    : {jira_key} → In Progress ✓")
        else:
            print(f"   JIRA    : {jira_key} — no 'In Progress' transition found (transitions: {list(transitions.keys())})")
    else:
        print(f"   JIRA    : issue not found for {story_id}")

    # GitHub: add 'status:in-progress' label
    gh_issue = find_gh_issue(story_id)
    if gh_issue:
        gh_req("POST", f"/issues/{gh_issue['number']}/labels", {"labels": ["status:in-progress"]})
        print(f"   GitHub  : #{gh_issue['number']} labelled in-progress ✓")

    print(f"\n   Checkin : git checkout {branch}")
    print(f"   Commit  : git commit -m \"feat({story_id.lower()}): <description>\"")
    print()


def cmd_done(story_id, commit_ref=None):
    """Complete a story: update JIRA to Done, close GitHub issue."""
    story = next((s for s in STORIES if s["id"] == story_id), None)
    if not story:
        print(f"ERROR: Story {story_id} not found in data.py")
        sys.exit(1)

    print(f"\n✅  Completing {story_id}: {story['title']}")

    # Get latest commit if not provided
    if not commit_ref:
        _, sha, _ = git("git rev-parse --short HEAD")
        commit_ref = sha or "HEAD"

    # JIRA → Done
    jira_issue = find_jira_issue(story_id)
    if jira_issue:
        jira_key = jira_issue["key"]
        transitions = get_transitions(jira_key)
        done_id = transitions.get("done") or transitions.get("mark as done") or transitions.get("resolve issue")
        if done_id:
            jira_req("POST", f"/rest/api/3/issue/{jira_key}/transitions", {"transition": {"id": done_id}})
            print(f"   JIRA    : {jira_key} → Done ✓")
        else:
            print(f"   JIRA    : {jira_key} — transitions: {list(transitions.keys())}")
    else:
        print(f"   JIRA    : issue not found for {story_id}")

    # GitHub: comment + close
    gh_issue = find_gh_issue(story_id)
    if gh_issue:
        num = gh_issue["number"]
        ac_text = "\n".join(f"- [x] {ac}" for ac in story.get("acceptance_criteria", []))
        comment = (
            f"✅ **Completed** — {story_id}: {story['title']}\n\n"
            f"**Acceptance Criteria:**\n{ac_text}\n\n"
            f"Commit: `{GITHUB_OWNER}/{GITHUB_REPO}@{commit_ref}`"
        )
        gh_req("POST", f"/issues/{num}/comments", {"body": comment})
        gh_req("PATCH", f"/issues/{num}", {"state": "closed", "state_reason": "completed"})
        print(f"   GitHub  : #{num} closed ✓")

    print()


def cmd_list(filter_arg):
    """List stories filtered by release (r1/r2/r3) or epic (BC-E01 etc.)."""
    filt = filter_arg.lower()

    if filt in ("r1", "r2", "r3"):
        release_upper = filt.upper()
        epic_release = {e["id"]: e.get("release", "R1").split(",")[0].strip() for e in EPICS}
        subset = [
            s for s in STORIES
            if (s.get("release") or epic_release.get(s.get("epic_id", ""), "R1")) == release_upper
        ]
        print(f"\nStories in Release {release_upper} ({len(subset)}):\n")
    else:
        epic_id = filter_arg.upper()
        subset = [s for s in STORIES if s.get("epic_id") == epic_id]
        epic = next((e for e in EPICS if e["id"] == epic_id), None)
        title = epic["title"] if epic else epic_id
        print(f"\nStories in {epic_id}: {title} ({len(subset)}):\n")

    status_icon = {"✅ Done": "✅", "🔄 In Progress": "🔄", "📋 Planned": "📋", "": "📋"}
    for s in subset:
        icon = status_icon.get(s.get("status", ""), "📋")
        print(f"  {icon}  {s['id']:<10}  P{s['priority'][1]}  {s['title']}")
    print()


def cmd_status():
    """Show R1/R2/R3 overall progress."""
    # Build a release map: story_id → effective release (story.release overrides epic.release)
    epic_release = {e["id"]: e.get("release", "R1").split(",")[0].strip() for e in EPICS}
    print("\n  BreadCost -- Release Progress\n")
    for release in ("R1", "R2", "R3"):
        stories = [
            s for s in STORIES
            if (s.get("release") or epic_release.get(s.get("epic_id", ""), "R1")) == release
        ]
        done = sum(1 for s in stories if s.get("status") == "✅ Done")
        total = len(stories)
        bar_done = int(done / total * 40) if total else 0
        bar_todo = 40 - bar_done
        bar = chr(9608) * bar_done + chr(9617) * bar_todo
        pct = int(done / total * 100) if total else 0
        print(f"  {release}  [{bar}] {done}/{total} ({pct}%)")
    print()


# ── main ────────────────────────────────────────────────────

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)

    command = sys.argv[1].lower()

    if command == "start" and len(sys.argv) >= 3:
        cmd_start(sys.argv[2].upper())
    elif command == "done" and len(sys.argv) >= 3:
        commit = sys.argv[3] if len(sys.argv) >= 4 else None
        cmd_done(sys.argv[2].upper(), commit)
    elif command == "list" and len(sys.argv) >= 3:
        cmd_list(sys.argv[2])
    elif command == "status":
        cmd_status()
    else:
        print(__doc__)
        sys.exit(1)
