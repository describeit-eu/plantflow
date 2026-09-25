package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.engine.PetriNet
import spock.lang.Specification
import spock.lang.Unroll

class PetriNetCompilerSpec extends Specification {

    def 'should compile single-action linear AST into PetriNet'() {
        given:
        def action = new ActionNode('process order')
        def diagram = new ActivityDiagram(true, true, [action])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.places.size() == 2
        net.transitions.size() == 1

        and:
        net.startPlace.index == 0
        net.startPlace.label == PetriNetCompiler.PLACE_START
        net.endPlace.index == 1
        net.endPlace.label == PetriNetCompiler.PLACE_END

        and:
        def transition = net.transitions[0]
        transition.index == 0
        transition.label == 'process order'
        transition.actionKey == 'process order'
        transition.guardKey == null

        and:
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(0, 0) == 0
        net.incidenceMatrix.getInputWeight(1, 0) == 0
        net.incidenceMatrix.getOutputWeight(1, 0) == 1
        net.incidenceMatrix.getIncidence(0, 0) == -1
        net.incidenceMatrix.getIncidence(1, 0) == 1
    }

    def 'should compile two-action linear AST into PetriNet'() {
        given:
        def diagram = new ActivityDiagram(true, true, [
            new ActionNode('Hello world'),
            new ActionNode('groovy goodness')
        ])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net.places.size() == 3
        net.transitions.size() == 2

        and:
        net.places[0].label == PetriNetCompiler.PLACE_START
        net.places[1].label == 'P_1'
        net.places[2].label == PetriNetCompiler.PLACE_END
        net.startPlace == net.places[0]
        net.endPlace == net.places[2]

        and:
        net.transitions[0].label == 'Hello world'
        net.transitions[0].actionKey == 'Hello world'
        net.transitions[1].label == 'groovy goodness'
        net.transitions[1].actionKey == 'groovy goodness'

        and:
        // T_0: consumes from P_start (0), produces to P_1 (1)
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(1, 0) == 1
        net.incidenceMatrix.getIncidence(0, 0) == -1
        net.incidenceMatrix.getIncidence(1, 0) == 1

        // T_1: consumes from P_1 (1), produces to P_end (2)
        net.incidenceMatrix.getInputWeight(1, 1) == 1
        net.incidenceMatrix.getOutputWeight(2, 1) == 1
        net.incidenceMatrix.getIncidence(1, 1) == -1
        net.incidenceMatrix.getIncidence(2, 1) == 1
    }

    def 'should compile multi-action linear AST with sequential intermediate places and arcs'() {
        given:
        def actions = ['step 1', 'step 2', 'step 3', 'step 4']
        def actionNodes = actions.collect { String name -> new ActionNode(name) }
        def diagram = new ActivityDiagram(true, true, actionNodes)

        when:
        def net = PetriNetCompiler.compileLinear(diagram)

        then:
        net.places.size() == 5
        net.transitions.size() == 4

        and:
        net.places[0].label == PetriNetCompiler.PLACE_START
        net.places[1].label == 'P_1'
        net.places[2].label == 'P_2'
        net.places[3].label == 'P_3'
        net.places[4].label == PetriNetCompiler.PLACE_END

        and:
        (0..3).each { int i ->
            assert net.transitions[i].label == actions[i]
            assert net.transitions[i].actionKey == actions[i]
            assert net.transitions[i].guardKey == null
            assert net.incidenceMatrix.getInputWeight(i, i) == 1
            assert net.incidenceMatrix.getOutputWeight(i + 1, i) == 1
            assert net.incidenceMatrix.getIncidence(i, i) == -1
            assert net.incidenceMatrix.getIncidence(i + 1, i) == 1
        }
    }

