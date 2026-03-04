#!/usr/bin/env python3
"""
BreadCost — GitHub Sync Script
Creates GitHub Issues and Labels mirroring all JIRA epics and stories.

Usage:
    python sync_github.py           # dry run (config.py DRY_RUN=True)
    python sync_github.py --run     # actually create issues + labels
    python sync_github.py --labels  # only create/update labels

GitHub Issues will be created with:
  - Title: [BC-XXX] Story title
  - Body:  Acceptance criteria + epic reference + JIRA story ID
  - Labels: epic label, release label, domain labels, priority label

Before running:
    1. Fill in config.py with your credentials
    2. pip install -r requirements.txt
"""

import sys
import json
import time
import argparse
import urllib.request
import urllib.error
from data import EPICS, STORIES, PRIORITY_MAP

try:
    from config import GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN, DRY_RUN
except ImportError:
    print("ERROR: config.py not found. Copy config.py and fill in your credentials.")
    sys.exit(1)


# ── Label definitions ────────────────────────────────────────
# Format: (name, color_hex, description)
RELEASE_LABELS = [
    ("release:r1",  "0075ca", "Release 1 — Core MVP"),
    ("release:r2",  "e4e669", "Release 2 — Growth"),
    ("release:r3",  "d93f0b", "Release 3 — AI + Mobile"),
]

PRIORITY_LABELS = [
    ("priority:p0", "b60205", "P0 — Highest (must have)"),
    ("priority:p1", "e11d48", "P1 — High"),
    ("priority:p2", "f97316", "P2 — Medium"),
    ("priority:p3", "84cc16", "P3 — Low"),
]

STATUS_LABELS = [
    ("status:done",        "0e8a16", "Work is complete"),
    ("status:in-progress", "fbca04", "Currently being worked on"),
    ("status:planned",     "c5def5", "Planned for a future sprint"),
]

TYPE_LABELS = [
    ("type:epic",  "6f42c1", "Overarching epic"),
    ("type:story", "0075ca", "User story"),
]


def epic_label(epic_id: str) -> tuple:
    """Returns (name, color, description) for an epic label."""
    colors = [
        "1d76db", "0075ca", "e4e669", "d93f0b", "5319e7",
        "006b75", "e11d48", "0e8a16", "fbca04", "b60205",
        "84cc16", "f97316",
    ]
    idx = int(epic_id.replace("BC-E", "")) % len(colors)
    return (
        f"epic:{epic_id.lower()}",
        colors[idx],
        f"Epic {epic_id}",
    )


def domain_label(name: str) -> tuple:
    return (f"domain:{name}", "c5def5", f"Domain: {name}")


# ── GitHub API helpers ────────────────────────────────────────
def _request(method: str, path: str, body: dict | None = None) -> dict | list | None:
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}{path}"
    headers = {
        "Authorization": f"Bearer {GITHUB_TOKEN}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
    }
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            text = resp.read()
            return json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        body_text = e.read().decode()
        print(f"  HTTP {e.code} {method} {path}")
        print(f"  Response: {body_text[:500]}")
        raise


def get_existing_labels() -> set:
    """Returns set of existing label names in the repo."""
    labels = _request("GET", "/labels?per_page=100")
    return {lbl["name"] for lbl in (labels or [])}


# ── Create/update labels ─────────────────────────────────────
def ensure_labels(dry_run: bool) -> None:
    all_labels: list[tuple] = []

    all_labels.extend(RELEASE_LABELS)
    all_labels.extend(PRIORITY_LABELS)
    all_labels.extend(STATUS_LABELS)
    all_labels.extend(TYPE_LABELS)

    # One label per epic
    for epic in EPICS:
        all_labels.append(epic_label(epic["id"]))

    # Domain labels from story labels
    seen_domains = set()
    for story in STORIES:
        for lbl in story.get("labels", []):
            if lbl not in seen_domains:
                seen_domains.add(lbl)
                all_labels.append(domain_label(lbl))

    existing = get_existing_labels() if not dry_run else set()
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Ensuring {len(all_labels)} labels...")

    for name, color, description in all_labels:
        if dry_run:
            print(f"  DRY RUN | Would create label: #{color} {name}")
            continue
        if name in existing:
            # Update in case color/description changed
            _request("PATCH", f"/labels/{urllib.parse.quote(name)}", {
                "color": color, "description": description
            })
            print(f"  Updated  label: {name}")
        else:
            _request("POST", "/labels", {
                "name": name, "color": color, "description": description
            })
            print(f"  Created  label: {name}")
        time.sleep(0.1)


