package eu.describeit.plantflow.compiler

import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class LinearNetCompilerSpec extends Specification {

    def compiler = new LinearNetCompiler()

    def 'should compile single action into PetriNet with start and end places'() {
        given:
        def actions = actions('process order')

        when:
        def net = compiler.compile(actions)

        then:
        validateNetStructure(net, 2, 1)
        validatePlaceLabels(net, ['start', 'end'])
        net.transitions[0].label == 'process order'
        net.transitions[0].actionKey == 'process order'
        net.transitions[0].guardKey == null
    }

    def 'should compile two actions into PetriNet with intermediate place'() {
        given:
        def actions = actions('Hello world', 'groovy goodness')

        when:
        def net = compiler.compile(actions)

        then:
        validateNetStructure(net, 3, 2)
        validatePlaceLabels(net, ['start', 'P_1', 'end'])
        verifyLinearChain(net, ['Hello world', 'groovy goodness'])

        and:
        verifyTransitionConnection(net, 0, 0, 1)
        verifyTransitionConnection(net, 1, 1, 2)
    }

    def 'should compile multi-action sequence with sequential intermediate places'() {
        given:
        def actionNames = ['step 1', 'step 2', 'step 3', 'step 4']
        def actions = actions(*actionNames)

        when:
        def net = compiler.compile(actions)

        then:
        validateNetStructure(net, 5, 4)
        validatePlaceLabels(net, ['start', 'P_1', 'P_2', 'P_3', 'end'])
        verifyLinearChain(net, actionNames)

        and:
        (0..3).each { int i ->
            verifyTransitionConnection(net, i, i, i + 1)
        }
    }

    def 'should compile single action with direct start-to-end connection'() {
        given:
        def actions = actions('only action')

        when:
        def net = compiler.compile(actions)

        then:
        validateNetStructure(net, 2, 1)
        validatePlaceLabels(net, ['start', 'end'])
        net.transitions[0].actionKey == 'only action'

        and:
        verifyTransitionConnection(net, 0, 0, 1)
    }

    def 'should compile many actions with correct chaining'() {
        given:
        def actionList = (1..5).collect { "action ${it}" }
        def actions = actions(*actionList as String[])

        when:
        def net = compiler.compile(actions)

        then:
        validateNetStructure(net, 6, 5)
        verifyLinearChain(net, actionList)
    }

    @Unroll
    def 'should throw IllegalArgumentException for #scenario'() {
        when:
        compiler.compile(actions)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Diagram must contain at least one action transition'

        where:
        scenario          | actions
        'null action list' | null
        'empty action list' | []
    }
}
