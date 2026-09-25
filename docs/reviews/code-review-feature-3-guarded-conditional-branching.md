# Code Review: `develop` → `feature/3-guarded-conditional-branching`

**Fixed point**: `develop` (9cd66e8a)
**Head**: `a15c315` (feature/3-guarded-conditional-branching)
**Commits reviewed**: a15c315, 9cd66e8
**Date**: 2026-09-18

---

## Standards

**ActivityDiagramParser.groovy**
- Hard: Lines 148-177 — Dead code in comments violates clean code standards
- Hard: Lines 182-183 — `actions[0]`/`actions[1]` hardcodes 2-action assumption
- Judgement: Line 52 — Unused variable `tokens` (Mysterious Name)
- Judgement: Lines 53-55 — Primitive Obsession (Strings for guard/label domain concepts)

**PlantFlowSpec.groovy**
- Judgement: Lines 489-554 — Duplicated Code (guard registration repeated)

---

## Spec

**Issue**: #3 - Guarded Conditional Branching for If-Then-Else Diagrams

- **(b) Scope creep**: `PlantFlowSpec.groovy:576-629` adds `UnregisteredHandlerException` tests for if-then-else. Spec only asks: *"Verified via end-to-end Spock specifications using `src/test/data/puml/ifThenElseEndif.puml`"*

- **(c) Implemented but wrong**: `ActivityDiagramParser.groovy:179-184` creates structural transitions (`T_start_to_decision`, `T_branch_yes`, `T_branch_no`, `T_endif_to_end`) with `actionKey`, requiring action handlers. Spec distinguishes: *"guarded branch transitions (`T_branch_yes`, `T_branch_no`)"* from *"branch action transitions (`:process all;`, `:process none;`)",* implying structural transitions should only need guards.

---

## Summary

Standards: 5 findings (2 hard), worst: dead code.
Spec: 2 findings, worst: structural transitions require action handlers contrary to spec.
