package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNetStructure
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Unit tests for the recursive chain compiler: sequential and nested
 * Action/Conditional/Repeat nodes compile uniformly with unique labels
 * drawn from one shared naming context.
 */
class ChainNetCompilerSpec extends Specification {

    def compiler = new ChainNetCompiler()

    def 'should compile sequential conditionals with distinct naming context ids'() {
        given:
        def diagram = diagram(true, true, [
            conditional('g1', actions('a'), actions('b')),
            conditional('g2', actions('c'), actions('d')),
        ])

        when:
        PetriNetStructure net = compiler.compile(diagram)

        then:
        findPlaceByLabel(net, 'P_if_1_decision') != null
        findPlaceByLabel(net, 'P_if_2_decision') != null
        findTransitionByLabel(net, 'T_if_1_yes')?.guardKey == 'g1'
        findTransitionByLabel(net, 'T_if_2_yes')?.guardKey == 'g2'
        net.transitions*.actionKey.containsAll(['a', 'b', 'c', 'd'])

        and: 'the first conditional exits into the second conditional'
        def exit1 = findTransitionByLabel(net, 'T_if_1_to_end')
        def secondEntry = findPlaceByLabel(net, 'P_1')
        net.incidenceMatrix.getOutputWeight(secondEntry.index, exit1.index) == 1
        def toDecision2 = findTransitionByLabel(net, 'T_if_2_to_decision')
        net.incidenceMatrix.getInputWeight(secondEntry.index, toDecision2.index) == 1
    }

    def 'should compile a conditional followed by a repeat loop in written order'() {
        given:
        def diagram = diagram(true, true, [
            conditional('g', actions('a'), actions('b')),
            new eu.describeit.plantflow.ast.RepeatNode('c', [action('l')]),
        ])

        when:
        PetriNetStructure net = compiler.compile(diagram)

        then: 'the conditional takes id 1 and the loop id 2 from the shared counter'
        findPlaceByLabel(net, 'P_if_1_decision') != null
        findPlaceByLabel(net, 'P_repeat_2_decision') != null
        findTransitionByLabel(net, 'T_repeat_2_loop_back')?.guardKey == 'c'
        net.transitions*.actionKey.containsAll(['a', 'b', 'l'])

        and: 'the conditional merge exits into the loop start place'
        def exit = findTransitionByLabel(net, 'T_if_1_to_end')
        def loopStart = findPlaceByLabel(net, 'P_1')
        net.incidenceMatrix.getOutputWeight(loopStart.index, exit.index) == 1
        def loopBack = findTransitionByLabel(net, 'T_repeat_2_loop_back')
        net.incidenceMatrix.getOutputWeight(loopStart.index, loopBack.index) == 1
    }

    def 'should compile nested conditionals with unique labels at any depth'() {
        given:
        def inner = conditional('g2', actions('a'), actions('b'))
        def outer = conditional('g1', [inner] as List<eu.describeit.plantflow.ast.ActivityNode>, actions('c'))
        def diagram = diagram(true, true, [outer])

        when:
        PetriNetStructure net = compiler.compile(diagram)

        then:
        findPlaceByLabel(net, 'P_if_1_decision') != null
        findPlaceByLabel(net, 'P_if_2_decision') != null
        findPlaceByLabel(net, 'P_if_1_endif') != null
        findPlaceByLabel(net, 'P_if_2_endif') != null
        findTransitionByLabel(net, 'T_if_2_to_end') != null
    }

    def 'should keep pre and post actions around a conditional in order'() {
        given:
        def diagram = diagram(true, true, [
            action('pre'),
            conditional('g', actions('a'), actions('b')),
            action('post'),
        ])

        when:
        PetriNetStructure net = compiler.compile(diagram)

        then:
        net.transitions*.actionKey.containsAll(['pre', 'a', 'b', 'post'])
        def labels = net.places*.label
        labels.containsAll(['start', 'P_1', 'P_2', 'P_if_1_decision', 'P_if_1_then', 'P_if_1_else', 'P_if_1_endif', 'end'])

        and: 'the pre action feeds the conditional and the post action follows its merge'
        def pre = findTransitionByAction(net, 'pre')
        def toDecision = findTransitionByLabel(net, 'T_if_1_to_decision')
        net.incidenceMatrix.getOutputWeight(findPlaceByLabel(net, 'P_1').index, pre.index) == 1
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_1').index, toDecision.index) == 1
        def post = findTransitionByAction(net, 'post')
        def exit = findTransitionByLabel(net, 'T_if_1_to_end')
        net.incidenceMatrix.getOutputWeight(findPlaceByLabel(net, 'P_2').index, exit.index) == 1
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_2').index, post.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, post.index) == 1
    }

    def 'should throw for an empty diagram'() {
        when:
        compiler.compile(diagram(true, true, []))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Diagram must contain at least one action transition'
    }

    def 'should throw for an unsupported fork node inside a chain'() {
        given:
        def fork = new ForkNode([[action('b1')], [action('b2')]] as List<List<eu.describeit.plantflow.ast.ActivityNode>>)
        def diagram = diagram(true, true, [action('pre'), fork])

        when:
        compiler.compile(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.startsWith('Unsupported node type in chain: ')
    }
}
