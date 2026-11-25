# Code Review — Ticket #55 (commit `b7bfc7d`)

Fixed point: `integration/switch-case-endswitch`
Commit: `b7bfc7d feat(parser): add switch/case/endswitch control structure (#55)`

## Standards

No hard violations. AGENTS.md / docs/agents/quality.md prescribe tooling only; new code follows existing idioms (`@CompileStatic`, `@EqualsAndHashCode`, defensive-copy constructors).

Judgement calls:
1. **Shotgun Surgery** — switch feature scattered across 6 main files; consistent with existing per-node-type pattern (ForkNode/RepeatNode), not new debt.
2. **Repeated Switches** — five type-keyed cascades on the node hierarchy; `Instanceof` explicitly excluded in CodeNarc config, so pattern is deliberate.
3. **Duplicated Code** — `SwitchNode`/`SwitchCase` near-identical boilerplate; consistent with `ConditionalNode`, low severity.
4. **Readability (actionable)** — `DiagramCompiler.compile` guard clause is a ~200-char single line with five `containsNode` clauses. Should be wrapped.
5. **Message chain (minor, actionable)** — `UnifiedDiagramBuilder.currentContainer` calls `state.stack.peek()` three times in one switch; bind once.
6. **Glossary gap** — new domain terms ("case branch", "endswitch merge place", `SwitchNode`) have no `GLOSSARY.md` entry (per docs/agents/domain.md).

## Spec

All 9 acceptance criteria implemented and passing (959 tests green).

Partial deviations / notes:
1. **"Verbatim" vs trimmed guard** — spec says case condition extracted "verbatim"; classifier applies `.trim()`. Test asserts `case ( conditionA )` → `'conditionA'`. Trimming inner whitespace is more robust; accepted as-is.
2. **Scope creep** — switch rows removed from `UnsupportedControlSyntaxSpec` / `PlantFlowCompositionRegressionSpec` (technically #59's cleanup, rides along). Unrelated refactors in `UnifiedDiagramBuilder` (behaviour-preserving). Bare `switch`/`case` without parens accepted with null guard.
3. **Empty switch compiles silently** — deferred to #57 by design.

## Verdict

All acceptance criteria met. Actionable fixes: wrap long guard clause, bind peek() once, add glossary entries.
