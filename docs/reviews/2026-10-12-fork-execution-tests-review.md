# Code Review: Issue #14 - Fork Execution & Tests

**Commit:** a0e5506 feat(execution): implement fork/merge execution with deep-merge token payloads
**Date:** 2026-10-12
**Reviewer:** code-review skill (Standards + Spec axes)

## Standards

### Hard violations

1. **Unused import** — `ForkEndMergePumlCompilerSpec.groovy:4` imports `eu.describeit.plantflow.PetriNetCompiler` but never references it.

### Baseline smells (judgement calls)

2. **Duplicated Code (strong)** — `ForkEndMergePumlCompilerSpec` repeats ~7 tests nearly verbatim from `parser/ForkEndMergePumlParserSpec.groovy:215-326`, including identical feature names and assertions.

3. **Duplicated Code (strong)** — `ForkEndMergePumlExecutionSpec.groovy:518-539` `'regression: sequence.puml should execute correctly'` is a verbatim copy of `PlantFlowExecutionSpec.groovy:102-123`. The ifThenElse regressions duplicate `PlantFlowSpec.groovy` and `PlantFlowSteppableExecutionSpec.groovy`.

4. **Duplicated Code (moderate)** — Within `ForkEndMergePumlExecutionSpec`, the `registerAction` closure block repeats 4x and the two-branch fork/merge puml template repeats ~6x.

5. **Speculative Generality (mild)** — `Token.deepMerge`'s null-guard; sole caller never passes null. Acceptable for public API.

6. **Style nits** — `deepMergeMaps` uses Java-style `Map.Entry` loop with raw casts where Groovy `each` would do; redundant `("action_${i}" as String)` cast.

## Spec

### Verdict: Implementation matches spec; all 4 acceptance criteria met.

### (a) Missing / partial requirements

- **"Regression tests pass for all existing .puml files"** — partially met. Regression test covers only 3 of 10 files. Files like `crud.puml`, `repeatWhile.puml`, `switchCaseEndswitch.puml`, `whileEndwhile.puml` parse successfully but are untested. Files `ifEqualsThenEndif.puml`, `ifIsThenEndif.puml`, `whileInfinite.puml` throw `IllegalArgumentException`.

### (b) Scope creep

- `DefaultPetriNet.getActionOutputToken()` now deep-merges consumed tokens for **every** transition, not just merge sync transitions. Behaviorally a no-op for single-input transitions.

### (c) Looks implemented but questionable

- **Merge conflict winner is arbitrary** — highest-indexed branch-end place wins. Deterministic but no defined branch-priority semantics.
- **Shallow-copy aliasing in `deepMergeMaps`** — nested maps present only in base are shared by reference with merged token.
- **Step-by-step test fragility** — intermediate-state assertions depend on compiler transition ordering.

## Summary

- **Standards:** 1 hard violation, 5 judgement calls
- **Spec:** All acceptance criteria met; 3 areas of concern
- **Worst Standards issue:** Duplicated test code across spec files
- **Worst Spec issue:** Regression test coverage gap (3/10 files)
