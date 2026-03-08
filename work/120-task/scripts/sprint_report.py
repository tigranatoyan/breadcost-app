#!/usr/bin/env python3
"""
sprint_report.py  —  JIRA Sprint Cycle-Time Report

Reads the changelog of every issue in a sprint and reports:
  • Time spent in each status   (To Do / In Progress / Done)
  • Cycle time (first "In Progress" → Done)
  • Lead time  (created → Done)

Usage:
    python sprint_report.py              # Sprint 2 (R2) — default
    python sprint_report.py --sprint 5   # explicit sprint id
    python sprint_report.py --sprint all # all sprints
    python sprint_report.py --csv        # append CSV output
"""

import sys
import json
import base64
import urllib.request
import urllib.error
import argparse
from datetime import datetime, timezone
from collections import defaultdict

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

DEFAULT_SPRINT_ID = 5  # Sprint 2 · R2 — Growth


# ── HTTP helpers ──────────────────────────────────────────────────────────────

def _auth() -> dict:
    t = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
    return {
        "Authorization": f"Basic {t}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }


def _get(path: str) -> dict:
    url = f"{JIRA_BASE_URL}{path}"
    req = urllib.request.Request(url, headers=_auth())
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"  HTTP {e.code} GET {path}: {e.read().decode()[:200]}")
        return {}


def _post(path: str, body: dict) -> dict:
    url = f"{JIRA_BASE_URL}{path}"
    data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers=_auth(), method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"  HTTP {e.code} POST {path}: {e.read().decode()[:200]}")
        return {}


# ── Data fetching ─────────────────────────────────────────────────────────────

def get_sprint_issues(sprint_id: int) -> list[dict]:
    """Return all issues in a sprint (paginated)."""
    issues = []
    start = 0
    while True:
        data = _get(f"/rest/agile/1.0/sprint/{sprint_id}/issue"
                    f"?startAt={start}&maxResults=100"
                    f"&fields=summary,status,created,resolutiondate,issuetype,labels")
        chunk = data.get("issues", [])
        issues.extend(chunk)
        if start + len(chunk) >= data.get("total", 0):
            break
        start += len(chunk)
    return issues


def get_all_sprint_issues(project: str) -> list[dict]:
    """Return issues across ALL sprints in the project."""
    data = _get(f"/rest/agile/1.0/board/2/sprint?state=active,closed,future")
    sprint_ids = [s["id"] for s in data.get("values", [])]
    all_issues = []
    for sid in sprint_ids:
        all_issues.extend(get_sprint_issues(sid))
    return all_issues


def get_changelog(issue_key: str) -> list[dict]:
    """Fetch the full changelog for an issue (paginated)."""
    entries = []
    start = 0
    while True:
        data = _get(f"/rest/api/3/issue/{issue_key}/changelog?startAt={start}&maxResults=100")
        values = data.get("values", [])
        entries.extend(values)
        if start + len(values) >= data.get("total", 0):
            break
        start += len(values)
    return entries


# ── Time-in-status calculation ────────────────────────────────────────────────

def _parse_ts(s: str) -> datetime | None:
    if not s:
        return None
    # JIRA timestamps: "2026-03-04T12:34:56.000+0400"
    for fmt in ("%Y-%m-%dT%H:%M:%S.%f%z", "%Y-%m-%dT%H:%M:%S%z"):
        try:
            return datetime.strptime(s, fmt)
        except ValueError:
            pass
    return None


