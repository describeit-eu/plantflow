# Retrospective: Environment Improvements for Spec #19

**Date:** 2026-10-23
**Session:** Implementation of spec #19 (Deepen PlantFlow architecture)
**Branch:** `spec/19-deepen-plantflow-architecture`
**PR:** #24

---

## Summary

The spec #19 implementation revealed gaps in the coding agent's **environment** — the repo's CI, skills, and tooling. This retrospective identifies improvement candidates in order of severity.

---

## 🔴 HIGH SEVERITY

### 1. No CI/CD Pipeline

**Category:** Automated checks

**Finding:** The repo has **no `.github/` directory at all**. No CI workflows exist.

**Evidence:**
- `ls .github/` → "No such file or directory"
- `build.gradle.kts` has CodeNarc (strict, `isIgnoreFailures = false`), tests, and Pitest configured — but nothing runs them automatically

**Impact:** A PR can be merged with failing tests or lint violations. The quality tools exist but are **unwired**.

**Recommendation:** Add `.github/workflows/ci.yml`:

```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - run: ./gradlew codenarcMain codenarcTest test
```

---

### 2. `implement-ticket` Skill References Non-Existent Agent Types

**Category:** Information access

**Finding:** The skill says "Spawn a subagent named `tdd-developer`" (and `review-developer`, `static-fixer`, `coverage-fixer`, `test-hardener`, `test-consolidator`), but **these types don't exist**. Available types: `explore`, `general`.

**Evidence:**
- `.agents/skills/implement-ticket/SKILL.md` lines 3, 6, 8, 10, 12, 14
- `grep -r "tdd-developer" .agents/skills/` → only found in the skill itself, no definitions
- `.agents/skills/implement-ticket/agents/openai.yaml` only defines the interface, not agent implementations

**Impact:** Caused the agent to skip steps 5-15 entirely (code review, static fixes, coverage, mutation testing, test consolidation). The skill says "Do not deviate from this sequence" but provides no fallback when the named types don't exist.

**Recommendation:** Add a fallback section to the skill:

> **If named agent types are unavailable:** Map each phase to `general` with phase-specific instructions. Do not skip phases.

Or define the agent types in `.agents/skills/implement-ticket/agents/` with their prompts.

---

## 🟡 MEDIUM SEVERITY

### 3. No Pre-Commit Hook

**Category:** Automated checks

**Finding:** `.git/hooks/` only contains `.sample` files. No hook runs tests/lint before committing.

**Impact:** Commits with failing tests or lint violations can be created locally.

**Recommendation:** Add a pre-commit hook or use `lefthook`:

```bash
#!/bin/sh
./gradlew codenarcMain codenarcTest test --console=plain
```

---

### 4. Test CodeNarc Ruleset Is Empty

**Category:** Automated checks

**Finding:** `config/codenarc/test-rules.groovy` is just `ruleset { }` — **zero rules for test code**.

**Impact:** Test code has no linting. Dead code, duplicated strings, and other smells in tests go uncaught. This contributed to the ~30 dead TestKit methods that the code review found.

**Recommendation:** Enable basic rules for test code:

```groovy
ruleset {
  ruleset('rulesets/basic.xml')
  ruleset('rulesets/dry.xml')  // catches DuplicateStringLiteral
  ruleset('rulesets/unused.xml')  // catches unused methods
}
```

---

### 5. No Automated Check for "Tests Assert on Behavior, Not Implementation"

**Category:** Coding standards

**Finding:** The code review found 30 occurrences of `incidenceMatrix.getInputWeight()` in compiler specs. All 706 tests passed. CodeNarc was clean. But the tests were testing **implementation details** instead of **behavior**.

**Impact:** Tests pass but don't actually verify the right thing. Refactors can break behavior without failing tests.

**Recommendation:** Add a custom CodeNarc rule for test code that flags direct access to `incidenceMatrix` in compiler specs, or add a reviewer rule (judgement call).

---

## 🟢 LOW SEVERITY

### 6. AGENTS.md Is Well-Structured (No Changes Needed)

**Category:** Global AGENTS.md

**Finding:** `AGENTS.md` is 18 lines, well-organized with pointers to `docs/agents/*.md`. No no-ops.

**Status:** ✅ Good as-is. If CI is added, consider adding a pointer to the CI workflow.

---

### 7. Skills Are Reasonably Sized

**Category:** No-ops

**Finding:** Largest skill is `pr/SKILL.md` at 170 lines. No sprawl issues.

**Status:** ✅ No changes needed.

---

## Summary Table

| # | Severity | Category | Finding | Recommendation |
|---|----------|----------|---------|----------------|
| 1 | 🔴 HIGH | Automated checks | No CI/CD pipeline | Add `.github/workflows/ci.yml` |
| 2 | 🔴 HIGH | Information access | `implement-ticket` references non-existent agent types | Add fallback mapping or define agent types |
| 3 | 🟡 MEDIUM | Automated checks | No pre-commit hook | Add hook running tests/lint |
| 4 | 🟡 MEDIUM | Automated checks | Test CodeNarc ruleset is empty | Enable rules for test code |
| 5 | 🟡 MEDIUM | Coding standards | No check for "tests assert on behavior" | Custom CodeNarc rule or reviewer rule |
| 6 | 🟢 LOW | Global AGENTS.md | AGENTS.md is well-structured | No changes |
| 7 | 🟢 LOW | No-ops | Skills are reasonably sized | No changes |

---

## Related Documents

- **Code review:** `docs/reviews/2026-10-23-spec-19-architecture-deepening-review.md`
- **Process retrospective:** `docs/reviews/2026-10-23-spec-19-development-process-retrospective.md`
- **Skill:** `.agents/skills/implement-ticket/SKILL.md`
- **Build config:** `build.gradle.kts`
- **CodeNarc config:** `config/codenarc/rules.groovy`, `config/codenarc/test-rules.groovy`
