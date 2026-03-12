#!/usr/bin/env python3
"""
Close duplicate JIRA issues (BC-137..BC-216) created by accidental sync_jira.py --run.
Transitions them to Done and adds a 'duplicate' label.
Delete via API is blocked (403) on JIRA Free; closing is the cleanest alternative.

Usage:
    python delete_duplicates.py          # dry run
    python delete_duplicates.py --run    # live: close + label
"""
import sys
import json
import base64
import urllib.request
import urllib.error

try:
    from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN
except ImportError:
    print("ERROR: config.py not found.")
    sys.exit(1)

DRY_RUN = "--run" not in sys.argv
DONE_TRANSITION_ID = "31"  # from GET /issue/{key}/transitions


def _auth() -> dict:
    t = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
    return {"Authorization": f"Basic {t}", "Content-Type": "application/json", "Accept": "application/json"}


def _request(method: str, path: str, body: dict | None = None):
    url = f"{JIRA_BASE_URL}/rest/api/3{path}"
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=_auth(), method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        print(f"  HTTP {e.code} {method} {path}: {e.read().decode()[:150]}")
        return None


def close_issue(key: str) -> bool:
    # Add label 'duplicate'
    result = _request("PUT", f"/issue/{key}", {"fields": {"labels": ["duplicate"]}})
    if result is None:
        return False
    # Transition to Done
    result = _request("POST", f"/issue/{key}/transitions",
                      {"transition": {"id": DONE_TRANSITION_ID}})
    return result is not None


def main():
    keys = [f"BC-{n}" for n in range(137, 217)]  # BC-137 to BC-216 inclusive (80 issues)
    mode = "DRY RUN" if DRY_RUN else "LIVE"
    print(f"\n  Closing {len(keys)} duplicate issues ({keys[0]}..{keys[-1]}) as Done+duplicate [{mode}]\n")

    ok = 0
    for key in keys:
        if DRY_RUN:
            print(f"  DRY  CLOSE {key}")
            ok += 1
        else:
            if close_issue(key):
                print(f"  CLOSED  {key}")
                ok += 1
            else:
                print(f"  FAIL    {key}")
    print(f"\n  Done: {ok}/{len(keys)} closed.\n")


if __name__ == "__main__":
    main()
