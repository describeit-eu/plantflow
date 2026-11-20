# Code Review: Spec #48 - Arbitrary nesting and sequencing of conditionals, Repeat Loops, and Forks

**Branch:** `spec/48-nested-sequential-composition` (9c19c71)
**Fixed point:** `develop` (3a20b41)
**Date:** 2026-11-20
**Reviewer:** code-review skill (two-axis: Standards + Spec)
**Spec:** Issue #48 with child tickets #49-#52 (all closed)
**Status:** ⚠️ OPEN — findings below are unaddressed; branch is green (949 tests, 0 failures; CodeNarc clean; JaCoCo pass; Pitest 96% kill / 95% strength) but has spec-conformance gaps worth fixing before merge

## Diff Summary
- 48 files changed, +1764/-3267 lines, 10 commits
- Engine (`engine/`, `ast/`) untouched, as the spec requires
- New: `UnifiedDiagramBuilder` fork frames, `ChainNetCompiler`, `NamingContext`, `UNSUPPORTED` line classification; deleted: all specialized builders/compilers and `NetLabeling`

---

## Standards

**Hard violations of documented standards: none.** Groovy 5/`@CompileStatic` throughout; GLOSSARY.md vocabulary respected (Join vs Merge used correctly); ADR-0008 parse→AST→validate→compile pipeline preserved; CodeNarc limits within bounds (ChainNetCompiler 274 lines vs ClassSize 300; `compileFork` 5 params vs ParamCount 5); no dangling references to deleted classes.

**Baseline smells (judgement calls):**

1. **Repeated Switches** — `UnifiedDiagramBuilder.processLine` dispatches into construct-grouped sub-methods that each re-switch on `line.type`; same shape recurs in `ChainNetCompiler.compileChain` and `DiagramCompiler.childrenOf` (all switch on node type). Defensible grouping-by-construct, but the pattern is repeated.
2. **Duplicated Code** — `ActivityDiagramParser.rejectUnsupportedLines` / `rejectMalformedLines` are near-identical `findAll → collect → join → throw` shapes; extract one helper taking a `LineType` filter.
3. **Data Clump / Primitive Obsession** — `ClassifiedLine` now carries six fields (`type, text, action, guard, branchLabel, construct`), with `construct` meaningful only for `UNSUPPORTED`; `UnifiedState.openConstructEndFork` is a nullable `Boolean` standing in for a tri-state (unset / merge / fork) — a small enum would say it.
4. **Middle Man (minor)** — `DiagramCompiler` now only adds the `ERR_THEN_EMPTY` guard plus recursive `containsNode` traversal, then delegates to `ChainNetCompiler`; the traversal duplicates AST-children knowledge that `childrenOf` also encodes.
5. Stale test-class name `RepeatConditionalNetCompilerSpec` references removed compilers (test-side only).

## Spec

### (a) Missing / partial requirements

| # | Requirement | Status | Details |
|---|---|---|---|
| 1 | Stories 8/9: nested Fork Join/Merge per-activation semantics **at any depth** | ⚠️ PARTIAL | Fork inside another fork's branch compiles tag-transparent (`nestedInForkBranch`, no descriptors): inner `end fork` fires on first branch token, inner `end merge` can continue per branch — not all-branches / exactly-once. Forks in conditional branches and loop bodies are correct (covered by `PlantFlowForkCompositionSpec`). |
| 2 | Story 14: mismatched terminators rejected only **within one construct** | ❌ VIOLATED | One shared `openConstructEndFork` field (reset only on outermost fork push) rejects an inner `end merge` inside an outer `end fork`. Terminator state should live on each `ForkFrame`. Legacy specs `PlantFlowForkJoinSpec:376-377`, `PlantFlowMalformedForkTerminatorSpec:35-36` currently assert the rejecting behavior and need updating. |
| 3 | Zero-**nodes** emptiness rule (structure-only bodies valid) | ⚠️ PARTIAL | Behavior correct, but error messages still say "must contain at least one action" (`Repeat loop body…`, `Fork branch…`) — misdescribes the rule. |
| 4 | "Single public-API test seam; no new parser/compiler seams" | ⚠️ PARTIAL | New `ChainNetCompilerSpec`, `NamingContextSpec`, expanded `UnifiedDiagramBuilderSpec`, `UnsupportedControlSyntaxSpec` assert internal labels. The 11 acceptance scenarios are correctly at the public API. |
| 5 | Full suite green + quality gates | ✅ MET | Verified outside the read-only review: 949 tests / 0 failures (all 11 `PlantFlowCompositionRegressionSpec` scenarios), CodeNarc 0 violations, JaCoCo verification passed, Pitest 1201/1255 killed (96%), 0 survived. |

### (b) Scope creep

- Parser/compiler unit specs asserting internal labels (see a.4) — likely driven by JaCoCo/Pitest gates.
- Apparently unreachable legacy `ERR_THEN_EMPTY` guard in `DiagramCompiler` (builder always emits a `ConditionalNode` on `endif`).
- Benign: GLOSSARY.md typo fix; `DiagramValidator` recursion into fork branches (needed for nested validation).

### (c) Implemented but looks wrong

1. **Tag-transparent fork-in-fork** (same as a.1) — openly documented in the `ChainNetCompiler` Javadoc as a deliberate divergence from the spec's per-activation-descriptor decision.
2. **Shared `openConstructEndFork` global state** (same as a.2) — genuine logic smell beyond the spec wording: per-construct terminator tracking belongs on `ForkFrame`.
3. **`PlantFlowExecutionSpec:1226` assertion weakened** — nested-fork payload check changed from asserting neither nested action ran to accepting either (a disjunction where a precise payload assertion belongs).

### Verified correct
Detach scoping (innermost loop only, must directly follow `repeat while`); zero-node empty validation; `Unsupported control syntax '…' (unimplemented feature)` rejection distinct from malformed; strict written-order execution of sequential constructs; per-iteration fork-activation isolation by token identity; engine untouched; no public API changes.

---

**Summary:** Standards — 0 hard violations, 4 judgement-call smells (worst: repeated type-switches across builder/compiler). Spec — 2 violated/partial requirements + 3 wrong-looking behaviors (worst: fork-in-fork Join/Merge semantics and the global mismatched-terminator check, both rooted in `UnifiedDiagramBuilder`/`ChainNetCompiler` fork handling).
