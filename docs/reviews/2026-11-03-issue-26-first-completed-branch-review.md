# Code Review: Issue #26 - Continue from the first completed Parallel Branch

**Commit:** 7757dd8 feat(engine): continue from first completed parallel branch at merge
**Date:** 2026-11-03
**Reviewer:** code-review skill (Standards + Spec axes)

## Standards

### Hard violations

None. CodeNarc main + test pass with zero violations.

### Baseline smells (judgement calls)

1. **Mutable post-construction metadata (mild)** — `DefaultPetriNet.forkDescriptors` is a
   non-final field assigned via a `@PackageScope` setter from `PetriNetBuilder` to stay within
   the CodeNarc `ParameterCount` limit of 5. Acceptable for a builder-assembled structure.

2. **Per-call descriptor scan (mild)** — `DefaultPetriNetEngine` scans `net.forkDescriptors` on
   every `isEnabled`/`fire` call. Fine at this net scale; a cached lookup could be added if nets grow.

3. **Test duplication (moderate)** — fork puml templates and two-branch fixtures repeat across
   `PlantFlowExecutionSpec` and `PlantFlowForkFirstCompletionSpec`; consolidated later by the
   test-consolidation step.

## Spec

### Verdict: Implementation matches the ticket; all acceptance criteria met.

### (a) Acceptance criteria coverage

- Either branch can complete first and immediately enables the outgoing flow:
  `PlantFlowForkFirstCompletionSpec` @Unroll over winner branch a/b, asserting the winner's merge
  transition is enabled while the loser is unfinished.
- Multi-action branch cannot reach Merge before its final action; winner Token continues without
  combining losers: multi-action branch test + payload assertions (`winner` only, no loser keys).
- Later branches remain executable and finish without a second continuation: `task 2`/`task 3`
  execute after `finalize`, their merge transitions stay disabled, exactly one end token.
- Post-Merge actions and Merge directly to End Place both covered (dedicated tests).
- Stateful and custom-Marking APIs covered with behavior-focused Spock tests; old
  all-branch-synchronization assertions in compiler/parser/execution/test-helper specs updated.
- Ordinary multi-input Transitions, deep-merge, scheduling, completion unchanged:
  `DefaultPetriNetEngineSpec` join/deep-merge tests untouched and passing; nets without
  `ForkDescriptor`s take the exact pre-change code path.
- Targeted tests, full Gradle suite, CodeNarc all pass.

### (b) Scope creep

None. Parser/AST untouched; nested fork remains out of scope (flattened to 4 branches as before).

### (c) Looks implemented but questionable

- **Overlapping activations are counted, not individually identified** — the Marking tracks a
  pending-continuation count per fork transition instead of per-activation identity, so with
  overlapping activations the aggregate behavior (one continuation per activation) holds, but a
  single activation could theoretically claim two continuations if its own two branches complete
  before another activation's branches. No acceptance criterion observes this; token-to-activation
  correlation would require tagging Tokens, which the spec rules out.
- **Losing branch tokens rest at branch end places** rather than being consumed by a discard
  transition. Observable state only; completion semantics (`isCompleted`) unaffected.
