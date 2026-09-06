---
sessionId: session-260905-115025-uaxc
---

# Requirements

### Overview & Goals
The objective is to optimize the test suite to maximize defect detection and ensure execution reliability with minimum test maintenance cost. Based on `build/reports/jacoco/test/jacocoTestReport.xml`, the current coverage baseline is **86.8% instruction coverage**, **67.5% branch coverage**, and **92.9% line coverage** (21 missed lines and 110 missed branches).

This plan outlines targeted, data-driven Spock test additions to achieve **100% line coverage** and maximize branch coverage across all engine components.

### Scope
- **In Scope:**
  - `HandlerRegistry`: Uncovered SAM-interface overloads (`ActionHandler`, `GuardPredicate`) and null parameter validations.
  - `ActivityDiagramParser`: Uncovered error validations (missing `start`, missing `end`/`stop`, blank PUML, empty action sequences) and `stop` terminal keyword parsing.
  - `Marking`: Uncovered `copy()` method, null token ingestion, and non-existent place lookups.
  - `DefaultPetriNet`: Null transition checks, empty token generation fallbacks, guard evaluation on empty places, and unhandled action return types.
  - `PlantFlow`: Direct `isEnabled()` calls, halted `step()` returns, null `getEndToken()`, and intermediate `isCompleted()` branch combinations.
  - Domain models (`RecordToken`, `Place`, `IncidenceMatrix`, `UnregisteredHandlerException`): Null defaults, boundary dimensions, and multi-arg exception constructors.
- **Out of Scope:**
  - Modifying engine production logic or adding new workflow features.
  - Non-linear PlantUML syntax parsing beyond existing linear grammar support.

### Acceptance Criteria
- Line coverage reaches 100% (296/296 lines).
- Branch coverage increases to >= 95%, covering all reachable control branches.
- All new tests follow Spock idiomatic data-driven design using `where:` blocks to maximize test density and minimize test duplication.

# Technical Design

### Current Implementation & Gap Analysis
The test suite in `src/test/groovy/eu/describeit/plantflow/` contains 7 Spock specifications. Analysis of `jacocoTestReport.xml` reveals the exact missing branches and lines:

- `HandlerRegistry.groovy` (66.7% line, 40.0% branch):
  - Missed lines 16–19: `registerAction(String, ActionHandler)` and its null checks.
  - Missed lines 35–38: `registerGuard(String, GuardPredicate)` and its null checks.
  - Partial branches on lines 23–24, 42–43: null validations in Closure-based registration methods.
- `ActivityDiagramParser.groovy` (92.9% line, 73.1% branch):
  - Missed line 26: `IllegalArgumentException` on null or empty PlantUML input.
  - Missed lines 74, 77, 80: Validation exceptions when diagram is missing `start`, `end`/`stop`, or actions.
  - Missed branch on line 63: diagram terminated with `stop` keyword.
  - Missed branch on line 20: null `File` handling.
- `Marking.groovy` (88.9% line, 57.1% branch):
  - Missed lines 88–92: `Marking.copy()` method completely untested.
  - Partial branches on lines 24, 50, 63, 76: null token in `addToken`, and non-existent `placeId` in `getTokens`, `getTokenCount`, and `isEmpty`.
- `DefaultPetriNet.groovy` (97.2% line, 76.1% branch):
  - Missed line 136: `getGuardToken` returning null when input places are empty.
  - Missed line 158: `getActionOutputToken` returning `emptyToken` when action handler returns non-Map / non-Token result or null.
  - Missed branches on lines 45, 146: null transition check and empty consumed tokens handling.
- `PlantFlow.groovy` (96.8% line, 62.5% branch):
  - Missed line 51: `isEnabled(Transition)` method.
  - Missed branches on lines 46, 67, 85, 89: null token seeding fallback, `step()` returning false when no transitions are enabled, `getEndToken()` on empty end place, and `isCompleted()` false conditions.
- `Domain Classes`:
  - `UnregisteredHandlerException.groovy`: Missed lines 12–13 (2-arg constructor).
  - `RecordToken.groovy`: Default fallbacks on lines 18–20 for null `id`, `timestamp`, `payload`.
  - `Place.groovy`: Fallback label on line 18 (`label ?: id`).
  - `IncidenceMatrix.groovy`: Matrix bounds and null safety on lines 26–27.

### High-Utility Testing Architecture
To maximize value per unit of test maintenance, tests should be parameterized using Spock `where:` tables rather than discrete test methods:
- Consolidate validation failure tests into single data-driven methods with descriptive `#scenario` columns.
- Test boundary values and default fallbacks in compact tables.
- Use explicit interface implementations to verify SAM interface contracts.

