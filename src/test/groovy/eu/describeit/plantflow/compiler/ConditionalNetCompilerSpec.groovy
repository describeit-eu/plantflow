package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class ConditionalNetCompilerSpec extends Specification {

    def compiler = new ConditionalNetCompiler()

    def 'should compile conditional with single action in each branch'() {
        given:
        def guard = "actions['process all']"
        def node = conditional(guard, actions('process all'), actions('process none'))

        when:
        def net = compiler.compile(node)

        then:
        validateNetStructure(net, 6, 6)
        validatePlaceLabels(net, ['start', 'P_if_decision', 'P_then', 'P_else', 'P_endif', 'end'])

        and:
        verifyConditionalStructure(net, guard)

        and:
        def startToDecision = findTransitionByLabel(net, 'T_start_to_decision')
        def branchYes = findTransitionByLabel(net, 'T_branch_yes')
        def branchNo = findTransitionByLabel(net, 'T_branch_no')
        def actionThen = findTransitionByAction(net, 'process all')
        def actionElse = findTransitionByAction(net, 'process none')
        def endifToEnd = findTransitionByLabel(net, 'T_endif_to_end')

        startToDecision != null
        branchYes != null
        branchNo != null
        actionThen != null
        actionElse != null
        endifToEnd != null

        and:
        startToDecision.actionKey == null
        branchYes.actionKey == null
        branchNo.actionKey == null
        endifToEnd.actionKey == null
    }

    def 'should compile conditional with multiple actions in branches'() {
        given:
        def node = conditional('isValid', actions('then_a', 'then_b'), actions('else_a', 'else_b', 'else_c'))

        when:
        def net = compiler.compile(node)

        then:
        validateNetStructure(net, 9, 9)
        validatePlaceLabels(net, ['start', 'P_if_decision', 'P_then', 'P_then_1', 'P_else', 'P_else_1', 'P_else_2', 'P_endif', 'end'])

        and:
        verifyConditionalStructure(net, 'isValid')

        and:
        net.transitions[3].label == 'then_a'
        net.transitions[3].actionKey == 'then_a'
        net.transitions[4].label == 'then_b'
        net.transitions[4].actionKey == 'then_b'
        net.transitions[5].label == 'else_a'
        net.transitions[5].actionKey == 'else_a'
        net.transitions[6].label == 'else_b'
        net.transitions[6].actionKey == 'else_b'
        net.transitions[7].label == 'else_c'
        net.transitions[7].actionKey == 'else_c'
        net.transitions[8].label == 'T_endif_to_end'
    }

    def 'should connect start to decision to branches to endif to end'() {
        given:
        def node = conditional('myGuard', actions('then action'), actions('else action'))

        when:
        def net = compiler.compile(node)

        then:
        def startToDecision = findTransitionByLabel(net, 'T_start_to_decision')
        verifyTransitionConnection(net, startToDecision.index, 0, 1)

        and:
        def branchYes = findTransitionByLabel(net, 'T_branch_yes')
        def branchNo = findTransitionByLabel(net, 'T_branch_no')
        def decisionPlace = findPlaceByLabel(net, 'P_if_decision')
        verifyTransitionIO(net, branchYes, 1, 1)
        verifyTransitionIO(net, branchNo, 1, 1)

        and:
        def endifToEnd = findTransitionByLabel(net, 'T_endif_to_end')
        def endifPlace = findPlaceByLabel(net, 'P_endif')
        net.incidenceMatrix.getInputWeight(endifPlace.index, endifToEnd.index) == 1
        net.incidenceMatrix.getOutputWeight(5, endifToEnd.index) == 1
    }

    def 'should create explicit intermediate branch places per ADR-0007'() {
        given:
        def node = conditional('guard', actions('a1', 'a2'), actions('b1'))

        when:
        def net = compiler.compile(node)

        then:
        findPlaceByLabel(net, 'P_then') != null
        findPlaceByLabel(net, 'P_then_1') != null
        findPlaceByLabel(net, 'P_else') != null
        findPlaceByLabel(net, 'P_endif') != null
    }

    @Unroll
    def 'should throw IllegalArgumentException for #scenario'() {
        when:
        compiler.compile(node)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario              | node                                                              | expectedMessage
        'null node'           | null                                                              | 'ConditionalNode cannot be null'
        'empty then actions'  | conditional('cond', Collections.<ActionNode>emptyList(), actions('a')) | 'Then block must contain at least one action'
        'empty else actions'  | conditional('cond', actions('a'), Collections.<ActionNode>emptyList()) | 'Else block must contain at least one action'
    }
}