def calc_time_in_status(issue: dict, changelog: list[dict]) -> dict:
    """
    Walk the status-change history and return a dict of:
        { "To Do": <minutes>, "In Progress": <minutes>, "Done": <minutes>, ... }

    Also computes:
        "cycle_minutes"  — first In Progress → Done (or now if not Done yet)
        "lead_minutes"   — created → Done (or now)
        "first_in_progress_at" — datetime or None
        "done_at"        — datetime or None
    """
    now = datetime.now(tz=timezone.utc)
    created_at = _parse_ts(issue["fields"].get("created")) or now

    # Build ordered list of (timestamp, from_status, to_status) for status changes
    transitions: list[tuple[datetime, str, str]] = []
    for entry in changelog:
        ts = _parse_ts(entry.get("created"))
        if ts is None:
            continue
        for item in entry.get("items", []):
            if item.get("field") == "status":
                frm = item.get("fromString", "")
                to  = item.get("toString", "")
                transitions.append((ts, frm, to))

    transitions.sort(key=lambda x: x[0])

    # Determine starting status and its start time
    if transitions:
        first_status = transitions[0][1]  # fromString of first transition
        first_start  = created_at
    else:
        first_status = issue["fields"]["status"]["name"]
        first_start  = created_at

    time_in: dict[str, float] = defaultdict(float)
    current_status = first_status
    current_start  = first_start
    first_in_progress_at = None
    done_at = None

    for ts, frm, to in transitions:
        delta_minutes = max(0.0, (ts - current_start).total_seconds() / 60)
        time_in[current_status] += delta_minutes

        if to.lower() in ("in progress",) and first_in_progress_at is None:
            first_in_progress_at = ts
        if to.lower() == "done":
            done_at = ts

        current_status = to
        current_start  = ts

    # Accumulate time in final status up to now (or done_at)
    end = done_at if done_at else now
    delta_minutes = max(0.0, (end - current_start).total_seconds() / 60)
    time_in[current_status] += delta_minutes

    # Cycle time
    if first_in_progress_at and done_at:
        cycle_minutes = (done_at - first_in_progress_at).total_seconds() / 60
    elif first_in_progress_at:
        cycle_minutes = (now - first_in_progress_at).total_seconds() / 60
    else:
        cycle_minutes = None

    # Lead time
    lead_minutes = ((done_at or now) - created_at).total_seconds() / 60

    return {
        "time_in": dict(time_in),
        "cycle_minutes": cycle_minutes,
        "lead_minutes": lead_minutes,
        "first_in_progress_at": first_in_progress_at,
        "done_at": done_at,
    }


# ── Formatting helpers ────────────────────────────────────────────────────────