### Affected Test Files
- `src/test/groovy/eu/describeit/plantflow/HandlerRegistrySpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/SequencePumlParserSpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/MarkingSpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/DefaultPetriNetSpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/PlantFlowSpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/RecordTokenSpec.groovy`
- `src/test/groovy/eu/describeit/plantflow/PetriNetSpec.groovy`

# Testing

### Validation Approach
Verification is performed by running the Gradle test and JaCoCo report tasks and confirming that all missing lines and branch paths identified in the baseline report are resolved.

### Key Scenarios to Verify
- **Input Validation Matrix:**
  - `HandlerRegistry`: `registerAction` and `registerGuard` with `null` labels, `null` closures, and `null` interface instances throw `IllegalArgumentException`.
  - `ActivityDiagramParser`: `parse` with `null`, `""`, `"   "`, diagrams without `start`, diagrams without `end`/`stop`, and diagrams without action transitions throw `IllegalArgumentException` with precise messages.
  - `ActivityDiagramParser`: diagrams terminated with `stop` parse successfully into valid `PetriNet` instances.
- **SAM Interface Registration:**
  - `HandlerRegistry.registerAction(String, ActionHandler)` and `HandlerRegistry.registerGuard(String, GuardPredicate)` execute correctly during Petri net workflow processing.
- **Marking Mutability & Isolation:**
  - `Marking.copy()` produces an independent copy where mutating the copy does not alter the original marking vector or token lists.
  - `Marking.getTokens('unknown')`, `getTokenCount('unknown')`, and `isEmpty('unknown')` return `[]`, `0`, and `true`.
- **Workflow State & Step Transitions:**
  - `PlantFlow.isEnabled(Transition)` accurately reflects transition readiness.
  - `PlantFlow.step()` returns `false` when no transitions are enabled.
  - `PlantFlow.isCompleted()` accurately evaluates to `false` when end place is empty or when transitions remain enabled.
- **Domain Resilience:**
  - `RecordToken` generates random UUID, current timestamp, and empty map when initialized with `null` arguments.
  - `Place` defaults `label` to `id` when initialized without a label or with `null`.
  - `UnregisteredHandlerException` correctly retains both message and cause.

### Verification Command
Run `./gradlew check jacocoTestReport` and verify that instruction and line coverage reach 100% and branch coverage reaches >= 95%.

# Delivery Steps

### ✓ Step 1: Cover HandlerRegistry and ActivityDiagramParser validation and syntax paths
HandlerRegistry and ActivityDiagramParser achieve complete branch and line coverage for all validation, syntax, and interface overload paths.

- Extend `HandlerRegistrySpec` with data-driven Spock `where:` blocks covering null checks for labels, closures, `ActionHandler` instances, and `GuardPredicate` instances.
- Add tests in `HandlerRegistrySpec` validating SAM interface overloads for `registerAction(String, ActionHandler)` and `registerGuard(String, GuardPredicate)`.
- Add parameterized tests in `SequencePumlParserSpec` covering all validation error paths in `ActivityDiagramParser` (null/blank input, missing `start`, missing `end`/`stop`, and diagrams missing action transitions).
- Add test in `SequencePumlParserSpec` verifying activity diagrams terminated with the `stop` keyword.

### ✓ Step 2: Cover execution engine lifecycle, step execution, and Marking state mutations
Marking, DefaultPetriNet, and PlantFlow execution paths and state management edge cases achieve full test coverage.

- Add test in `MarkingSpec` verifying `Marking.copy()` deep-copy semantics and independence of cloned token vectors.
- Add parameterized assertions in `MarkingSpec` testing `addToken` with null tokens and querying non-existent place IDs via `getTokens`, `getTokenCount`, and `isEmpty`.
- Expand `DefaultPetriNetSpec` to cover null transition evaluation, guard evaluation when input places are empty, empty token consumption fallbacks, and action handlers returning non-Map/non-Token results.
- Expand `PlantFlowSpec` to verify `isEnabled(Transition)`, stepping a blocked workflow (`step() == false`), `getEndToken()` on empty end places, and `isCompleted()` across incomplete states.

### ✓ Step 3: Cover domain model defaults, matrix boundaries, and exception constructors
RecordToken, Place, IncidenceMatrix, and UnregisteredHandlerException achieve full domain coverage and verified overall metrics.

- Add parameterized tests in `RecordTokenSpec` for constructor fallback defaults when `id`, `timestamp`, or `payload` are null.
- Add test for `Place` constructor verifying default label fallback (`label ?: id`).
- Add tests verifying `IncidenceMatrix` resilience against null or truncated matrix dimensions.
- Add test in `HandlerRegistrySpec` or dedicated spec for `UnregisteredHandlerException(String, Throwable)` constructor.
- Execute test suite and verify JaCoCo metrics report line coverage at 100% and branch coverage at maximum achievable levels.