# Code Review: Overlapping Fork Activations (Commits 02f2c5c, 0a0af22)

**Date:** 2026-11-03
**Commits:** 02f2c5c (feat), 0a0af22 (refactor)
**Reviewer:** opencode agent
**Spec:** Issue #27 (Keep overlapping Fork activations independent), parent Issue #25
**Fixed Point:** 56af865 (spec/25-non-synchronizing-fork-merge)

---

## Standards

### Findings (all resolved in 0a0af22)

1. **MethodCount / ClassSize (hard, CodeNarc size rules)** — `Marking` grew to 37 methods / 326 lines against limits of 30 / 300.
   - **Fix:** merged `addToken` overload pairs via default parameter, merged `pendingList`/`selectedList` into `activationIdList`, inlined `indexOfToken`, removed redundant `removeToken(Place, Token)`, `getTokenActivation(Place, int)` and `getSelectedForkActivationCount` overloads (call sites now use `.index` / `.size()`), moved private helpers below public methods.
2. **MethodSize (hard, CodeNarc)** — `DefaultPetriNetEngine.fire` was 44 lines against a limit of 40.
   - **Fix:** extracted `consumeInputTokens` returning a `ConsumedInputs` holder (avoids a data clump of two parallel lists and stays within the 5-parameter limit).
3. **ReturnsNullInsteadOfEmptyArray / DuplicateNumberLiteral (hard, CodeNarc)** — `findPendingMergeToken` returned `null` for an `int[]` and literal `2` was duplicated.
   - **Fix:** return `new int[0]` and compare with `.length > 0`.
4. **ExplicitCallToEqualsMethod / ExplicitArrayListInstantiation / StaticMethodsBeforeInstanceMethods / PublicMethodsBeforeNonPublicMethods (hard, CodeNarc)** — `equals()` call, `new ArrayList<>()`, method ordering.
   - **Fix:** `==` comparison, `[]` literals, `getInputPlaces`/`getOutputPlaces` made static, private helpers ordered after public methods.
5. **Duplicated Code (judgement call, resolved)** — `getTokenActivations()` now delegates to `activationIdSnapshot` instead of duplicating the snapshot logic.

### Judgement calls

- **Backward-compat shim:** `setForkActivations` rebuilds pending ids from legacy count-based JSON only when no explicit pending ids are present; kept for deserialization compatibility with #26-era payloads.
- **ConsumedInputs holder:** small private static class bundling consumed tokens with their activation ids; avoids a data clump without a public type.

## Spec

### Findings

1. **Merge enablement assumed token position 0 (resolved before 02f2c5c)** — with an untagged or already-selected token ahead of a pending one, the merge was wrongly disabled (and a strict position-0 rule could starve a pending activation behind a late arrival, violating "each activation continues exactly once").
   - **Fix:** `findPendingMergeToken` scans all input tokens for one whose activation is still pending; `fire` consumes exactly that token. Late arrivals can neither suppress nor repeat another activation's continuation.
2. **Replacement Tokens** — correlation is carried by per-token activation tags in the `Marking`, never by payload, so handler-returned replacement Tokens keep their activation. Covered by `PlantFlowOverlappingForkActivationsSpec`.
3. **Scope check:** no changes to generic multi-input Transition deep-merge (`mergeConsumedTokens` untouched), scheduling, or completion semantics; existing `PlantFlowExecutionSpec`/deep-merge tests pass unchanged.

### Acceptance criteria mapping

- Interleaved activations continue exactly once with their own winner Token — `DefaultPetriNetEngineSpec` "overlapping activations...", `PlantFlowOverlappingForkActivationsSpec` interleaved test.
- Replacement Tokens keep correlation — replacement-token test.
- Separate custom Markings independent — custom-Marking test.
- Copy and serialization round-trips preserve in-progress and selected state — copy + two serialization tests, `MarkingSpec`, `MarkingMarshallerSpec`.
- Stateful/custom-Marking API consistency; multi-input Transition unchanged — consistency test plus unchanged existing suite.

**Summary:** Standards — 5 hard findings, all fixed; Spec — 1 behavioral finding, fixed; all acceptance criteria covered by tests.
