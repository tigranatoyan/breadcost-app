"""
Sync BACKLOG.md → Jira: create R4, R5 (released), R6 (unreleased) + 55 tickets.
DRY RUN by default. Pass --run to actually create.

Safety:
  - Only creates NEW versions and issues
  - Does NOT modify existing 199 Done stories
  - Does NOT touch git, GitHub, or local files
"""
import urllib.request, urllib.error, urllib.parse, base64, json, sys

sys.path.insert(0, ".")
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT

DRY_RUN = "--run" not in sys.argv
MODE = "DRY RUN" if DRY_RUN else "LIVE"
print("=" * 60)
print("  BACKLOG → Jira Sync  [" + MODE + "]")
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


def api_put(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(JIRA_BASE_URL + path, data=data, headers=headers_post, method="PUT")
    urllib.request.urlopen(req, timeout=15)


# ────────────────────────────────────────────────────────────
# 1. RELEASES
# ────────────────────────────────────────────────────────────
print("\n--- RELEASES ---")
existing = api_get("/rest/api/3/project/" + JIRA_PROJECT + "/versions")
existing_names = {v["name"]: v for v in existing}

releases_to_create = [
    {"name": "R4 — Security + Customer Portal", "released": True, "description": "R4 S1-S6: Security hardening, customer portal (8 pages), subscription enforcement, visual rework. 449 tests."},
    {"name": "R5 — ARCMAP Gap Close", "released": True, "description": "R5 S1-S3: POS inventory deduction, WO material checks, push notifications, stock alerts, yield tracking, invoice disputes, subscription expiry, supplier mapping. 469 tests."},
    {"name": "R6 — Arc Validation Fixes", "released": False, "description": "57 issues from arc-by-arc manual validation. See work/BACKLOG.md."},
]

version_ids = {}
for rel in releases_to_create:
    if rel["name"] in existing_names:
        print("  SKIP (exists): " + rel["name"])
        version_ids[rel["name"]] = existing_names[rel["name"]]["id"]
        continue
    print("  CREATE: " + rel["name"] + " [" + ("released" if rel["released"] else "unreleased") + "]")
    if not DRY_RUN:
        body = {
            "name": rel["name"],
            "projectId": api_get("/rest/api/3/project/" + JIRA_PROJECT)["id"],
            "released": rel["released"],
            "description": rel["description"],
        }
        if rel["released"]:
            body["releaseDate"] = "2026-03-13"
        result = api_post("/rest/api/3/version", body)
        version_ids[rel["name"]] = result["id"]
        print("    → Created: " + result["id"])

# ────────────────────────────────────────────────────────────
# 2. BACKLOG ISSUES → Jira Tickets
# ────────────────────────────────────────────────────────────
print("\n--- ISSUES ---")

# Map backlog type to Jira issue type
def get_issue_type(backlog_type):
    if backlog_type == "BUG":
        return ISSUE_TYPE_BUG
    elif backlog_type in ("FEATURE", "DESIGN"):
        return ISSUE_TYPE_STORY
    else:
        return ISSUE_TYPE_TASK

# Map priority
def get_priority(p):
    mapping = {"P0": "Highest", "P1": "High", "P2": "Medium", "P3": "Low", "P4": "Lowest"}
    return mapping.get(p, "Medium")

# All backlog items (excluding Deferred)
backlog = [
    # Arc 1: Order Lifecycle
    ("A1-01", "P1", "BUG", "/orders", "Line Total shows 0.00 for all order lines — backend not calculating qty × unitPrice"),
    ("A1-02", "P3", "i18n", "/orders", "Status badges untranslated: DRAFT, CONFIRMED, IN_PRODUCTION, READY, OUT FOR DELIVERY, DELIVERED"),
    ("A1-03", "P3", "i18n", "/orders", "Column header broken mixed-language translation"),
    ("A1-04", "P3", "i18n", "/orders", "PCS unit in English; RUSH badges in English"),
    ("A1-05", "P2", "FEATURE", "/orders", "DRAFT orders not editable — no edit button for lines, customer, delivery date"),
    ("A1-06", "P4", "DESIGN", "/orders", "Order lifecycle (READY→DELIVERED) driven from /orders, should be from /deliveries + /driver"),
    ("A1-07", "P1", "BUG", "/deliveries", "Marking Out for Delivery on /orders does NOT create a delivery run on /deliveries"),
    ("A1-08", "P2", "UX", "/deliveries", "Assign Orders requires typing raw UUIDs — should be selectable list of READY orders"),
    ("A1-09", "P2", "UX", "/deliveries", "Run ID displayed as raw UUID — should be sequential number"),
    ("A1-10", "P3", "i18n", "/deliveries", "All 15+ UI strings in English"),
    ("A1-11", "P1", "BUG", "/driver", "NOT IN NAVIGATION MENU — only accessible via direct URL"),
    ("A1-12", "P0", "BUG", "/driver", "Button labels show RAW i18n KEYS: driver.refresh, driver.lookup"),
    ("A1-13", "P2", "UX", "/driver", "Requires typing raw UUIDs for Delivery Run ID & Session ID"),
    ("A1-14", "P4", "DESIGN", "/driver", "Should auto-show assigned delivery run for logged-in driver"),
    ("A1-15", "P3", "i18n", "/driver", "All text in English"),
    # Arc 2: Production Planning
    ("A2-01", "P1", "BUG", "/production-plans", "Clicking Create Plan repeatedly creates duplicate plans — no idempotency"),
    ("A2-02", "P1", "BUG", "/production-plans", "Generate Work Orders on duplicate plans creates duplicate WOs"),
    ("A2-03", "P2", "UX", "/production-plans", "New plans generate 0 WOs but no feedback to user"),
    ("A2-04", "P1", "BUG", "/production-plans", "Plan-level Complete button does nothing when clicked"),
    ("A2-05", "P1", "BUG", "/production-plans", "Yield shows 0.0h — entered value not saved or displays in wrong format"),
    ("A2-06", "P1", "BUG", "/production-plans", "Plan completion does NOT auto-update linked order status"),
    ("A2-07", "P2", "UX", "/production-plans", "Plans not sorted chronologically"),
    ("A2-08", "P2", "UX", "/production-plans", "No way to delete duplicate/empty plans"),
    ("A2-09", "P2", "UX", "/production-plans", "Only one plan expandable at a time — can't compare"),
    ("A2-10", "P4", "DESIGN", "/production-plans", "Approve without reject option — no way to send back"),
    ("A2-11", "P4", "DESIGN", "/production-plans", "No auto-plan from confirmed orders — entire flow is manual"),
    ("A2-12", "P4", "DESIGN", "/production-plans", "Mark Ready on /orders should auto-trigger from plan completion"),
    ("A2-13", "P3", "i18n", "/production-plans", "All status badges, headers, section names in English"),
    ("A2-14", "P4", "DESIGN", "/floor", "/floor is passive read-only — should be PRIMARY page for workers"),
    ("A2-15", "P1", "BUG", "/floor", "Date-locked to today — can't see other dates plans"),
    ("A2-16", "P2", "UX", "/floor", "No date navigation"),
    ("A2-17", "P3", "i18n", "/floor", "All text in English"),
    # Arc 3: Inventory & Supply Chain
    ("A3-01", "P1", "BUG", "/suppliers", "Currency defaults to UZS instead of AMD"),
    ("A3-02", "P0", "BUG", "/suppliers", "Auto-Suggest POs returns HTTP 500 — backend crash"),
    ("A3-03", "P2", "UX", "/suppliers", "Create PO requires manually typing Ingredient ID — should be dropdown"),
    ("A3-04", "P2", "UX", "/suppliers", "Ingredient Name doesn't auto-populate from Ingredient ID"),
    ("A3-05", "P4", "DESIGN", "/suppliers", "Mixes admin (supplier setup) with operations (PO creation)"),
    ("A3-06", "P3", "i18n", "/suppliers", "All 20+ UI strings in English"),
    ("A3-07", "P2", "FEATURE", "/inventory", "No way to ADD new inventory items"),
    ("A3-08", "P2", "FEATURE", "/inventory", "No bulk CSV/XLSX import for items or stock receipts"),
    ("A3-09", "P3", "i18n", "/inventory", "All UI text in English"),
    # Arc 4: Customer Portal
    ("A4-01", "P1", "BUG", "/customers", "Product Catalog tab returns errors"),
    ("A4-02", "P2", "UX", "/customers", "Customer names truncated"),
    ("A4-03", "P2", "UX", "/customers", "Order IDs shown as truncated UUIDs"),
    ("A4-04", "P4", "DESIGN", "/customers", "Customer Orders tab duplicates /orders — no unique value"),
    ("A4-05", "P2", "UX", "/customers", "Creating order requires manually typing customer name"),
    ("A4-06", "P3", "i18n", "/customers", "All text in English"),
    ("A4-07", "P2", "UX", "/loyalty", "Balance lookup requires raw Customer ID"),
    # Arc 6: Financial Operations
    ("A6-01", "P0", "BUG", "/reports", "Page crashes: Cannot read properties of undefined (reading length)"),
    ("A6-02", "P4", "DESIGN", "/reports", "Too many indicators — should be filterable or from report-builder"),
    ("A6-03", "P4", "DESIGN", "/report-builder", "3-tier subscription model needs implementation clarity"),
    # Arc 7: AI Assistance
    ("A7-01", "P4", "DESIGN", "AI pages", "AI features should be embedded in context pages, not standalone"),
    ("A7-02", "P4", "DESIGN", "/ai-whatsapp", "WhatsApp = priority first AI implementation"),
    ("A7-03", "P4", "DESIGN", "/quality-predictions", "Should embed in production floor/plans context"),
    # Arc 8: Platform Administration
    ("A8-01", "P4", "DESIGN", "/dashboard", "Dashboard should be configurable — widget selection from admin panel"),
    ("A8-02", "P4", "DESIGN", "/analytics", "Overlaps with /reports — merge into dashboard widgets"),
    ("A8-03", "P2", "BUG", "/technologist", "Observation incomplete (chat broke before finishing)"),
    # Cross-Cutting
    ("X-01", "P1", "BUG", "All pages", "Pages accessible without logging in — no auth guard on frontend routes"),
    ("X-02", "P2", "UX", "All pages", "UUID inputs everywhere instead of searchable dropdowns"),
]

# Arc labels for Jira labels
arc_map = {
    "A1": "arc-1-orders",
    "A2": "arc-2-production",
    "A3": "arc-3-inventory",
    "A4": "arc-4-customer",
    "A5": "arc-5-pos",
    "A6": "arc-6-finance",
    "A7": "arc-7-ai",
    "A8": "arc-8-admin",
    "X": "cross-cutting",
}

created = 0
skipped = 0
failed = 0

for bid, prio, btype, page, summary in backlog:
    arc_prefix = bid.rsplit("-", 1)[0]
    arc_label = arc_map.get(arc_prefix, "unknown")
    prio_label = prio.lower()

    issue_type_id = get_issue_type(btype)
    jira_summary = "[" + bid + "] " + summary

    # Truncate summary to 255 chars (Jira limit)
    if len(jira_summary) > 255:
        jira_summary = jira_summary[:252] + "..."

    description = "**Backlog ID**: " + bid + "\n"
    description += "**Priority**: " + prio + "\n"
    description += "**Type**: " + btype + "\n"
    description += "**Page**: " + page + "\n\n"
    description += summary + "\n\n"
    description += "Source: work/BACKLOG.md"

    body = {
        "fields": {
            "project": {"key": JIRA_PROJECT},
            "summary": jira_summary,
            "issuetype": {"id": issue_type_id},
            "priority": {"name": get_priority(prio)},
            "labels": [arc_label, prio_label, btype.lower()],
            "description": {
                "version": 1,
                "type": "doc",
                "content": [
                    {
                        "type": "paragraph",
                        "content": [{"type": "text", "text": description}],
                    }
                ],
            },
        }
    }

    # Add fixVersion (R6) if version was created
    r6_name = "R6 — Arc Validation Fixes"
    if r6_name in version_ids:
        body["fields"]["fixVersions"] = [{"id": version_ids[r6_name]}]

    if DRY_RUN:
        type_name = "Bug" if issue_type_id == ISSUE_TYPE_BUG else "Story" if issue_type_id == ISSUE_TYPE_STORY else "Task"
        print("  WOULD CREATE: " + type_name + " | " + get_priority(prio) + " | " + jira_summary)
        created += 1
    else:
        try:
            result = api_post("/rest/api/3/issue", body)
            print("  CREATED: " + result["key"] + " | " + jira_summary)
            created += 1
        except urllib.error.HTTPError as e:
            err = e.read().decode()[:300]
            print("  FAILED: " + jira_summary + " → " + str(e.code) + " " + err)
            failed += 1

# ────────────────────────────────────────────────────────────
# SUMMARY
# ────────────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("  SUMMARY [" + MODE + "]")
print("=" * 60)
print("  Releases: " + str(len(releases_to_create)))
print("  Issues created: " + str(created))
if failed:
    print("  Issues failed: " + str(failed))
if DRY_RUN:
    print("\n  → Re-run with --run to execute for real")
