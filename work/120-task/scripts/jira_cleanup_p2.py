#!/usr/bin/env python3
"""jira_cleanup_p2.py — Handle items that couldn't be deleted (403/401):
  - Archive duplicate R2 version instead of deleting
  - Label + resolve duplicate stories/epics since we can't delete them
"""
import sys, json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN

DRY = "--run" not in sys.argv
_TOKEN = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
_HDRS = {"Authorization": "Basic " + _TOKEN, "Accept": "application/json",
         "Content-Type": "application/json"}

VER_R2X = "10036"  # duplicate R2 version (0 stories)
DUP_STORIES = [f"BC-{i}" for i in range(161, 218)]
DUP_EPICS   = [f"BC-{i}" for i in range(137, 161)]
DONE_TRANSITION = "31"

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
        try: return e.code, json.loads(raw)
        except: return e.code, {"raw": raw.decode()[:300]}

ok = fail = 0
def log(tag, msg):
    global ok, fail
    if tag == "OK": ok += 1
    elif tag == "FAIL": fail += 1
    prefix = "  DRY " if DRY else f"  {tag:4s}"
    print(f"{prefix} {msg}")

def main():
    print("=" * 65)
    print(f"  JIRA CLEANUP PHASE 2 — {'DRY RUN' if DRY else 'LIVE RUN'}")
    print("=" * 65)

    # ── 1. Archive duplicate R2 version ──
    print("\n── 1. Archive duplicate R2 version ──")
    if DRY:
        log("DRY", f"ARCHIVE version {VER_R2X}")
    else:
        s, d = jira("PUT", f"/rest/api/3/version/{VER_R2X}",
                     {"archived": True, "name": "R2 — Growth (DUPLICATE — archived)"})
        log("OK" if s in (200, 204) else "FAIL", f"ARCHIVE version {VER_R2X}: {s}")

    # ── 2. Label + transition duplicate stories ──
    print(f"\n── 2. Label duplicate R1 stories ({len(DUP_STORIES)}) ──")
    for key in DUP_STORIES:
        if DRY:
            log("DRY", f"LABEL {key} as duplicate")
        else:
            # Add 'duplicate' label
            s1, _ = jira("PUT", f"/rest/api/3/issue/{key}",
                         {"update": {"labels": [{"add": "duplicate"}]},
                          "fields": {"summary": "[DUPLICATE] " + key}})
            # Try to transition to Done if not already
            s2, _ = jira("POST", f"/rest/api/3/issue/{key}/transitions",
                         {"transition": {"id": DONE_TRANSITION}})
            if s1 in (200, 204):
                log("OK", f"{key} labeled duplicate")
            else:
                log("FAIL", f"{key}: label={s1}")

    # ── 3. Label + transition duplicate epics ──
    print(f"\n── 3. Label duplicate epics ({len(DUP_EPICS)}) ──")
    for key in DUP_EPICS:
        if DRY:
            log("DRY", f"LABEL {key} as duplicate")
        else:
            # Already labeled 'duplicate' and Done from prior scripts
            # Just rename to make it clear
            s, d = jira("GET", f"/rest/api/3/issue/{key}?fields=summary")
            if s == 200:
                summary = d.get("fields", {}).get("summary", "")
                if not summary.startswith("[DUPLICATE]"):
                    s2, _ = jira("PUT", f"/rest/api/3/issue/{key}",
                                 {"fields": {"summary": "[DUPLICATE] " + summary}})
                    log("OK" if s2 in (200, 204) else "FAIL", f"{key} renamed")
                else:
                    log("OK", f"{key} already marked duplicate")
            else:
                log("FAIL", f"{key}: GET {s}")

    print(f"\n{'='*65}")
    if DRY:
        print(f"  DRY RUN — run with --run to execute")
    else:
        print(f"  COMPLETE: {ok} OK, {fail} FAIL")
    print("=" * 65)

if __name__ == "__main__":
    main()
