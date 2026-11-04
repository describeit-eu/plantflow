# Code Review: ticket #31 (feature/31-resume-in-progress-join-activations vs spec/29-synchronizing-fork-join)

## Standards
Hard violation:
1. Spock iron rule — PlantFlowResumeJoinSpec tests 1–3 are 3+ similar tests without a `where:` block (groovy-developer testing-spock.md:7); collapse into a `#mode`-parameterized `@Unroll` test like PlantFlowForkJoinSpec does. Test 4 (legacy JSON) may stand alone.

Judgement calls:
2. Duplicated setup/assert scaffolding across the 4 tests (extract or via the where: refactor).
3. Message chains `marking.getTokens(...)*.payload.toSet()` — repo-tolerated, optional.

## Spec
(a) Partial: legacy-format compatibility test uses a linear diagram, so the `forkActivations` field is never exercised — strengthen it to resume a legacy in-progress fork/join marking (count-based forkActivations JSON) with correlation intact.
(b) No scope creep. (c) Change is test-only (resume behavior already worked on base of #30) — acceptable as regression coverage.
