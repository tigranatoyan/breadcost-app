#!/usr/bin/env python3
"""
sync_github_r15.py — Create GitHub issues for all R1.5 stories + epics.
Mirrors the JIRA R1.5 sprint structure as GitHub Issues with labels.

Run:
    python sync_github_r15.py          # dry-run
    python sync_github_r15.py --run    # live

Requires config.py with GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN.
"""
import sys, json, urllib.request, urllib.error
from config import GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN
from data import EPICS, STORIES

DRY_RUN = "--run" not in sys.argv

_HDRS = {
    "Authorization": "Bearer " + GITHUB_TOKEN,
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "Content-Type": "application/json",
}


def gh(method, path, body=None):
    data = json.dumps(body).encode() if body else None
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}{path}"
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
            return e.code, {}


# ── Existing issues ──────────────────────────────────────
def get_existing_titles():
    titles = set()
    page = 1
    while True:
        s, issues = gh("GET", f"/issues?state=all&per_page=100&page={page}")
        if s != 200 or not issues:
            break
        for iss in issues:
            titles.add(iss.get("title", ""))
        if len(issues) < 100:
            break
        page += 1
    return titles


# ── Labels ───────────────────────────────────────────────
R15_LABELS = {
    "release:r1.5":    ("fbca04", "Release 1.5 — Frontend E2E Completion"),
    "sprint:4":        ("c2e0c6", "Sprint 4 — Inventory & Warehouse E2E"),
    "sprint:5":        ("bfd4f2", "Sprint 5 — POS & Sales E2E"),
    "sprint:6":        ("fef2c0", "Sprint 6 — Admin, Config & Catalog E2E"),
    "sprint:7":        ("f9d0c4", "Sprint 7 — Reports, Dashboard & Production Polish E2E"),
    "type:epic":       ("6f42c1", "Overarching epic"),
    "type:story":      ("0075ca", "User story"),
    "status:planned":  ("c5def5", "Planned for a future sprint"),
    "priority:p0":     ("b60205", "P0 — Highest (must have)"),
    "priority:p1":     ("e11d48", "P1 — High"),
    "priority:p2":     ("f97316", "P2 — Medium"),
    "domain:frontend": ("1d76db", "Frontend implementation"),
    "domain:inventory":   ("006b75", "Domain: inventory"),
    "domain:dashboard":   ("0075ca", "Domain: dashboard"),
    "domain:pos":         ("e4e669", "Domain: pos"),
    "domain:admin":       ("5319e7", "Domain: admin"),
    "domain:users":       ("5319e7", "Domain: users"),
    "domain:config":      ("5319e7", "Domain: config"),
    "domain:departments": ("c5def5", "Domain: departments"),
    "domain:products":    ("c5def5", "Domain: products"),
    "domain:recipes":     ("c5def5", "Domain: recipes"),
    "domain:reports":     ("0e8a16", "Domain: reports"),
    "domain:technologist":("0e8a16", "Domain: technologist"),
    "domain:production":  ("d93f0b", "Domain: production"),
}

# Epic label colors
EPIC_COLORS = {
    "BC-E25": "006b75",
    "BC-E26": "e4e669",
    "BC-E27": "5319e7",
    "BC-E28": "0e8a16",
}


def ensure_labels():
    s, existing = gh("GET", "/labels?per_page=100")
    existing_names = {l["name"] for l in (existing if s == 200 else [])}

    # R1.5 labels
    for name, (color, desc) in R15_LABELS.items():
        if name in existing_names:
            continue
        if DRY_RUN:
            print(f"  DRY  label: {name}")
            continue
        s2, _ = gh("POST", "/labels", {"name": name, "color": color, "description": desc[:100]})
        if s2 == 201:
            print(f"  CREATE label: {name}")
        else:
            print(f"  SKIP label: {name} ({s2})")

    # Epic-specific labels
    for epic_id, color in EPIC_COLORS.items():
        label_name = f"epic:{epic_id.lower()}"
        if label_name in existing_names:
            continue
        if DRY_RUN:
            print(f"  DRY  label: {label_name}")
            continue
        s2, _ = gh("POST", "/labels", {"name": label_name, "color": color, "description": f"Epic {epic_id}"})
        if s2 == 201:
            print(f"  CREATE label: {label_name}")
        else:
            print(f"  SKIP label: {label_name} ({s2})")


