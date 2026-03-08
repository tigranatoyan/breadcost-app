import urllib.request, json, base64
from config import JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT
token = base64.b64encode((JIRA_EMAIL+':'+JIRA_API_TOKEN).encode()).decode()
hdrs = {'Authorization':'Basic '+token,'Accept':'application/json','Content-Type':'application/json'}

# 1. List all versions/releases
req = urllib.request.Request(JIRA_BASE_URL+f'/rest/api/3/project/{JIRA_PROJECT}/versions', headers=hdrs)
with urllib.request.urlopen(req) as r:
    versions = json.loads(r.read())
print("=== JIRA Releases (Fix Versions) ===")
for v in versions:
    released = "RELEASED" if v.get("released") else "unreleased"
    archived = " ARCHIVED" if v.get("archived") else ""
    date = v.get("releaseDate", "no date")
    print(f"  id={v['id']:6s}  {released:11s}{archived}  {date:12s}  {v['name']}")

# 2. Check sprint states
print("\n=== Sprints ===")
req2 = urllib.request.Request(JIRA_BASE_URL+f'/rest/agile/1.0/board/2/sprint?maxResults=50', headers=hdrs)
with urllib.request.urlopen(req2) as r:
    sprints = json.loads(r.read())
for sp in sprints.get('values', []):
    print(f"  id={sp['id']:4d}  {sp['state']:8s}  {sp['name']}")

# 3. Check fixVersion on Sprint 4/5 done stories
print("\n=== Sprint 4+5 Done stories — fixVersion check ===")
for sprint_id, label in [(40, "Sprint 4"), (41, "Sprint 5")]:
    body = json.dumps({'jql':f'project={JIRA_PROJECT} AND sprint={sprint_id} AND status=Done','maxResults':20,'fields':['summary','fixVersions','status']}).encode()
    req3 = urllib.request.Request(JIRA_BASE_URL+'/rest/api/3/search/jql',data=body,headers=hdrs,method='POST')
    with urllib.request.urlopen(req3) as r:
        data = json.loads(r.read())
    print(f"\n  {label} (sprint={sprint_id}):")
    for i in data.get('issues',[]):
        fv = [v['name'] for v in i['fields'].get('fixVersions',[])]
        fv_str = ', '.join(fv) if fv else '** NONE **'
        print(f"    {i['key']:8s} fixVersion=[{fv_str}]  {i['fields']['summary'][:60]}")
