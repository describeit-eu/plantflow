---
name: implement-loop
description: "Implement the result of /to-spec and /to-tickets in a sequential manner."
disable-model-invocation: true
metadata:
  credits:
    skill: implement-spec
    author: Matt Pocock
    url: "https://github.com/mattpocock/skills/tree/main/skills/engineering/implement-spec/SKILL.md"
---

You have been provided a spec. This spec should have tickets associated with it, describing how to implement the spec.

The issue tracker should have been provided to you. If not, tell the user to run `/setup-matt-pocock-skills`.

The goal is the entire spec implemented on a single **temporary integration branch**, with every ticket resolved the way the issue tracker closes work.

The tickets are not a list of steps. They are a **task graph** with blocking relationships between them. This means there is always a **frontier** of tickets which are ready to be grabbed.

Run `/implement-ticket` for each ticket in its own worktree, one at a time.

## Steps

1. Read the spec and tickets to understand the task graph.
2. Switch to the main integration branch (e.g. develop) and stop in case anything is changed or new.
3. Create the **temporary integration branch** named after the spec.
4. Pick one ticket from the **frontier** (e.g. choose the ticket with the smallest number), create a worktree on a branch named after the ticket based on the **temporary integration branch**. Run `/implement-ticket` in that worktree.
5. Once the `/implement-ticket` run completes, merge its branch to the **temporary integration branch**.
6. If the issue tracker closes work through PRs, or the user asks for one, check if a draft PR already exists. If not, open one after the first merge and instruct the PR to close the spec and its tickets.
7. If this changes the **frontier** of available tickets, go back to step 4.
8. If a draft PR exists, mark it ready for review. Otherwise, resolve each ticket the way the issue tracker closes work. Report the **temporary integration branch**.
9. Clean up all ticket branches and worktrees.