    def 'should compile conditional AST with single action in branches'() {
        given:
        def guard = "actions['process all']"
        def thenNode = new ActionNode('process all')
        def elseNode = new ActionNode('process none')
        def condNode = new ConditionalNode(guard, [thenNode], [elseNode])
        def diagram = new ActivityDiagram(true, true, [condNode]).withIf().withElse().withEndif()

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.places.size() == 6
        net.transitions.size() == 6

        and:
        net.startPlace.index == 0
        net.startPlace.label == PetriNetCompiler.PLACE_START
        net.endPlace.index == 5
        net.endPlace.label == PetriNetCompiler.PLACE_END

        and:
        def decisionPlace = net.places.find { it.label == PetriNetCompiler.PLACE_IF_DECISION }
        def thenPlace = net.places.find { it.label == PetriNetCompiler.PLACE_THEN }
        def elsePlace = net.places.find { it.label == PetriNetCompiler.PLACE_ELSE }
        def endifPlace = net.places.find { it.label == PetriNetCompiler.PLACE_ENDIF }

        decisionPlace != null
        decisionPlace.index == 1
        thenPlace != null
        thenPlace.index == 2
        elsePlace != null
        elsePlace.index == 3
        endifPlace != null
        endifPlace.index == 4

        and:
        def startToDecision = net.transitions.find { it.label == PetriNetCompiler.TRANSITION_START_TO_DECISION }
        def branchYes = net.transitions.find { it.label == PetriNetCompiler.TRANSITION_BRANCH_YES }
        def branchNo = net.transitions.find { it.label == PetriNetCompiler.TRANSITION_BRANCH_NO }
        def actionThen = net.transitions.find { it.label == 'process all' }
        def actionElse = net.transitions.find { it.label == 'process none' }
        def endifToEnd = net.transitions.find { it.label == PetriNetCompiler.TRANSITION_ENDIF_TO_END }

        startToDecision != null
        branchYes != null
        branchNo != null
        actionThen != null
        actionElse != null
        endifToEnd != null

        and:
        branchYes.guardKey == guard
        branchNo.guardKey == "!(" + guard + ")"
        actionThen.actionKey == 'process all'
        actionElse.actionKey == 'process none'
        startToDecision.actionKey == null
        branchYes.actionKey == null
        branchNo.actionKey == null
        endifToEnd.actionKey == null

        and:
        // T_start_to_decision: start (0) -> P_if_decision (1)
        net.incidenceMatrix.getInputWeight(0, startToDecision.index) == 1
        net.incidenceMatrix.getOutputWeight(1, startToDecision.index) == 1

        // T_branch_yes: P_if_decision (1) -> P_then (2)
        net.incidenceMatrix.getInputWeight(1, branchYes.index) == 1
        net.incidenceMatrix.getOutputWeight(2, branchYes.index) == 1

        // T_branch_no: P_if_decision (1) -> P_else (3)
        net.incidenceMatrix.getInputWeight(1, branchNo.index) == 1
        net.incidenceMatrix.getOutputWeight(3, branchNo.index) == 1

        // actionThen: P_then (2) -> P_endif (4)
        net.incidenceMatrix.getInputWeight(2, actionThen.index) == 1
        net.incidenceMatrix.getOutputWeight(4, actionThen.index) == 1

        // actionElse: P_else (3) -> P_endif (4)
        net.incidenceMatrix.getInputWeight(3, actionElse.index) == 1
        net.incidenceMatrix.getOutputWeight(4, actionElse.index) == 1

        // T_endif_to_end: P_endif (4) -> end (5)
        net.incidenceMatrix.getInputWeight(4, endifToEnd.index) == 1
        net.incidenceMatrix.getOutputWeight(5, endifToEnd.index) == 1
    }