def _fmt_dur(minutes: float | None) -> str:
    if minutes is None:
        return "  --   "
    h = int(minutes // 60)
    m = int(minutes % 60)
    if h >= 24:
        d = h // 24
        h = h % 24
        return f"{d}d {h:02d}:{m:02d}"
    return f"{h:02d}:{m:02d}"


def _bar(pct: float, width: int = 20) -> str:
    filled = int(pct * width / 100)
    return chr(9608) * filled + chr(9617) * (width - filled)


# ── Report ────────────────────────────────────────────────────────────────────

STATUS_ORDER = ["To Do", "In Progress", "Done"]
COL_W = 11


def print_report(sprint_id: int | str, issues: list[dict], csv_mode: bool = False):
    sprint_label = f"Sprint {sprint_id}" if isinstance(sprint_id, int) else "All Sprints"

    print()
    print("=" * 90)
    print(f"  BreadCost -- Sprint Cycle-Time Report   [{sprint_label}]   {datetime.now():%Y-%m-%d %H:%M}")
    print("=" * 90)

    rows = []
    status_counts: dict[str, int] = defaultdict(int)

    for issue in issues:
        labels = issue["fields"].get("labels", [])
        if "duplicate" in labels:
            continue
        if issue["fields"]["issuetype"]["name"].lower() == "epic":
            continue

        key      = issue["key"]
        summary  = issue["fields"]["summary"]
        cur_stat = issue["fields"]["status"]["name"]

        # Extract BC-XXXX from summary like "[BC-1101] ..."
        bc_id = ""
        if summary.startswith("[BC-"):
            try:
                bc_id = summary[1:summary.index("]")]
            except ValueError:
                bc_id = key

        changelog = get_changelog(key)
        stats = calc_time_in_status(issue, changelog)

        rows.append({
            "key": key,
            "bc_id": bc_id or key,
            "title": summary.split("]", 1)[-1].strip()[:40],
            "status": cur_stat,
            "stats": stats,
        })
        status_counts[cur_stat] += 1

    # ── Header
    hdr  = f"  {'BC-ID':<12} {'Title':<42}"
    hdr += f" {'To Do':>{COL_W}} {'In Prog':>{COL_W}} {'Done':>{COL_W}}"
    hdr += f" {'Cycle':>{COL_W}} {'Lead':>{COL_W}}  Status"
    print(hdr)
    print("  " + "-" * 86)

    total_cycle = []
    total_lead  = []

    for r in rows:
        ti    = r["stats"]["time_in"]
        cyc   = r["stats"]["cycle_minutes"]
        lead  = r["stats"]["lead_minutes"]

        todo_m  = ti.get("To Do", 0)
        inp_m   = ti.get("In Progress", 0)
        done_m  = ti.get("Done", 0)

        if cyc is not None:
            total_cycle.append(cyc)
        total_lead.append(lead)

        status_sym = {"To Do": "  ", "In Progress": ">> ", "Done": "OK "}.get(r["status"], "?  ")

        line  = f"  {r['bc_id']:<12} {r['title']:<42}"
        line += f" {_fmt_dur(todo_m):>{COL_W}} {_fmt_dur(inp_m):>{COL_W}} {_fmt_dur(done_m):>{COL_W}}"
        line += f" {_fmt_dur(cyc):>{COL_W}} {_fmt_dur(lead):>{COL_W}}  {status_sym}{r['status']}"
        print(line)

    # ── Summary
    total = len(rows)
    done  = status_counts.get("Done", 0)
    wip   = status_counts.get("In Progress", 0)
    todo  = status_counts.get("To Do", 0)

    print()
    print("  " + "-" * 86)
    print(f"  Total issues : {total}")
    print(f"  Done         : {done}  ({int(done/total*100) if total else 0}%)")
    print(f"  In Progress  : {wip}")
    print(f"  To Do        : {todo}")

    if total_cycle:
        avg_cyc = sum(total_cycle) / len(total_cycle)
        print(f"  Avg cycle time (In Progress -> Done) : {_fmt_dur(avg_cyc)}")
    if total_lead:
        avg_lead = sum(total_lead) / len(total_lead)
        print(f"  Avg lead time  (Created -> Done)     : {_fmt_dur(avg_lead)}")

    # ── Throughput bar
    print()
    print(f"  Throughput   [{_bar(int(done/total*100) if total else 0)}] {done}/{total}")
    print()

    # ── CSV output
    if csv_mode:
        csv_path = "sprint_report.csv"
        import csv, io
        out = io.StringIO()
        w = csv.writer(out)
        w.writerow(["bc_id", "jira_key", "title", "status",
                    "todo_min", "inprog_min", "done_min",
                    "cycle_min", "lead_min",
                    "first_in_progress_at", "done_at"])
        for r in rows:
            ti = r["stats"]["time_in"]
            w.writerow([
                r["bc_id"], r["key"], r["title"], r["status"],
                round(ti.get("To Do", 0), 1),
                round(ti.get("In Progress", 0), 1),
                round(ti.get("Done", 0), 1),
                round(r["stats"]["cycle_minutes"] or 0, 1),
                round(r["stats"]["lead_minutes"] or 0, 1),
                r["stats"]["first_in_progress_at"] or "",
                r["stats"]["done_at"] or "",
            ])
        with open(csv_path, "w", newline="", encoding="utf-8") as f:
            f.write(out.getvalue())
        print(f"  CSV written -> {csv_path}")
        print()


# ── Entry point ───────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="JIRA Sprint Cycle-Time Report")
    parser.add_argument("--sprint", default=str(DEFAULT_SPRINT_ID),
                        help=f"Sprint ID (default: {DEFAULT_SPRINT_ID}) or 'all'")
    parser.add_argument("--csv", action="store_true", help="Also write sprint_report.csv")
    args = parser.parse_args()

    if args.sprint.lower() == "all":
        issues = get_all_sprint_issues(JIRA_PROJECT)
        print_report("All", issues, csv_mode=args.csv)
    else:
        sprint_id = int(args.sprint)
        print(f"  Fetching Sprint {sprint_id} issues...", end=" ", flush=True)
        issues = get_sprint_issues(sprint_id)
        print(f"{len(issues)} found.")
        print_report(sprint_id, issues, csv_mode=args.csv)


if __name__ == "__main__":
    main()
