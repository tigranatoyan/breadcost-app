#!/usr/bin/env python3
"""Quick diagnostic: check Jira API auth and project state."""
import json, base64, urllib.request, urllib.error
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

token = base64.b64encode(f"{JIRA_EMAIL}:{JIRA_API_TOKEN}".encode()).decode()
headers = {
    "Authorization": f"Basic {token}",
    "Accept": "application/json",
    "Content-Type": "application/json",
}

def get(path):
    req = urllib.request.Request(f"{JIRA_BASE_URL}{path}", headers=headers)
    try:
        resp = urllib.request.urlopen(req)
        return 200, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]

# 1. Auth check
code, data = get("/rest/api/3/myself")
if code == 200:
    print(f"Auth OK: {data.get('displayName', '?')} ({data.get('emailAddress', '?')})")
else:
    print(f"Auth FAIL: HTTP {code}")
    print(data)

# 2. List projects
code, data = get("/rest/api/3/project/search?maxResults=20")
if code == 200:
    for p in data.get("values", []):
        print(f"  Project: {p['key']} - {p['name']} (id={p['id']})")
else:
    print(f"Projects FAIL: HTTP {code}")

# 3. Check specific project
code, data = get(f"/rest/api/3/project/{JIRA_PROJECT}")
if code == 200:
    print(f"\nProject {JIRA_PROJECT} found: {data.get('name', '?')} (id={data.get('id', '?')})")
else:
    print(f"\nProject {JIRA_PROJECT}: HTTP {code} - {data[:200]}")

# 4. Check a known issue
for key in ["BC-1", "BC-85", "BC-122"]:
    code, data = get(f"/rest/api/3/issue/{key}?fields=status,summary")
    if code == 200:
        status = data.get("fields", {}).get("status", {}).get("name", "?")
        summary = data.get("fields", {}).get("summary", "?")[:60]
        print(f"  {key}: [{status}] {summary}")
    else:
        print(f"  {key}: HTTP {code}")

# 5. Check transitions for an issue
code, data = get(f"/rest/api/3/issue/BC-1/transitions")
if code == 200:
    transitions = data.get("transitions", [])
    print(f"\nBC-1 transitions: {[t['name'] for t in transitions]}")
else:
    print(f"\nBC-1 transitions: HTTP {code}")
