# Retrospective: Environment Improvements for Spec #25

**Date:** 2026-11-03
**Session:** Implementation of spec #25 (non-synchronizing Fork Merge)
**Branch:** `spec/25-non-synchronizing-fork-merge`

---

## Summary

The spec #25 implementation (tickets #26 and #27, run via the implement-loop on a
temporary integration branch) surfaced environment gaps in guardrails and in the
loop's handling of a dirty main worktree. Candidates below are in order of severity.

---

## 🔴 HIGH SEVERITY

### 1. No guardrails: quality checks exist but are unwired

**Category:** Automated checks

**Finding:** The repo has no `.github/workflows/` directory and no non-sample git hooks.
CodeNarc, JaCoCo, Pitest, and the test suite only run if the agent remembers to invoke
them manually. Already flagged in the spec #19 retrospective; still standing.

**Impact:** A failing build or lint violation can land on the integration branch with
nothing catching it. The skills mandate these checks, but nothing enforces them.

**Recommendation:** Add `.github/workflows/ci.yml` running
`./gradlew codenarcMain codenarcTest test` on push/PR, plus a pre-commit hook running
the same, so the mandated checks are enforced rather than voluntary.

### 2. `implement-loop` has no procedure for a dirty main worktree

**Category:** Navigation / process

**Finding:** The loop says "stop in case anything is changed or new", but this session
started with a pre-seeded uncommitted `GLOSSARY.md` edit (spec preparation). It was
neither committed nor stashed, and the later `git merge --ff-only` of the ticket #26
branch into the integration branch aborted with "local changes to GLOSSARY.md would be
overwritten". The driver had to manually verify the branch's committed version
superseded the dirty edit before discarding it.

**Impact:** The loop dead-ends at the merge step on any pre-existing dirty file, with no
guidance on how to classify and resolve it.

**Recommendation:** Add a step to the loop: when the main worktree is dirty, classify
each change (spec prep vs. stray edit) and either commit it to the integration branch
first or stash it before any ticket merges.

---

## 🟡 MEDIUM SEVERITY

### 3. Subagent dispatch can silently abort

**Category:** Tool economy

**Finding:** The first `task` dispatch for ticket #27 returned `Tool execution aborted`
with no indication it was transient; the identical call had to be re-issued manually.
Neither `implement-loop` nor `implement-ticket` documents that task launches are
retryable.

**Recommendation:** One line in the loop skills: on task-tool failure, re-dispatch once
before treating it as a real failure.

### 4. `gh issue view` single-issue body extraction is undocumented

**Category:** Information access

**Finding:** Reading a ticket body required piping `gh issue view --json` through an
ad-hoc `python3` filter. `docs/agents/issue-tracker.md` documents `--jq` patterns for
`issue list` but not for single issues.

**Recommendation:** Add `gh issue view <n> --jq '.body'` to the tracker doc's read
conventions.

---

## 🟢 LOW / NONE

- `AGENTS.md` is 18 lines; no sprawl, no no-op steering instructions found, nothing to
  move into coding standards.
- The review stage ran as designed: both tickets have committed code-review reports
  (`docs/reviews/2026-11-03-issue-26-first-completed-branch-review.md`,
  `docs/reviews/2026-11-03-issue-27-overlapping-fork-activations-review.md`).
