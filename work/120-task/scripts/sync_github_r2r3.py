"""
sync_github_r2r3.py — Create GitHub issues for all R2/R3 stories.
Uses same label system as sync_github.py; skips already-existing issues.
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

# Fetch existing issue titles (open + closed)
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

# Ensure new labels exist
NEW_LABELS = {
    "customer-portal": "0075ca",
    "loyalty":         "e4e669",
    "suppliers":       "d93f0b",
    "procurement":     "f9d0c4",
    "delivery":        "1d76db",
    "invoicing":       "0e8a16",
    "subscriptions":   "5319e7",
    "integrations":    "c5def5",
    "whatsapp":        "bfd4f2",
    "forecasting":     "fef2c0",
    "sprint:r1":       "c2e0c6",
    "sprint:r2":       "bfd4f2",
    "sprint:r3":       "fef2c0",
}

def ensure_new_labels():
    s, existing = gh("GET", "/labels?per_page=100")
    existing_names = {l["name"] for l in (existing if s == 200 else [])}
    for name, color in NEW_LABELS.items():
        if name in existing_names:
            continue
        if DRY_RUN:
            print(f"  DRY  label: {name}")
            continue
        s2, _ = gh("POST", "/labels", {"name": name, "color": color})
        if s2 == 201:
            print(f"  CREATE label: {name}")
        else:
            print(f"  SKIP label: {name} ({s2})")

EPIC_TITLE = {e["id"]: e["title"] for e in EPICS}
PRIORITY_MAP = {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low"}

def story_labels(story):
    release = story.get("release") or next(
        (e["release"].split(",")[0].strip() for e in EPICS if e["id"] == story["epic_id"]), "R1"
    )
    labels = [
        f"release:{release.lower()}",
        f"sprint:{release.lower()}",
        f"priority:{story['priority'].lower()}",
        "status:planned",
        "type:story",
        f"epic:{story['epic_id'].lower()}",
    ]
    for lab in story.get("labels", []):
        labels.append(f"domain:{lab}")
    return labels

def story_body(story):
    release = story.get("release") or next(
        (e["release"] for e in EPICS if e["id"] == story["epic_id"]), "R1"
    )
    epic_title = EPIC_TITLE.get(story["epic_id"], story["epic_id"])
    ac_lines = "\n".join(f"- [ ] {ac}" for ac in story.get("acceptance_criteria", []))
    return (
        f"**Epic:** {story['epic_id']} — {epic_title}  \n"
        f"**Release:** {release}  \n"
        f"**Priority:** {PRIORITY_MAP.get(story['priority'], story['priority'])}  \n"
        f"**Status:** {story.get('status', 'Planned')}\n\n"
        f"## Acceptance Criteria\n\n{ac_lines}\n"
    )

def main():
    print("=" * 60)
    print(f"  GitHub R2/R3 Issues Sync")
    print(f"  Repo : {GITHUB_OWNER}/{GITHUB_REPO}")
    print(f"  Mode : {'DRY RUN' if DRY_RUN else '*** LIVE ***'}")
    print("=" * 60)

    if not DRY_RUN:
        confirm = input("\nType YES to proceed: ").strip()
        if confirm != "YES":
            sys.exit(0)

    print("\nFetching existing issues...")
    existing_titles = get_existing_titles()
    print(f"  {len(existing_titles)} issues already exist")

    print("\nEnsuring new labels...")
    ensure_new_labels()

    new_stories = [s for s in STORIES if s.get("release") in ("R2", "R3")]
    print(f"\nCreating {len(new_stories)} R2/R3 story issues...")

    created, skipped = 0, 0
    for story in new_stories:
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

    print(f"\nDone. Created {created} / skipped {skipped} / total {len(new_stories)}")

if __name__ == "__main__":
    main()
