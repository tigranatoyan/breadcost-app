"""Audit current Jira state — issues, statuses, sprints, boards."""
import urllib.request, urllib.error, urllib.parse, base64, json, sys

sys.path.insert(0, ".")
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

creds = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
headers = {"Authorization": "Basic " + creds, "Accept": "application/json"}


def api_get(path):
    req = urllib.request.Request(JIRA_BASE_URL + path, headers=headers)
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


# 1. Project info
print("=== PROJECT ===")
try:
    proj = api_get("/rest/api/3/project/" + JIRA_PROJECT)
    print("Key:", proj["key"], "| Name:", proj["name"])
    print("Style:", proj.get("style", "?"), "| Type:", proj.get("projectTypeKey", "?"))
except urllib.error.HTTPError as e:
    print("FAIL:", e.code, e.read().decode()[:200])

# 2. Issue count and breakdown via REST API 2 (fallback if v3 search fails)
print("\n=== ISSUES ===")
jql = "project=" + JIRA_PROJECT + " ORDER BY key ASC"
found = False
for api_ver in ["3", "2"]:
    try:
        url = "/rest/api/" + api_ver + "/search?jql=" + urllib.parse.quote(jql) + "&maxResults=100&fields=key,summary,status,issuetype"
        data = api_get(url)
        found = True
        break
    except urllib.error.HTTPError as e:
        print("API v" + api_ver + " search:", e.code)
        continue

if found:
    total = data["total"]
    print("Total issues:", total)
    statuses = {}
    types = {}
    for iss in data["issues"]:
        s = iss["fields"]["status"]["name"]
        t = iss["fields"]["issuetype"]["name"]
        statuses[s] = statuses.get(s, 0) + 1
        types[t] = types.get(t, 0) + 1

    print("\nBy Status:")
    for s, c in sorted(statuses.items(), key=lambda x: -x[1]):
        print("  " + s + ": " + str(c))
    print("\nBy Type:")
    for t, c in sorted(types.items(), key=lambda x: -x[1]):
        print("  " + t + ": " + str(c))

    print("\nFirst 15 issues:")
    for iss in data["issues"][:15]:
        f = iss["fields"]
        print("  " + iss["key"] + " [" + f["status"]["name"] + "] " + f["issuetype"]["name"] + " - " + f["summary"])

    if total > 15:
        print("  ... (" + str(total - 15) + " more)")
else:
    print("Could not query issues via search API")

# 3. Boards
print("\n=== BOARDS ===")
try:
    boards = api_get("/rest/agile/1.0/board?projectKeyOrId=" + JIRA_PROJECT)
    for b in boards.get("values", []):
        print("  Board:", b["id"], "-", b["name"], "(" + b["type"] + ")")
        # Sprints for this board
        try:
            sprints = api_get("/rest/agile/1.0/board/" + str(b["id"]) + "/sprint?maxResults=20")
            for sp in sprints.get("values", []):
                print("    Sprint:", sp["id"], "-", sp["name"], "[" + sp["state"] + "]")
        except urllib.error.HTTPError as e:
            print("    Sprints: HTTP", e.code)
except urllib.error.HTTPError as e:
    print("  Boards: HTTP", e.code, e.read().decode()[:100])

# 4. Versions / Releases
print("\n=== RELEASES ===")
try:
    versions = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/version?maxResults=50")
    for v in versions.get("values", []):
        print("  " + v["name"] + " [" + ("released" if v.get("released") else "unreleased") + "]")
except urllib.error.HTTPError as e:
    print("  HTTP", e.code)
except Exception as e:
    # Try alternate endpoint
    try:
        versions = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/versions")
        for v in versions:
            print("  " + v["name"] + " [" + ("released" if v.get("released") else "unreleased") + "]")
    except Exception as e2:
        print("  Could not fetch versions:", e2)
