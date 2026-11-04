# ADR-0010: Synchronizing Fork Join

## Status

Accepted; implementation pending.

## Context

PlantUML `fork ... end fork` describes a synchronization point where execution
continues after every Parallel Branch has completed. This differs from
`fork ... end merge`, whose first-completion behavior is defined in ADR-0009.

## Decision

- A Join waits for every Parallel Branch of the same Fork activation and then
  continues once. Tokens from overlapping activations must not be mixed.
- If a branch does not reach the Join, its activation remains waiting. No
  timeout is required.
- Branch Token payloads are deep-merged in diagram order; later branches win
  conflicting values.
- The existing first-completion behavior of `end merge` remains unchanged.

## Consequences

- Join must distinguish Tokens belonging to different Fork activations, unlike
  the activation counting used for Merge in ADR-0009.
- A Join can remain waiting indefinitely if a branch never completes.