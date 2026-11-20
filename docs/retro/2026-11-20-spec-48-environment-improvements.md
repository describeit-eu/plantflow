# Retrospective: Environment Improvements for Spec #48

**Date:** 2026-11-20
**Session:** Spec #48 end to end — grilling → to-spec (#48) → to-tickets (#49–#52) → implement-loop on `spec/48-nested-sequential-composition` → code-review → this retro
**Branch:** `spec/48-nested-sequential-composition` (90ab0f4)

---

## Summary

The full loop ran cleanly: spec published as issue #48, four tickets wired as sub-issues with
native `blocked_by` edges, each implemented in its own worktree off the integration branch,
all closed with comment+close, 949 tests green plus CodeNarc/JaCoCo/Pitest. The code-review
ran in sub-agents **by skill design** (two axes, fresh context windows each — correct for a
48-file diff), but the harness gave no way to await a specific child, so one duplicate
review sub-agent was spawned and the two overlapping reports had to be merged manually.
Candidates below are in order of severity.

---

## 🔴 HIGH SEVERITY

### 1. No guardrails: quality checks exist but are unwired (standing)

**Category:** Automated checks

**Finding:** Still no `.github/workflows/` and no non-sample git hooks (flagged in the
spec #19 and #25 retrospectives; unchanged). CodeNarc, JaCoCo, Pitest and the test suite
only run when the agent remembers to invoke them manually. This session ran them manually
on the integration branch (949 tests / 0 failures, CodeNarc clean, JaCoCo pass, Pitest
96% kill / 95% strength), but nothing enforces that on push or PR.

**Impact:** A failing build or lint violation can land on the integration branch uncaught.

**Recommendation:** Add `.github/workflows/ci.yml` running
`./gradlew codenarcMain codenarcTest test jacocoTestCoverageVerification` on push/PR, plus
a pre-commit hook running the same.

### 2. Acceptance suite can be green while a spec requirement is unmet

**Category:** Automated checks / testing seams

**Finding:** After ticket #52 the suite was fully green (all 11 `PlantFlowCompositionRegressionSpec`
scenarios pass), yet the code-review Spec axis found that fork-in-fork Join/Merge semantics
silently degrade (tag-transparent compilation) and that the mismatched-terminator check is
global rather than per-construct — both contradicting issue #48 stories 8/9/14. No test or
mutation gate catches these; only the review did.

**Impact:** "All tests green" is not equivalent to "spec met"; semantic gaps ship silently.

**Recommendation:** Extend the public-API acceptance scenarios with fork-in-fork `end fork`
(waits for all inner branches) and nested mixed-terminator cases so the e2e suite itself
pins those stories, not just the review.

---

## 🟡 MEDIUM SEVERITY

### 3. No wait/list operation for running sub-agents → duplicate review work

**Category:** Tool economy

**Finding:** `code-review` mandates two parallel sub-agents whose reports "arrive when each
goes idle". When the Standards report landed first, there was no way to await the Spec
report, so a follow-up "ping" sub-agent was dispatched and re-ran the entire spec-axis
review; the two overlapping reports then had to be deduplicated by hand. (Answer to the
session question: the sub-agent split itself was the skill's design, not an accident — the
waste was the missing await.)

**Impact:** Duplicated context-spending work and manual aggregation whenever reports arrive
out of order.

**Recommendation:** Add a sub-agent `list`/`wait` tool, or a line in the code-review skill:
"reports arrive automatically as each axis goes idle — never spawn follow-ups to chase them."

### 4. `gh issue create` has no `--json`; ticket numbers had to be scraped from URLs

**Category:** Tool economy / information access

**Finding:** `gh issue create --json number` fails ("unknown flag: --json"), so ticket
numbers were extracted with `grep -o '[0-9]*$'` from the printed URL. The tracker doc
documents `--jq` patterns for `issue list` but `issue create` was assumed to support
`--json` like other gh commands.

**Impact:** Fragile shell scraping in every to-tickets run.

**Recommendation:** Document the URL-scrape (or `--jq`-free) pattern in
`docs/agents/issue-tracker.md`, or wrap issue creation in a tiny script.

### 5. `code-review` fixed point defaults to asking instead of a stated default

**Category:** Process / navigation

**Finding:** The skill says "if the user didn't specify a fixed point, ask" — the user said
just "run /code-review" and the base (`develop`) was chosen silently. Right here, wrong in
general.

**Impact:** On other branches the review can pin the wrong base without anyone noticing.

**Recommendation:** Default the fixed point to the merge-base with the repo's default
branch and print it in the report header.

---

## 🟢 LOW / NONE

- **Navigation:** AGENTS.md pointers (issue-tracker, triage, quality, domain) led directly
  to the right docs; worktrees under `/tmp/opencode` plus `session_move` worked as intended.
  The dirty-worktree classification step in implement-loop worked this time (asked the
  user, committed the `GLOSSARY.md` typo fix to the integration branch) — the spec #25
  gap is closed by practice, but the loop skill still doesn't document the procedure.
- **Coding standards:** no new reviewer rule needed — the Spec axis already covers the
  fork-in-fork class of finding; Standards axis found only judgement-call smells.
- **Global AGENTS.md:** 18 lines, no sprawl, no no-op steering lines; nothing to move.
- **Information access:** `gh` via shell worked throughout; ad-hoc `python3` parsing of
  test-result XMLs this session repeats the spec #25 note — consider a `test-summary`
  gradle task or documenting the XML parsing pattern.
- Reviews are saved as committed artifacts: `docs/reviews/2026-11-20-spec-48-nested-composition-review.md`.
