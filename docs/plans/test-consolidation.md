# Test Consolidation Plan

**Goal**: 70-75% duplication reduction, preserve all coverage + issue #4 criteria

---

## Coverage Baseline (JaCoCo Report)

**Source**: `build/reports/jacoco/test/jacocoTestReport.xml` (generated via `./gradlew test`)

| Package | INSTRUCTION | BRANCH | LINE | COMPLEXITY |
|---------|-------------|--------|------|------------|
| `eu/describeit/plantflow` | 98% | 93% | 100% | 93% |
| `eu/describeit/plantflow/ast` | 99% | 88% | 100% | 90% |
| `eu/describeit/plantflow/engine` | 98% | 92% | 100% | 93% |

**Overall**: 100% line coverage across all packages. Branch coverage target: ≥92%. Instruction coverage target: ≥98%. Complexity coverage target: ≥93%.

---

## Coverage Preservation Rules

1. **Before each step**: Run `./gradlew test` to generate fresh JaCoCo report
2. **Before removing any test**: Verify the test does not uniquely cover any instruction/branch (use JaCoCo to identify)
3. **After each file move/modification**: Re-run tests and check JaCoCo report
4. **Minimum acceptable coverage**: LINE ≥100%, INSTRUCTION ≥98%, BRANCH ≥92%, COMPLEXITY ≥93%
5. **Critical paths**: Any test covering missed branches in ast (88%) or engine (92%) packages must be preserved

---

## Package Structure

**Principle**: Domain-based organization mirroring main code

| Domain | Package | Contents |
|--------|---------|----------|
| AST | `ast/` | DiagramValidatorSpec, ValidationTestUtil |
| Compiler | `compiler/` | PetriNetCompilerSpec |
| Parser | `parser/` | IfThenElseEndifPumlParserSpec, SequencePumlParserSpec, ParserTestBase |
| Registry | `registry/` | HandlerRegistrySpec |
| Engine | `engine/` | DefaultPetriNetSpec, ExecutionTestBase, MarkingSpec, PetriNetBuilderSpec, PetriNetComponentTestBase, PetriNetSpec, PlaceSpec, TransitionSpec, TokenSpec |
| Marshaller | `marshaller/` | IncidenceMatrixMarshallerSpec, MarshallerTestBase, MarkingMarshallerSpec |
| Root | `root/` | PlantFlowSpec, PlantFlowExecutionSpec, PlantFlowParsingSpec, PlantFlowErrorHandlingSpec, PlantFlowConstructorSpec |

---

## File Operations

### Create (Step 1)
- `ast/ValidationTestUtil.groovy`
- `engine/ExecutionTestBase.groovy`
- `marshaller/MarshallerTestBase.groovy`
- `engine/PetriNetComponentTestBase.groovy`
- `parser/ParserTestBase.groovy` (Step 3)

### Create (Step 2)
- `PlantFlowExecutionSpec.groovy`
- `PlantFlowParsingSpec.groovy`
- `PlantFlowErrorHandlingSpec.groovy`
- `PlantFlowConstructorSpec.groovy`

### Move
| File | From | To | Step |
|------|------|----|-------|
| DiagramValidatorSpec | root | ast/ | 2 |
| TokenSpec | root | engine/ | 3 |
| PetriNetCompilerSpec | root | compiler/ | 3 |
| IfThenElseEndifPumlParserSpec | root | parser/ | 3 |
| SequencePumlParserSpec | root | parser/ | 3 |
| HandlerRegistrySpec | root | registry/ | 4 |
| IncidenceMatrixMarshallerSpec | json/ | marshaller/ | 4 |
| MarkingMarshallerSpec | json/ | marshaller/ | 4 |

### Modify
- PlantFlowSpec: Reduce to integration-only tests (Step 2)

---

## Steps

### Step 1: Infrastructure - Complete
Create 4 utilities, update 2-3 specs to use them. **Verify**: JaCoCo report shows no coverage regression.

### Step 2: Split PlantFlowSpec - Complete
- Create 4 new PlantFlow*Spec files
- Move tests, remove duplicates
- Move DiagramValidatorSpec to ast/
**Verify**: Run JaCoCo, confirm LINE=100%, INSTRUCTION≥98.9%, BRANCH≥93.0%, COMPLEXITY≥90.7%

### Step 3: Parser/Compiler - Complete
- Create ParserTestBase
- Remove duplicates between parser/compiler specs
- Move TokenSpec to engine/
**Verify**: Run JaCoCo, confirm LINE=100%, INSTRUCTION≥98.9%, BRANCH≥93.0%, COMPLEXITY≥90.7%

### Step 4: Final - Complete
- Move HandlerRegistrySpec to registry/
- Move IncidenceMatrixMarshallerSpec and MarkingMarshallerSpec to marshaller/
- Standardize patterns, verify all tests pass
- Update docs (CONTEXT.md, AGENTS.md)
**Verify**: Run JaCoCo, confirm LINE=100%, INSTRUCTION≥98%, BRANCH≥92%, COMPLEXITY≥93% - PASSED

---
---

## Preserve

**Issue #4 acceptance criteria**:
- UnregisteredHandlerException for missing guard/action handlers
- runUntilEnd fails fast with unregistered handlers
- Deterministic transition selection (first enabled in matrix order)

---

## Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Test files | 19 | 18-20 |
| Duplicate tests | ~30-40 | <10 |
| Code reduction | - | 30-40% |
| LINE coverage | 100% | 100% |
| INSTRUCTION coverage | ≥98.9% | ≥98% |
| BRANCH coverage | ≥93.0% | ≥92% |
| COMPLEXITY coverage | ≥90.7% | ≥93% |