# ── Create issues ─────────────────────────────────────────────
def create_epic_issues(dry_run: bool) -> dict:
    """Creates GitHub Issues for each epic. Returns {epic_id -> issue_number}."""
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Creating {len(EPICS)} epic issues...")
    created = {}

    for epic in EPICS:
        release = epic["release"].lower().replace("-", "")  # r1, r2, r3
        labels = [
            "type:epic",
            f"epic:{epic['id'].lower()}",
            f"release:{release.split()[0]}",  # first release only
        ] + [f"domain:{lbl}" for lbl in epic.get("labels", []) if "r" not in lbl]

        body = (
            f"## {epic['id']} — {epic['title']}\n\n"
            f"**Goal:** {epic['goal']}\n\n"
            f"**Requirements:** {epic['requirements']}\n\n"
            f"**Release:** {epic['release']}\n\n"
            f"---\n_This issue was auto-created from `work/120-task/JIRA.md`._"
        )

        if dry_run:
            print(f"  DRY RUN | Would create epic issue: [{epic['id']}] {epic['title']}")
            created[epic["id"]] = 0
            continue

        try:
            result = _request("POST", "/issues", {
                "title": f"[{epic['id']}] {epic['title']}",
                "body": body,
                "labels": labels,
            })
            issue_number = result["number"]
            print(f"  Created  #%d ← [%s] %s" % (issue_number, epic["id"], epic["title"]))
            created[epic["id"]] = issue_number
            time.sleep(0.5)
        except Exception as e:
            print(f"  FAILED   epic: [{epic['id']}] {epic['title']} — {e}")

    return created


def create_story_issues(epic_issue_map: dict, dry_run: bool) -> None:
    """Creates GitHub Issues for each story."""
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Creating {len(STORIES)} story issues...")

    for story in STORIES:
        priority_label = f"priority:{story['priority'].lower()}"
        epic_id = story["epic_id"]

        # Find release from epic
        epic_data = next((e for e in EPICS if e["id"] == epic_id), None)
        release = "r1"
        if epic_data:
            r = epic_data["release"].lower()
            if "r2" in r:
                release = "r2"
            elif "r3" in r:
                release = "r3"

        labels = [
            "type:story",
            f"epic:{epic_id.lower()}",
            f"release:{release}",
            priority_label,
        ] + [f"domain:{lbl}" for lbl in story.get("labels", [])]

        # Status label
        status_raw = story.get("status", "")
        if "Done" in status_raw:
            labels.append("status:done")
        elif "Progress" in status_raw:
            labels.append("status:in-progress")
        else:
            labels.append("status:planned")

        # Build body
        ac_md = "\n".join(f"- [ ] {ac}" for ac in story["acceptance_criteria"])
        epic_ref = epic_issue_map.get(epic_id)
        epic_link = f"#{epic_ref}" if epic_ref else epic_id

        body = (
            f"## {story['id']} — {story['title']}\n\n"
            f"**Epic:** {epic_link}\n\n"
            f"### Acceptance Criteria\n{ac_md}\n\n"
            f"---\n_This issue was auto-created from `work/120-task/JIRA.md`._"
        )

        if dry_run:
            print(f"  DRY RUN | Would create story: [{story['id']}] {story['title']}")
            continue

        try:
            result = _request("POST", "/issues", {
                "title": f"[{story['id']}] {story['title']}",
                "body": body,
                "labels": labels,
            })
            issue_number = result["number"]
            print(f"  Created  #%d ← [%s] %s" % (issue_number, story["id"], story["title"]))
            time.sleep(0.5)
        except Exception as e:
            print(f"  FAILED   story: [{story['id']}] {story['title']} — {e}")


# ── Entry point ───────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Sync BreadCost GitHub Issues")
    parser.add_argument("--run",    action="store_true", help="Actually create issues")
    parser.add_argument("--labels", action="store_true", help="Only create/update labels")
    args = parser.parse_args()

    dry_run = DRY_RUN and not args.run

    print("=" * 60)
    print(f"  BreadCost GitHub Sync")
    print(f"  Repo  : {GITHUB_OWNER}/{GITHUB_REPO}")
    print(f"  Mode  : {'DRY RUN — nothing will be created' if dry_run else '*** LIVE — issues will be created ***'}")
    print("=" * 60)

    if not dry_run:
        confirm = input("\nType YES to proceed with live creation: ")
        if confirm.strip().upper() != "YES":
            print("Aborted.")
            sys.exit(0)

    # Always ensure labels first
    ensure_labels(dry_run)

    if not args.labels:
        epic_issue_map = create_epic_issues(dry_run)
        create_story_issues(epic_issue_map, dry_run)

    print("\nDone.")


if __name__ == "__main__":
    import urllib.parse
    main()
