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

You have been provided a spec, perhaps with extra instructions. This spec should have tickets associated with it, describing how to implement the spec. The goal is to implement the entire spec on a single **temporary integration branch**, with every ticket resolved the way the issue tracker closes work.

The issue tracker should have been provided to you. If not, tell the user to run `/setup-skills`.

The tickets are not a list of steps. They are a **task graph** with blocking relationships between them. This means there is always a **frontier** of tickets which are ready to be grabbed. However, the tickets shall be executed in a **sequential manner** to avoid conflicting edits of the same file. In other words, run `/implement-ticket` for each ticket in its own worktree, one at a time.

Follow these instructions because it is designed to control the context window.
- You must not deviate from the sequence described bellow. The best is to treat it as a template for your TODO plan.
- You shall not create subagent for these Steps. 

## Sequence of Steps

1. Read the spec and tickets to understand the task graph.
2. Check the main worktree if it is dirty or not. If dirty, classify each change as:
    - **spec prep**: commit it to integration branch
    - **stray edit**: full stop and ask user to resolve the issue
3. Create the **temporary integration branch** named after the spec.
4. Pick one and only ticket from the **frontier** (e.g. choose the ticket with the smallest number), create a worktree on a branch named after the ticket based on the **temporary integration branch**.
5. Call the Skill tool for `/implement-ticket` to work on the chosen ticket using the worktree.
6. Once the `/implement-ticket` run completes, rebase its branch to the **temporary integration branch**.
7. Resolve ticket the way the issue tracker closes a work.
8. If this changes the **frontier** of available tickets, go back to step 4.
9. Clean up all ticket branches and their worktrees.
