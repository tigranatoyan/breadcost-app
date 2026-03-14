"""
Create 6 missing Jira tickets identified by cross-checking ARC_OBSERVATIONS.md against BACKLOG.md.
DRY RUN by default. Pass --run to actually create.
"""
import urllib.request, urllib.error, base64, json, sys

sys.path.insert(0, ".")
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
MODE = "DRY RUN" if DRY_RUN else "LIVE"
print("=" * 60)
print("  Gap Tickets -> Jira  [" + MODE + "]")
print("=" * 60)

creds = base64.b64encode((JIRA_EMAIL + ":" + JIRA_API_TOKEN).encode()).decode()
headers_get = {"Authorization": "Basic " + creds, "Accept": "application/json"}
headers_post = {
    "Authorization": "Basic " + creds,
    "Accept": "application/json",
    "Content-Type": "application/json",
}

ISSUE_TYPE_BUG = "10008"
ISSUE_TYPE_STORY = "10005"
ISSUE_TYPE_TASK = "10006"


def api_get(path):
    req = urllib.request.Request(JIRA_BASE_URL + path, headers=headers_get)
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


def api_post(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=headers_post, method="POST")
    return json.loads(urllib.request.urlopen(req, timeout=15).read())


# Get R6 version ID
versions = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/versions")
r6_id = None
for v in versions:
    if "R6" in v["name"]:
        r6_id = v["id"]
        print("R6 version: id=" + v["id"] + ", name=" + v["name"])
        break

if not r6_id:
    print("ERROR: R6 version not found!")
    sys.exit(1)

