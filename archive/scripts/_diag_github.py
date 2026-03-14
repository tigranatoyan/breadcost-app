#!/usr/bin/env python3
"""Quick diagnostic: check GitHub API auth and repo state."""
import json, urllib.request, urllib.error
from config import GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN

headers = {
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
}

def get(path):
    url = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}{path}"
    req = urllib.request.Request(url, headers=headers)
    try:
        resp = urllib.request.urlopen(req)
        return 200, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]

# 1. Auth check
code, data = get("")
if code == 200:
    print(f"Repo: {data.get('full_name', '?')}")
    print(f"  Open issues: {data.get('open_issues_count', '?')}")
else:
    print(f"Auth FAIL: HTTP {code}")
    print(data[:200])
    exit(1)

# 2. List open issues (paginated)
all_issues = []
page = 1
while True:
    code, data = get(f"/issues?state=open&per_page=100&page={page}")
    if code != 200 or not data:
        break
    all_issues.extend(data)
    if len(data) < 100:
        break
    page += 1

print(f"\nOpen issues: {len(all_issues)}")
for issue in all_issues:
    labels = ", ".join(l["name"] for l in issue.get("labels", []))
    print(f"  #{issue['number']:>3}  {issue['title'][:70]}  [{labels}]")
