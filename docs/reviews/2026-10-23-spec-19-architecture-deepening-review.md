# Code Review: Spec #19 - Architecture Deepening

**Branch:** `spec/19-deepen-plantflow-architecture`
**Fixed point:** `develop` (38512a8)
**Date:** 2026-10-23
**Reviewer:** code-review skill (two-axis: Standards + Spec)

## Diff Summary
- 61 files changed, +7147/-4895 lines
- 8 commits implementing 4 phases of architecture deepening

---

## Standards

### Hard Violations
**None.** CodeNarc (strict, main+test) passes; full test suite passes (706 tests); no stale references to deleted `PetriNet`/`PetriNetCompiler`.

### Judgement Calls (baseline smells)

| # | Smell | Location | Description |
|---|-------|----------|-------------|
| 1 | **Duplicated Code** | `compiler/ForkNetCompiler.groovy:118-125` vs `compiler/LinearNetCompiler.groovy:47-54` | `connectLinearActions` is a verbatim copy. Extract one shared helper. |
| 2 | **Duplicated Code / Middle Man** | `PlantFlowTestKit.groovy:95-440` | ~15 "legacy name" alias methods (`place`/`createPlace`, `transition`/`createTransition`, etc.) duplicating real ones. |
| 3 | **Speculative Generality (dead code)** | `PlantFlowTestKit.groovy` | ~30 kit methods have zero callers across the entire test tree. In a brand-new file there is no legacy to serve. |
| 4 | **Middle Man** | `engine/StatefulExecution.groovy:19-45, 94-116` | ~12 of 15 methods are one-line delegations to `structure`/`marking`, yet both are also exposed as public final fields, so callers bypass the facade anyway. |
| 5 | **Message Chains / Feature Envy** | `PlantFlow.groovy:67-78` | Reaches `petriNet.engine.getEnabledTransitions(petriNet.structure, ...)` — two-level navigation into `StatefulExecution`'s guts. |
| 6 | **Mysterious Name** | `PlantFlow.groovy:18` | Field `petriNet` is typed `StatefulExecution`; the name hides that it holds marking + engine state. |
| 7 | **Repeated Switches** | `parser/LinearDiagramBuilder.groovy:17-29`, `ConditionalDiagramBuilder.groovy:22-28`, `ForkDiagramBuilder.groovy:28-34` | `START`/`END` cases duplicated across all three builder `switch (line.type)` blocks. Minor. |
| 8 | **Parser leniency** | `parser/PumlLineClassifier.groovy:76-80` | `tryFork` uses `startsWith('fork')` where the old parser required exact `^\s*fork\s*$`; a stray line like `forklift` now opens a fork branch. |

### ADR Compliance
All 8 ADRs respected:
- ADR-0001 (matrix incidence): kept in `PetriNetStructure`
- ADR-0002, 0006 (handler registry): fail-fast `UnregisteredHandlerException` tests preserved
- ADR-0003, 0004 (Token/Marking): untouched
- ADR-0005 (context mutator handlers): still threaded through handlers
- ADR-0007 (explicit branch places): `P_if_decision`/`T_branch_yes`/`P_then`/`P_endif` topology preserved
- ADR-0008 (AST pipeline): classify → build AST → validate → compile pipeline intact

### Domain Vocabulary
Compliant. New code uses correct domain terms from GLOSSARY.md. No forbidden synonyms found.

---

## Spec

### (a) Missing or Partial Requirements

| # | Requirement | Status | Details |
|---|-------------|--------|---------|
| 1 | **"Spec files shrink by 30–40%"** (Phase 1) | ❌ NOT MET | Old specs: 1657 lines → New specs: 1625 lines (~2% shrink). Duplication moved into TestKit, but spec files barely shrank. |
| 2 | **"PlantFlow facade's public API remains unchanged"** (cross-cutting) | ⚠️ PARTIAL | Public field `petriNet` changed type `PetriNet` → `StatefulExecution`; constructor parameter changed `PetriNet` → `PetriNetStructure`. Tests now depend on the new type. |
| 3 | **"Each new module has a dedicated spec"** | ⚠️ MINOR GAP | No dedicated spec for `ClassifiedLine`/`LineType` (only exercised via `PumlLineClassifierSpec`) or the `DiagramBuilder` interface. |

