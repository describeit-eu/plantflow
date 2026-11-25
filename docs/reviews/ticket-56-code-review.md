# Code Review — Ticket #56 (commit `ab2a644`)

Fixed point: `integration/switch-case-endswitch`

## Standards

**Hard violation (actionable):**
1. `getSwitchDescriptors()` bypasses the interface-default descriptor pattern — should be a `default` getter on `PetriNetStructure` (like fork/join), `@Override` on `DefaultPetriNet`, and `SwitchSupport` should use `net.switchDescriptors` polymorphically instead of `instanceof DefaultPetriNet` downcast.

**Judgement calls (actionable):**
2. Duplicated guard call — `SwitchSupport.assertNoStalledSwitchToken(...)` called from both `PlantFlow.step()` and `StatefulExecution.step()`.
3. Feature Envy — `SwitchDescriptor` should encapsulate `isCaseTransition(int)` like `ForkDescriptor.isMergeTransition(int)`.
4. `SwitchSupport` static utility lacks the private constructor that `ForkJoinSupport` has.

## Spec

All 4 acceptance criteria covered, 979 tests pass, no scope creep. Minor notes (non-blocking): first-match-wins relies on transition ordering convention; stall check precedence in PlantFlow.step() is deliberate.

## Verdict

Fix the interface-default pattern violation + judgement calls 2-4.