# ── Helpers ──────────────────────────────────────────────
EPIC_TITLE = {e["id"]: e["title"] for e in EPICS}
PRIORITY_MAP = {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low"}

R15_EPICS = [e for e in EPICS if e.get("release") == "R1.5"]
R15_STORIES = [s for s in STORIES if s.get("release") == "R1.5"]

SPRINT_LABEL_MAP = {
    "Sprint 4": "sprint:4",
    "Sprint 5": "sprint:5",
    "Sprint 6": "sprint:6",
    "Sprint 7": "sprint:7",
}


def story_labels(story):
    labels = [
        "release:r1.5",
        f"priority:{story['priority'].lower()}",
        "status:planned",
        "type:story",
        f"epic:{story['epic_id'].lower()}",
        "domain:frontend",
    ]
    sprint = story.get("sprint", "")
    if sprint in SPRINT_LABEL_MAP:
        labels.append(SPRINT_LABEL_MAP[sprint])
    for lab in story.get("labels", []):
        dl = f"domain:{lab}"
        if dl in R15_LABELS:
            labels.append(dl)
    return labels


def story_body(story):
    epic_title = EPIC_TITLE.get(story["epic_id"], story["epic_id"])
    ac_lines = "\n".join(f"- [ ] {ac}" for ac in story.get("acceptance_criteria", []))
    return (
        f"**Epic:** {story['epic_id']} — {epic_title}  \n"
        f"**Sprint:** {story.get('sprint', '?')}  \n"
        f"**Release:** R1.5  \n"
        f"**Priority:** {PRIORITY_MAP.get(story['priority'], story['priority'])}  \n"
        f"**Status:** Planned\n\n"
        f"## Acceptance Criteria\n\n{ac_lines}\n"
    )


def epic_body(epic):
    return (
        f"**Release:** {epic['release']}  \n"
        f"**Requirements:** {epic['requirements']}  \n\n"
        f"## Goal\n\n{epic['goal']}\n"
    )


def epic_labels(epic):
    labels = [
        "release:r1.5",
        "type:epic",
        f"epic:{epic['id'].lower()}",
    ]
    for lab in epic.get("labels", []):
        dl = f"domain:{lab}"
        if dl in R15_LABELS:
            labels.append(dl)
    return labels


# ── Main ─────────────────────────────────────────────────
def main():
    print("=" * 64)
    print(f"  GitHub R1.5 Issues Sync")
    print(f"  Repo : {GITHUB_OWNER}/{GITHUB_REPO}")
    print(f"  Mode : {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
    print(f"  Epics: {len(R15_EPICS)}  Stories: {len(R15_STORIES)}")
    print("=" * 64)

    if not DRY_RUN:
        confirm = input("\nType YES to proceed: ").strip()
        if confirm != "YES":
            sys.exit(0)

    print("\nFetching existing issues...")
    existing_titles = get_existing_titles()
    print(f"  {len(existing_titles)} issues already exist")

    print("\nEnsuring labels...")
    ensure_labels()

    # Create epic issues
    print(f"\nCreating {len(R15_EPICS)} epic issues...")
    for epic in R15_EPICS:
        title = f"[{epic['id']}] {epic['title']}"
        if title in existing_titles:
            print(f"  SKIP  {title[:60]}")
            continue
        if DRY_RUN:
            print(f"  DRY   {title[:60]}")
            continue
        s, d = gh("POST", "/issues", {
            "title": title,
            "body": epic_body(epic),
            "labels": epic_labels(epic),
        })
        if s == 201:
            print(f"  CREATE #{d['number']}  {title[:55]}")
        else:
            print(f"  FAIL  {title[:55]} ({s})")

    # Create story issues
    print(f"\nCreating {len(R15_STORIES)} story issues...")
    created, skipped = 0, 0
    for story in R15_STORIES:
        title = f"[{story['id']}] {story['title']}"
        if title in existing_titles:
            print(f"  SKIP  {title[:60]}")
            skipped += 1
            continue
        if DRY_RUN:
            print(f"  DRY   {title[:60]}")
            continue
        s, d = gh("POST", "/issues", {
            "title": title,
            "body": story_body(story),
            "labels": story_labels(story),
        })
        if s == 201:
            print(f"  CREATE #{d['number']}  {title[:55]}")
            created += 1
        else:
            print(f"  FAIL  {title[:55]} ({s})")

    print(f"\nDone. Created {created} / skipped {skipped} / total {len(R15_STORIES)}")


if __name__ == "__main__":
    main()
