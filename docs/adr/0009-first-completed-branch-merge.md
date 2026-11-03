# ADR-0009: Non-synchronizing Fork Merge (first completed branch continues)

## Status

Accepted (implements issue #26, specification issue #25)

## Context

A compiled `fork ... end merge` previously used a single multi-input synchronizing
transition at Merge, so the outgoing flow waited for every Parallel Branch and
deep-merged all branch Tokens. The specification requires the first completed
branch of each Fork activation to continue through Merge once with its own Token,
while remaining branches finish independently without triggering another
continuation.

## Decision

- `ForkNetCompiler` compiles each branch end place to its own single-input merge
  transition (`T_fork_<i>_merge_<n>`) into the shared merge place, instead of one
  all-branch `merge_sync` transition.
- The net carries explicit `ForkDescriptor` metadata (fork transition index plus
  its merge transition indexes) via `PetriNetStructure.getForkDescriptors()`,
  populated through `PetriNetBuilder`.
- The `Marking` tracks pending fork activations per fork transition
  (`registerForkActivation` / `getPendingForkActivations` /
  `consumeForkActivation`). Firing a fork transition grants one continuation;
  a merge transition is only enabled while a continuation is pending and consumes
  it when fired, so each activation continues exactly once with the winning
  branch's own Token. The state is copied by `Marking.copy()` and serialized with
  the Marking JSON (absent in legacy JSON, which deserializes to none).
- Ordinary multi-input transitions (no `ForkDescriptor`) keep synchronizing and
  deep-merging Tokens unchanged, as do scheduling and completion semantics.

## Consequences

- Merge is no longer a synchronization point; loser branch Tokens rest at their
  branch end places and cannot trigger a second continuation.
- Overlapping Fork activations are counted per fork transition: each activation
  grants exactly one continuation, without correlating individual Tokens to
  activations (Token payloads and identifiers are not used for this).
