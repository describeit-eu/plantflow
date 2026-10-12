# Code Review: Ticket #13 - Fork Compilation Validation

**Commit:** 8a553ea feat(parser): support post-merge actions in fork compilation
**Branch:** feature/13-fork-compilation-validation
**Date:** 2026-10-12

## Standards Review

**Hard violations:** None found.

### Judgement calls

1. **Duplicated Code** — `connectPostForkActions` (PetriNetCompiler.groovy:314-326) re-implements the shape of `connectLinearActions` (:148-159). Should delegate to existing helpers. Magic string `'post_'` deviates from named-constant pattern.

2. **Middle Man / Speculative Generality** — `compile(ForkNode)` (PetriNetCompiler.groovy:93-95) is a pure one-line delegate used only by tests. Creates ripple: `compile(null)` became ambiguous, forcing test edit to cast `(ActivityDiagram) null`.

3. **Speculative null-handling** — `postForkActions ?: Collections.<ActionNode>emptyList()` (PetriNetCompiler.groovy:113) is dead defensiveness; both callers pass non-null.

4. **Handler with hidden side effect** — `handleEndMergeMatching` (ActivityDiagramParser.groovy:156-172) no longer just matches; it mutates the diagram by adding ForkNode. Name promises matching; behavior builds AST.

5. **Visibility widened for tests** — `DiagramValidator.validateForkBranches` changed from `private` to package-private "for testing", but no test calls it directly.

6. **Duplicated Code (tests)** — `inputCount`/`outputCount` incidence-counting block copy-pasted 4x across PetriNetCompilerSpec and ForkEndMergePumlParserSpec.

## Spec Review

### (a) Missing or partial requirements

- No parser/AST-level test asserting post-merge `ActionNode`s land in `diagram.nodes` after the `ForkNode`.
- No negative validation test combining post-merge actions with an invalid fork (e.g., empty branch + trailing action).

### (b) Scope creep

- New public `PetriNetCompiler.compile(ForkNode)` overload — redundant with `compileFork(ForkNode)`.
- Breaking side effect: `compile(null)` now ambiguous for dynamic callers.
- `DiagramValidator.validateForkBranches` widened from `private` to public.
- Whitespace-only churn in parser and compiler.

### (c) Implemented but looks wrong / fragile

- Node ordering is load-bearing: `extractPostForkActions` relies on `node.is(forkNode)` identity plus list order. Silently drops non-ActionNode after fork.
- Sequential forks mishandled: `branches` never cleared between forks.
- Inconsistent place naming: `P_post_1` vs `P_fork_1_*` naming scheme.

## Summary

- **Standards:** 0 hard violations, 6 judgement calls
- **Spec:** 2 missing/partial, 4 scope creep, 3 fragile/wrong

**Worst Standards issue:** Duplicated Code in `connectPostForkActions` (should reuse `connectLinearActions`)
**Worst Spec issue:** `compile(ForkNode)` overload is scope creep with breaking side effect on `compile(null)`
