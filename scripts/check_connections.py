"""Quick connectivity check for Jira + GitHub. Run from scripts/ directory."""
import urllib.request, urllib.error, base64, json, sys

sys.path.insert(0, ".")
from config import (
    JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT,
    GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN,
)

print("=== JIRA CHECK ===")
try:
    creds = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
    req = urllib.request.Request(
        JIRA_BASE_URL + "/rest/api/3/project/" + JIRA_PROJECT,
        headers={"Authorization": "Basic " + creds, "Accept": "application/json"},
    )
    resp = urllib.request.urlopen(req, timeout=10)
    data = json.loads(resp.read())
    print("OK  Project:", data["key"], "-", data["name"])
    print("    Lead:", data.get("lead", {}).get("displayName", "?"))
except urllib.error.HTTPError as e:
    print("FAIL  HTTP", e.code, "-", e.read().decode()[:200])
except Exception as e:
    print("FAIL ", e)

print()
print("=== GITHUB CHECK ===")
try:
    req = urllib.request.Request(
        "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO,
        headers={"Authorization": "token " + GITHUB_TOKEN, "Accept": "application/vnd.github.v3+json"},
    )
    resp = urllib.request.urlopen(req, timeout=10)
    data = json.loads(resp.read())
    print("OK  Repo:", data["full_name"])
    print("    Private:", data["private"])
    print("    Default branch:", data["default_branch"])
except urllib.error.HTTPError as e:
    print("FAIL  HTTP", e.code, "-", e.read().decode()[:200])
except Exception as e:
    print("FAIL ", e)
