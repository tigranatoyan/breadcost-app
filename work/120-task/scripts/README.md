# BreadCost — JIRA & GitHub Integration Scripts

Sync all epics, stories, and labels from `work/120-task/JIRA.md` into your JIRA project and GitHub repository.

---

## Setup (one-time)

1. **Python 3.10+** required (no pip packages needed)
2. Edit `config.py` with your credentials:

| Field | Where to get it |
|-------|----------------|
| `JIRA_BASE_URL` | Your Atlassian cloud URL, e.g. `https://myorg.atlassian.net` |
| `JIRA_EMAIL` | Your Atlassian account email |
| `JIRA_API_TOKEN` | [id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens) |
| `JIRA_PROJECT` | Your project key — default is `BC` |
| `GITHUB_OWNER` | GitHub user or org name |
| `GITHUB_REPO` | Repository name |
| `GITHUB_TOKEN` | [github.com/settings/tokens](https://github.com/settings/tokens) — scopes: `repo` |

3. **Add `config.py` to `.gitignore`** before pushing — it contains secrets.

---

## Usage

### Dry run (safe — nothing is created)
```cmd
cd work\120-task\scripts
python sync_jira.py
python sync_github.py
```

### Live run — JIRA
```cmd
python sync_jira.py --run          # create epics + stories
python sync_jira.py --run --epics  # create epics only
python sync_jira.py --run --stories  # create stories only (epics must exist first)
```

### Live run — GitHub
```cmd
python sync_github.py --run           # create labels + epic issues + story issues
python sync_github.py --run --labels  # create/update labels only
```

---

## What gets created

### JIRA
| Item | Count | Details |
|------|-------|---------|
| Epics | 24 | BC-E00 (user journeys) through BC-E23 |
| Stories | 54 | All R1 stories BC-101 through BC-1002 |

Each story is linked to its parent epic via the `parent` field (next-gen projects) or `customfield_10014` (classic projects).

### GitHub
| Item | Details |
|------|---------|
| Labels | Release (`r1/r2/r3`), priority (`p0–p3`), status, type, epic, domain |
| Epic issues | One issue per epic with goal + requirements |
| Story issues | One issue per story with acceptance criteria checklist, linked to its epic issue |

---

## Interconnecting JIRA ↔ GitHub

After both are set up:

1. **Install the GitHub for JIRA app** (free):  
   JIRA → Apps → Marketplace → search "GitHub for Jira" → Install

2. **Connect your repo** in the GitHub for JIRA app settings

3. **Use branch naming convention** — Copilot/developers name branches:  
   `feature/BC-201-create-draft-order`  
   JIRA will auto-link commits, PRs, and branches to story BC-201.

4. **Smart commits** — include story ID in commit messages:  
   `git commit -m "BC-201: add order creation endpoint"`  
   JIRA will transition the story automatically if configured.

---

## File structure

```
scripts/
  config.py        ← credentials (DO NOT COMMIT)
  data.py          ← all epics + stories as Python data structures
  sync_jira.py     ← JIRA REST API sync
  sync_github.py   ← GitHub API sync
  requirements.txt ← no external deps needed
  README.md        ← this file
```