    def 'should compile conditional AST with multiple actions in then and else branches'() {
        given:
        def guard = 'isValid'
        def thenActions = [new ActionNode('then_a'), new ActionNode('then_b')]
        def elseActions = [new ActionNode('else_a'), new ActionNode('else_b'), new ActionNode('else_c')]
        def condNode = new ConditionalNode(guard, thenActions, elseActions)

        when:
        def net = PetriNetCompiler.compileConditional(condNode)

        then:
        // Places: start (1) + if_decision (1) + then (2) + else (3) + endif (1) + end (1) = 9
        net.places.size() == 9
        // Transitions: start_to_decision (1) + branch_yes (1) + branch_no (1) + then (2) + else (3) + endif_to_end (1) = 9
        net.transitions.size() == 9

        and:
        net.places[0].label == 'start'
        net.places[1].label == 'P_if_decision'
        net.places[2].label == 'P_then'
        net.places[3].label == 'P_then_1'
        net.places[4].label == 'P_else'
        net.places[5].label == 'P_else_1'
        net.places[6].label == 'P_else_2'
        net.places[7].label == 'P_endif'
        net.places[8].label == 'end'

        and:
        net.startPlace == net.places[0]
        net.endPlace == net.places[8]

        and:
        net.transitions[0].label == 'T_start_to_decision'
        net.transitions[1].label == 'T_branch_yes'
        net.transitions[1].guardKey == 'isValid'
        net.transitions[2].label == 'T_branch_no'
        net.transitions[2].guardKey == '!(isValid)'

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

        and:
        // T_0: start (0) -> P_if_decision (1)
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(1, 0) == 1

        // T_1 (branch_yes): P_if_decision (1) -> P_then (2)
        net.incidenceMatrix.getInputWeight(1, 1) == 1
        net.incidenceMatrix.getOutputWeight(2, 1) == 1

        // T_2 (branch_no): P_if_decision (1) -> P_else (4)
        net.incidenceMatrix.getInputWeight(1, 2) == 1
        net.incidenceMatrix.getOutputWeight(4, 2) == 1

        // then_a (3): P_then (2) -> P_then_1 (3)
        net.incidenceMatrix.getInputWeight(2, 3) == 1
        net.incidenceMatrix.getOutputWeight(3, 3) == 1

        // then_b (4): P_then_1 (3) -> P_endif (7)
        net.incidenceMatrix.getInputWeight(3, 4) == 1
        net.incidenceMatrix.getOutputWeight(7, 4) == 1

        // else_a (5): P_else (4) -> P_else_1 (5)
        net.incidenceMatrix.getInputWeight(4, 5) == 1
        net.incidenceMatrix.getOutputWeight(5, 5) == 1

        // else_b (6): P_else_1 (5) -> P_else_2 (6)
        net.incidenceMatrix.getInputWeight(5, 6) == 1
        net.incidenceMatrix.getOutputWeight(6, 6) == 1

        // else_c (7): P_else_2 (6) -> P_endif (7)
        net.incidenceMatrix.getInputWeight(6, 7) == 1
        net.incidenceMatrix.getOutputWeight(7, 7) == 1

        // T_endif_to_end (8): P_endif (7) -> end (8)
        net.incidenceMatrix.getInputWeight(7, 8) == 1
        net.incidenceMatrix.getOutputWeight(8, 8) == 1
    }

    @Unroll
    def 'should throw IllegalArgumentException for edge case: #scenario'() {
        when:
        action.call()

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario | action | expectedMessage
        'null diagram to compile' | { PetriNetCompiler.compile(null) } | PetriNetCompiler.ERR_DIAGRAM_NULL
        'null diagram to compileLinear' | { PetriNetCompiler.compileLinear(null) } | PetriNetCompiler.ERR_DIAGRAM_NULL
        'empty linear diagram' | { PetriNetCompiler.compileLinear(new ActivityDiagram(true, true, [])) } | PetriNetCompiler.ERR_NO_ACTIONS
        'null conditional node' | { PetriNetCompiler.compileConditional(null) } | PetriNetCompiler.ERR_CONDITIONAL_NODE_NULL
        'empty then actions' | { PetriNetCompiler.compileConditional(new ConditionalNode('cond', [], [new ActionNode('a')])) } | PetriNetCompiler.ERR_THEN_EMPTY
        'empty else actions' | { PetriNetCompiler.compileConditional(new ConditionalNode('cond', [new ActionNode('a')], [])) } | PetriNetCompiler.ERR_ELSE_EMPTY
        'conditional diagram missing conditional node' | { PetriNetCompiler.compile(new ActivityDiagram(true, true, []).withIf()) } | PetriNetCompiler.ERR_THEN_EMPTY
    }
}
