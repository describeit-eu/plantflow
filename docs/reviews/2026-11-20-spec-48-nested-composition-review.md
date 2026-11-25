# Code Review: Spec #48 - Arbitrary nesting and sequencing of conditionals, Repeat Loops, and Forks

**Branch:** `spec/48-nested-sequential-composition` (9c19c71)
**Fixed point:** `develop` (3a20b41)
**Date:** 2026-11-20
**Reviewer:** code-review skill (two-axis: Standards + Spec)
**Spec:** Issue #48 with child tickets #49-#52 (all closed)
**Status:** ⚠️ OPEN — spec findings a.1/a.2/a.3 and c.1/c.2/c.3 are RESOLVED on `fix/spec-48-review-findings`; remaining open items: a.4 (single test seam trade-off) and (b) scope-creep notes only; branch is green (955 tests, 0 failures; CodeNarc clean; JaCoCo pass)

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
| 1 | Stories 8/9: nested Fork Join/Merge per-activation semantics **at any depth** | ✅ RESOLVED | Tag-transparent special case removed: every ForkNode registers its own ForkDescriptor/JoinDescriptor keyed by its own fork transition index; covered by new `PlantFlowNestedForkSpec` e2e tests. |
| 2 | Story 14: mismatched terminators rejected only **within one construct** | ✅ RESOLVED | Terminator state moved onto each `ForkFrame`; inner `end merge` inside outer `end fork` (and vice versa) now parse; legacy specs updated plus new parser specs in `UnifiedDiagramBuilderSpec`/`ForkEndForkPumlParserSpec`. |
| 3 | Zero-**nodes** emptiness rule (structure-only bodies valid) | ✅ RESOLVED | `Repeat loop body…` / `Fork branch…` messages now say "at least one node" in `UnifiedDiagramBuilder`, `ChainNetCompiler`, `DiagramValidator`, and all asserting specs. |
| 4 | "Single public-API test seam; no new parser/compiler seams" | ⚠️ PARTIAL | New `ChainNetCompilerSpec`, `NamingContextSpec`, expanded `UnifiedDiagramBuilderSpec`, `UnsupportedControlSyntaxSpec` assert internal labels. The 11 acceptance scenarios are correctly at the public API. |
| 5 | Full suite green + quality gates | ✅ MET | Verified outside the read-only review: 949 tests / 0 failures (all 11 `PlantFlowCompositionRegressionSpec` scenarios), CodeNarc 0 violations, JaCoCo verification passed, Pitest 1201/1255 killed (96%), 0 survived. |

### (b) Scope creep

- Parser/compiler unit specs asserting internal labels (see a.4) — likely driven by JaCoCo/Pitest gates.
- Apparently unreachable legacy `ERR_THEN_EMPTY` guard in `DiagramCompiler` (builder always emits a `ConditionalNode` on `endif`).
- Benign: GLOSSARY.md typo fix; `DiagramValidator` recursion into fork branches (needed for nested validation).

### (c) Implemented but looks wrong

1. **Tag-transparent fork-in-fork** (same as a.1) — ✅ RESOLVED: `nestedInForkBranch` logic and its Javadoc paragraph deleted; `connectJoin`/`connectMerge` always add descriptors.
2. **Shared `openConstructEndFork` global state** (same as a.2) — ✅ RESOLVED: mismatched-terminator state now lives on each `ForkFrame` (`Boolean terminator`).
3. **`PlantFlowExecutionSpec:1226` assertion weakened** — ✅ RESOLVED: restored the precise payload assertion (neither nested action in the end token, verified by running).

### Verified correct
Detach scoping (innermost loop only, must directly follow `repeat while`); zero-node empty validation; `Unsupported control syntax '…' (unimplemented feature)` rejection distinct from malformed; strict written-order execution of sequential constructs; per-iteration fork-activation isolation by token identity; engine untouched; no public API changes.

---

**Summary:** Standards — 0 hard violations, 4 judgement-call smells (worst: repeated type-switches across builder/compiler). Spec — a.1/a.2/a.3 and c.1/c.2/c.3 RESOLVED on `fix/spec-48-review-findings` (fork-in-fork per-activation descriptors, per-`ForkFrame` terminator tracking, zero-nodes error wording, precise nested-fork payload assertion); remaining open: a.4 (test-seam trade-off) and (b) scope-creep notes.
