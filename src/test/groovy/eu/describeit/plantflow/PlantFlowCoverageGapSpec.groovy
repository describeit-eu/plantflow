package eu.describeit.plantflow

import eu.describeit.plantflow.engine.DefaultPetriNetEngine
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Gap tests for the PlantFlow facade: every constructor/factory overload and
 * step() on a workflow with no enabled transitions.
 */
class PlantFlowCoverageGapSpec extends Specification {

    def 'all constructor and factory overloads are usable'() {
        given:
        def structure = simpleLinearNet()
        def engine = new DefaultPetriNetEngine()
        def handlerRegistry = new HandlerRegistry()
        def executionContext = new ExecutionContext()

        expect:
        new PlantFlow(structure) != null
        new PlantFlow(structure, handlerRegistry) != null
        new PlantFlow(structure, handlerRegistry, executionContext) != null
        new PlantFlow(structure, engine) != null
        new PlantFlow(structure, engine, handlerRegistry) != null
        new PlantFlow(structure, engine, handlerRegistry, executionContext) != null
        PlantFlow.from(linearPuml('a')) != null
    }

    def 'PlantFlow.from loads a diagram from a file'() {
        given:
        File file = File.createTempFile('plantflow-gap', '.puml')
        file.text = linearPuml('a')
        file.deleteOnExit()

        expect:
        PlantFlow.from(file) != null
    }

    def 'constructors reject null arguments for every parameter'() {
        given:
        def structure = simpleLinearNet()
        def engine = new DefaultPetriNetEngine()
        def handlerRegistry = new HandlerRegistry()
        def executionContext = new ExecutionContext()

        when: 'the structure is null'
        new PlantFlow(null, handlerRegistry, executionContext)

        then:
        thrown(IllegalArgumentException)

        when: 'the handler registry is null'
        new PlantFlow(structure, (HandlerRegistry) null, executionContext)

        then:
        thrown(IllegalArgumentException)

        when: 'the execution context is null'
        new PlantFlow(structure, handlerRegistry, (ExecutionContext) null)

        then:
        thrown(IllegalArgumentException)

        when: 'the engine is null'
        new PlantFlow(structure, (eu.describeit.plantflow.engine.PetriNetEngine) null)

        then:
        thrown(IllegalArgumentException)

        when: 'the registry is null in the full constructor'
        new PlantFlow(structure, engine, null, executionContext)

        then:
        thrown(IllegalArgumentException)

        when: 'the context is null in the full constructor'
        new PlantFlow(structure, engine, handlerRegistry, null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'step returns false when no transitions are enabled'() {
        given: 'a fresh workflow that was never seeded'
        PlantFlow flow = PlantFlow.from(linearPuml('a'))

        expect:
        flow.getEnabledTransitions().isEmpty()
        !flow.step()
    }
}