### (b) Scope Creep

| # | Finding | Location |
|---|---------|----------|
| 1 | New public constructor `PlantFlow(PetriNetStructure, PetriNetEngine, …)` — additive API the spec never requested | `PlantFlow.groovy:28` |
| 2 | `PlantFlowTestKit` keeps ~10 legacy `createXxx` aliases duplicating new factory names — extra surface beyond "merge six utility modules" | `PlantFlowTestKit.groovy` |
| 3 | `PlantFlow.step()` lost its `log.info` line — minor behavior change under "all refactorings preserve external behavior" | `PlantFlow.groovy` |

### (c) Wrong Implementation

| # | Requirement | Status | Details |
|---|-------------|--------|---------|
| 1 | **"Tests assert on net topology, not incidence matrix indices"** (Phase 2) | ❌ VIOLATED | `ConditionalNetCompilerSpec.groovy:87-100` and `ForkNetCompilerSpec.groovy:108-196` assert via `net.incidenceMatrix.getInputWeight(place.index, …)` — 30 occurrences across compiler specs. |
| 2 | **"Builders construct ActivityDiagram explicitly (no flag mutation as side effects)"** (Phase 4) | ❌ VIOLATED | `ConditionalDiagramBuilder.buildDiagram` mutates flags post-construction (`diagram.hasIf = …`, `ConditionalDiagramBuilder.groovy:67-73`), including a surprising auto `hasEndif = true`. |

### Phase-by-Phase Verification

| Phase | Status | Notes |
|-------|--------|-------|
| **Phase 1** (TestKit) | ✅ Mostly met | PlantFlowTestKit consolidates all 6 old utilities; PlantFlowTestKitSpec meta-tests it; 7 specs merged into 2. **Missing:** 30–40% spec shrink not achieved. |
| **Phase 2** (Compiler) | ⚠️ Met except test-style | NetLabeling, LinearNetCompiler, ConditionalNetCompiler, ForkNetCompiler (self-contained), DiagramCompiler (48 lines < 50). **Violated:** tests still assert on matrix indices. |
| **Phase 3** (Interface) | ✅ Met | PetriNetStructure (5+2 methods), PetriNetEngine (3 methods), StatefulExecution adapter, DefaultPetriNet/DefaultPetriNetEngine. Old PetriNet and PetriNetSpec deleted. **Note:** Facade field type changed. |
| **Phase 4** (Parser) | ⚠️ Met except flag mutation | ClassifiedLine/LineType, PumlLineClassifier, three builders, ActivityDiagramParser at 43 lines. **Violated:** ConditionalDiagramBuilder mutates flags post-construction. |

---

## Summary

| Axis | Findings | Worst Issue |
|------|----------|-------------|
| **Standards** | 8 judgement calls, 0 hard violations | Speculative generality: ~30 dead TestKit methods with zero callers |
| **Spec** | 3 missing/partial, 3 scope creep, 2 wrong implementation | Phase 2 tests still assert on incidence matrix indices instead of net topology |

### Top Priority Fixes
1. **Spec violation:** Migrate compiler specs from `incidenceMatrix.getInputWeight()` to TestKit topology helpers (`verifyTransitionIO`, `findTransitionByLabel`, etc.)
2. **Spec violation:** Fix `ConditionalDiagramBuilder` to construct `ActivityDiagram` explicitly without flag mutation
3. **Standards smell:** Remove ~30 dead methods from `PlantFlowTestKit` (zero callers)
4. **Standards smell:** Extract shared `connectLinearActions` helper from `LinearNetCompiler`/`ForkNetCompiler`
5. **Spec gap:** Achieve 30–40% spec file shrink (currently ~2%)