# 6 missing tickets from cross-check
TICKETS = [
    {
        "summary": "[P1-BUG] Login page auto-redirects to dashboard — stale token prevents re-login as different user",
        "type": ISSUE_TYPE_BUG,
        "priority": "High",
        "labels": ["arc1", "auth"],
        "description": "ARC_OBSERVATIONS gap item #1.\n\n"
            "On /login page, if a previous session token exists in localStorage, the page auto-redirects "
            "to /dashboard. User cannot stay on the login page to log in as a different user.\n\n"
            "This is distinct from X-01 (no auth guard) — this is about inability to switch users.\n\n"
            "Expected: Login page clears stale tokens on load, or shows login form regardless of existing token.\n"
            "Affected page: /login",
    },
    {
        "summary": "[P2-UX] Production plan Approve shows raw window.confirm() dialog instead of proper modal",
        "type": ISSUE_TYPE_BUG,
        "priority": "Medium",
        "labels": ["arc2", "ux"],
        "description": "ARC_OBSERVATIONS gap item #3.\n\n"
            "Clicking 'Approve' on a production plan triggers a browser-native window.confirm('lock the order') "
            "dialog instead of a styled modal component.\n\n"
            "Expected: Use the app's modal/dialog component with proper Approve/Reject options, "
            "showing reasons to reject (lead time conflict, inventory shortage).\n"
            "Affected page: /production-plans",
    },
    {
        "summary": "[P2-I18N] Date format is US (M/D/YYYY) instead of localized (DD.MM.YYYY)",
        "type": ISSUE_TYPE_BUG,
        "priority": "Medium",
        "labels": ["i18n", "cross-cutting"],
        "description": "ARC_OBSERVATIONS gap item #8.\n\n"
            "Dates display as '3/14/2026' (US format) across multiple pages including /customers and /orders.\n\n"
            "Expected: Dates should follow Armenian locale format (DD.MM.YYYY) or be configurable per tenant.\n"
            "Affected pages: /customers, /orders, and likely all pages showing dates.",
    },
    {
        "summary": "[P3-I18N] Bulk i18n sweep: inventory units (KG/L), seed data names, loyalty/technologist pages",
        "type": ISSUE_TYPE_TASK,
        "priority": "Low",
        "labels": ["i18n", "cross-cutting"],
        "description": "ARC_OBSERVATIONS gap items #5, #6, #7, #9, #10 (rolled into one ticket).\n\n"
            "Multiple pages have untranslated strings not covered by existing i18n tickets:\n"
            "- /inventory: Units 'KG', 'L' in English (A1-04 only covers 'PCS' on /orders)\n"
            "- /inventory: Seed data item names in English ('Wheat Flour', 'White Sugar', 'Butter')\n"
            "- /orders: Seed data product names in English ('White Bread', 'Sourdough Bread', 'Baguette')\n"
            "- /loyalty: All page text in English (no existing i18n ticket)\n"
            "- /technologist: All page text in English (no existing i18n ticket)\n\n"
            "This will be systematically caught by the i18n-sweep Playwright test (frontend/e2e/tests/i18n-sweep.spec.ts).",
    },
    {
        "summary": "[P4-DESIGN] Technologist: template system for recipes + technological steps (industry flexibility)",
        "type": ISSUE_TYPE_STORY,
        "priority": "Low",
        "labels": ["arc8", "design", "technologist"],
        "description": "ARC_OBSERVATIONS gap item #11.\n\n"
            "The /technologist page is identified as the core differentiator for multi-industry flexibility.\n\n"
            "Design requirements from observations:\n"
            "- Technologist = aggregation of Recipe + Technology (technological steps)\n"
            "- Should allow choosing templates from: recipes and technological steps\n"
            "- Can combine multiple templates: just bread bakery, bread + pastry, or bread + pastry + fast food\n"
            "- This customizability enables serving ALL industries (not just bakeries)\n"
            "- If recipes and technological steps can be fully custom, the platform becomes industry-agnostic\n\n"
            "Current implementation needs review against this vision.",
    },
    {
        "summary": "[AUDIT] 9 un-observed pages need QA walkthrough: admin, products, departments, recipes, subscriptions, tenant-mgmt, notification-templates, exchange-rates, mobile-admin",
        "type": ISSUE_TYPE_TASK,
        "priority": "Medium",
        "labels": ["qa", "audit"],
        "description": "ARC_OBSERVATIONS gap item #12.\n\n"
            "During the original arc-by-arc manual validation, the chat session broke before these 9 pages "
            "could be observed. They are complete blind spots with no QA data:\n\n"
            "1. /admin\n"
            "2. /products\n"
            "3. /departments\n"
            "4. /recipes\n"
            "5. /subscriptions\n"
            "6. /tenant-management\n"
            "7. /notification-templates\n"
            "8. /exchange-rates\n"
            "9. /mobile-admin\n\n"
            "Action: Run each page through the arc QA workflow (qa/ARC_QA_WORKFLOW.md) and create "
            "tickets for any issues found.",
    },
]

# Create tickets
created = 0
failed = 0
for t in TICKETS:
    fields = {
        "project": {"key": JIRA_PROJECT},
        "issuetype": {"id": t["type"]},
        "summary": t["summary"],
        "description": {
            "version": 1,
            "type": "doc",
            "content": [{"type": "paragraph", "content": [{"type": "text", "text": t["description"]}]}],
        },
        "priority": {"name": t["priority"]},
        "labels": t["labels"],
        "fixVersions": [{"id": r6_id}],
    }

    if DRY_RUN:
        print("  [DRY] Would create: " + t["summary"][:80])
    else:
        try:
            resp = api_post("/rest/api/3/issue", {"fields": fields})
            key = resp.get("key", "???")
            print("  CREATED: " + key + " — " + t["summary"][:70])
            created += 1
        except urllib.error.HTTPError as e:
            body = e.read().decode()
            print("  FAILED: " + t["summary"][:50] + " — " + str(e.code) + ": " + body[:200])
            failed += 1

print("\n" + "=" * 60)
if DRY_RUN:
    print("DRY RUN complete. " + str(len(TICKETS)) + " tickets would be created.")
    print("Run with --run to create them.")
else:
    print("Created: " + str(created) + "  Failed: " + str(failed))
print("=" * 60)
