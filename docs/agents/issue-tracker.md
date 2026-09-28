# Issue Tracker: GitHub

Issues and specs live as GitHub issues. Use `gh` CLI for all operations.

## Conventions

For git workflow, branch naming, and commit conventions, see `git-user` skill.

- **Create**: `gh issue create --title "..." --body "..."`; use heredoc for multi-line bodies
- **Read**: `gh issue view <number> --comments`; filter comments with `jq` and fetch labels
- **List**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with `--label` and `--state` filters
- **Comment**: `gh issue comment <number> --body "..."`
- **Apply label**: `gh issue edit <number> --add-label "..."`
- **Remove label**: `gh issue edit <number> --remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v`; `gh` does this automatically when run inside a clone.

## PRs as Triage Surface

**PRs as request surface: no.** Set to `yes` if this repo treats external PRs as feature requests.

For PR lifecycle and workflow, see `git-user` skill.

When enabled, use `gh pr` equivalents:

- **Read**: `gh pr view <number> --comments` and `gh pr diff <number>` for diff
- **List external PRs**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments`; keep only `authorAssociation` of `CONTRIBUTOR`, `FIRST_TIME_CONTRIBUTOR`, or `NONE`
- **Comment**: `gh pr comment <number> --body "..."`
- **Apply label**: `gh pr edit <number> --add-label "..."`
- **Remove label**: `gh pr edit <number> --remove-label "..."`
- **Close**: `gh pr close <number>`

GitHub shares one number space across issues and PRs. Resolve with `gh pr view <n>` and fall back to `gh issue view <n>`.

## Skill Integrations

- **Publish to issue tracker**: Create GitHub issue
- **Fetch relevant ticket**: `gh issue view <number> --comments`

## Wayfinding Operations

Leading word: **structured** — used by `/wayfinder` with map and child ticket system.

### Map
Single issue labelled `wayfinder:map`, holding Notes, Decisions-so-far, and Fog body.
```bash
gh issue create --label wayfinder:map
```

### Child Ticket
Issue linked to map as GitHub sub-issue via `gh api` on sub-issues endpoint. Where unavailable, add child to task list in map body and put `Part of #<map>` at top of child body.

Labels: `wayfinder:<type>` where type is `research`, `prototype`, `grilling`, or `task`.

Once claimed, assign to driving dev.

### Blocking
Use GitHub native issue dependencies (UI-visible).

Add dependency edge:
```bash
gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>
```

Where `<blocker-db-id>` is the blocker's numeric database id:
```bash
gh api repos/<owner>/<repo>/issues/<n> --jq .id
```

Note: Use database id, _not_ the `#number` or `node_id`.

GitHub reports `issue_dependencies_summary.blocked_by` (open blockers only).

Fallback: add `Blocked by: #<n>, #<n>` line at top of child body.

A ticket is unblocked when every blocker is closed.

### Frontier Query
List map's open children (`gh issue list --state open`, scoped to map's sub-issues / task list).

Drop any with:
- Open blocker (`issue_dependencies_summary.blocked_by > 0`)
- Open issue in `Blocked by` line
- Assignee

First in map order wins.

### Claim
```bash
gh issue edit <n> --add-assignee @me
```
Session's first write.

### Resolve
```bash
gh issue comment <n> --body "<answer>"
gh issue close <n>
```
Then append context pointer (gist + link) to map's Decisions-so-far.
