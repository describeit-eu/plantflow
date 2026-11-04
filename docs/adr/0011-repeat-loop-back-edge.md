# ADR-0011: Repeat Loop as Post-Test Loop with Back Edge

## Status

Accepted; implementation pending.

## Context

PlantUML `repeat ... repeat while (cond) is (yes) not (no)` describes a loop
whose body executes at least once before the condition is checked (post-test,
do-while semantics), unlike a pre-test `while` loop. The variant
`repeat while (true)` combined with `-[hidden]-> detach` describes an infinite
loop whose only exit is detaching the Token.

## Decision

- A Repeat Loop compiles like a conditional (ADR-0007) placed after the loop
  body: the body chain ends at a Decision Place with two Branch Transitions —
  a loop-back transition carrying the Loop Condition guard routing the Token
  back to the loop start, and an exit transition carrying the negated guard
  routing it onward. Guards resolve through the Handler Registry like `if`
  guards.
- The loop body always executes at least once; the Loop Condition is first
  evaluated after the first iteration.
- `repeat while (true)` compiles to a loop-back transition whose guard is
  constant true; `-[hidden]-> detach` terminates the Token without producing
  an output Token, so the Infinite Loop runs until detached.

## Consequences

- Nested Repeat Loops need distinct loop start and decision places per loop
  instance, analogous to indexed fork places (ADR-0010).
- A loop-back creates a cycle in the net; the engine must rely on guard
  evaluation to guarantee termination of condition-based loops.
