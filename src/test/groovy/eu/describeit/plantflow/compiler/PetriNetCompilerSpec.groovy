package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.PetriNetCompiler
import eu.describeit.plantflow.PetriNetTestUtil
import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.ValidationTestUtil
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

    def 'should compile ForkNode via compileFork method'() {
        given:
        def branch1 = [new ActionNode('fork_a')]
        def branch2 = [new ActionNode('fork_b')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        net.startPlace != null
        net.endPlace != null

        and:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        forkTransition != null
        mergeSyncTransition != null
    }

    @Unroll
    def 'should compile fork AST with #branches.size() branches'() {
        given:
        def forkNode = new ForkNode(branches as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        net.startPlace != null
        net.endPlace != null

        and:
        net.transitions.find { it.label == 'T_fork_1' } != null
        net.transitions.find { it.label == 'T_fork_1_merge_sync' } != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } != null

        and:
        allActions.every { action -> net.transitions.find { it.actionKey == action } != null }

        where:
        branches                                                            | allActions
        [[new ActionNode('fork_a'), new ActionNode('fork_b')], [new ActionNode('fork_c')]] | ['fork_a', 'fork_b', 'fork_c']
        [[new ActionNode('action1')], [new ActionNode('action2')], [new ActionNode('action3')]] | ['action1', 'action2', 'action3']
    }

    def 'should compile fork AST with multiple action branches and verify transitions and place naming'() {
        given:
        def branch1 = [new ActionNode('a1'), new ActionNode('a2')]
        def branch2 = [new ActionNode('b1'), new ActionNode('b2'), new ActionNode('b3')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0

        and:
        net.transitions.find { it.actionKey == 'a1' } != null
        net.transitions.find { it.actionKey == 'a2' } != null
        net.transitions.find { it.actionKey == 'b1' } != null
        net.transitions.find { it.actionKey == 'b2' } != null
        net.transitions.find { it.actionKey == 'b3' } != null

        and:
        net.places.findAll { it.label.contains('P_fork_1_branch_1') }.size() >= 2
        net.places.findAll { it.label.contains('P_fork_1_branch_2') }.size() >= 3
        net.places.find { it.label == 'P_fork_1_branch_1_end' } != null
        net.places.find { it.label == 'P_fork_1_branch_2_end' } != null
    }

    def 'should throw IllegalArgumentException for null fork node'() {
        when:
        PetriNetCompiler.compileFork(null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_FORK_NODE_NULL
    }

    def 'should throw IllegalArgumentException for fork node with null branches'() {
        when:
        PetriNetCompiler.compileFork(new ForkNode(null))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_BRANCH_EMPTY
    }

    def 'should throw IllegalArgumentException for fork node with empty branches'() {
        when:
        PetriNetCompiler.compileFork(new ForkNode([]))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_BRANCH_EMPTY
    }

    def 'should throw IllegalArgumentException for fork node with empty branch'() {
        given:
        def branch1 = [new ActionNode('action1')]
        def branch2 = []
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        PetriNetCompiler.compileFork(forkNode)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_BRANCH_EMPTY
    }

    def 'should throw IllegalArgumentException for fork node with null branch'() {
        given:
        def branch1 = [new ActionNode('action1')]
        def forkNode = new ForkNode([branch1, null] as List<List<ActivityNode>>)

        when:
        PetriNetCompiler.compileFork(forkNode)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_BRANCH_EMPTY
    }

    def 'should compile fork diagram with post-merge actions'() {
        given:
        def branch1 = [new ActionNode('action 1')]
        def branch2 = [new ActionNode('action 2')]
        def branch3 = [new ActionNode('action 3'), new ActionNode('action 4')]
        def forkNode = new ForkNode([branch1, branch2, branch3] as List<List<ActivityNode>>)
        def postAction = new ActionNode('action 5')
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode, postAction])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null

        and:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        forkTransition != null
        mergeSyncTransition != null

        and:
        def action5Transition = net.transitions.find { it.actionKey == 'action 5' }
        action5Transition != null

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null

        and:
        net.incidenceMatrix.getInputWeight(mergePlace.index, action5Transition.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, action5Transition.index) == 1
    }

    def 'should compile fork with postForkActions via compileFork two-arg method'() {
        given:
        def branch1 = [new ActionNode('branch a')]
        def branch2 = [new ActionNode('branch b')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)
        def postActions = [new ActionNode('post 1'), new ActionNode('post 2')]

        when:
        def net = PetriNetCompiler.compileFork(forkNode, postActions)

        then:
        net != null
        def post1 = net.transitions.find { it.actionKey == 'post 1' }
        def post2 = net.transitions.find { it.actionKey == 'post 2' }
        post1 != null
        post2 != null

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null
        net.incidenceMatrix.getInputWeight(mergePlace.index, post1.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, post2.index) == 1
    }

    def 'should compile fork diagram with another ForkNode after first fork (second fork ignored in post-fork extraction)'() {
        given:
        def branch1 = [new ActionNode('action 1')]
        def branch2 = [new ActionNode('action 2')]
        def forkNode1 = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)
        def branch3 = [new ActionNode('action 3')]
        def branch4 = [new ActionNode('action 4')]
        def forkNode2 = new ForkNode([branch3, branch4] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode1, forkNode2])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.actionKey == 'action 1' } != null
        net.transitions.find { it.actionKey == 'action 2' } != null
    }

    def 'should compile fork with correct branch transition and place labels'() {
        given:
        def branch1 = [new ActionNode('a1')]
        def branch2 = [new ActionNode('a2')]
        def branch3 = [new ActionNode('a3')]
        def forkNode = new ForkNode([branch1, branch2, branch3] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net.places.find { it.label == 'P_fork_1_branch_1' } != null
        net.places.find { it.label == 'P_fork_1_branch_2' } != null
        net.places.find { it.label == 'P_fork_1_branch_3' } != null

        and:
        net.places.find { it.label == 'P_fork_1_branch_1_end' } != null
        net.places.find { it.label == 'P_fork_1_branch_2_end' } != null
        net.places.find { it.label == 'P_fork_1_branch_3_end' } != null

        and:
        net.transitions.find { it.label == 'T_fork_1_branch_1' } != null
        net.transitions.find { it.label == 'T_fork_1_branch_2' } != null
        net.transitions.find { it.label == 'T_fork_1_branch_3' } != null
    }

    def 'should compile fork with two branches and verify fork transition has 1 input and N outputs'() {
        given:
        def branch1 = [new ActionNode('a1')]
        def branch2 = [new ActionNode('a2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        forkTransition != null

        and:
        PetriNetTestUtil.verifyTransitionIO(net, forkTransition, 1, 2)

        and:
        net.incidenceMatrix.getInputWeight(net.startPlace.index, forkTransition.index) == 1
    }

    def 'should compile fork with three branches and verify merge transition has N inputs and 1 output'() {
        given:
        def branch1 = [new ActionNode('a1')]
        def branch2 = [new ActionNode('a2')]
        def branch3 = [new ActionNode('a3')]
        def forkNode = new ForkNode([branch1, branch2, branch3] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null

        and:
        PetriNetTestUtil.verifyTransitionIO(net, mergeSyncTransition, 3, 1)

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null
        net.incidenceMatrix.getOutputWeight(mergePlace.index, mergeSyncTransition.index) == 1
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

    def 'should compile linear diagram with single action'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('single action')])

        when:
        def net = PetriNetCompiler.compileLinear(diagram)

        then:
        net != null
        net.places.size() == 2
        net.transitions.size() == 1
        net.transitions[0].actionKey == 'single action'
    }

    def 'should compile linear diagram with many actions'() {
        given:
        def actions = (1..5).collect { new ActionNode("action ${it}") }
        def diagram = new ActivityDiagram(true, true, actions)

        when:
        def net = PetriNetCompiler.compileLinear(diagram)

        then:
        net != null
        net.places.size() == 6
        net.transitions.size() == 5
        (0..4).each { i ->
            assert net.transitions[i].actionKey == "action ${i + 1}"
        }
    }

    def 'should compile conditional diagram with single action in each branch'() {
        given:
        def condNode = new ConditionalNode('isValid', [new ActionNode('then action')], [new ActionNode('else action')])
        def diagram = new ActivityDiagram(true, true, [condNode]).withIf().withElse().withEndif()

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.places.size() == 6
        net.transitions.size() == 6
    }

    def 'should extract post-fork actions from diagram with fork node followed by actions'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def postAction1 = new ActionNode('post 1')
        def postAction2 = new ActionNode('post 2')
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode, postAction1, postAction2])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.actionKey == 'post 1' } != null
        net.transitions.find { it.actionKey == 'post 2' } != null
    }

    def 'should extract post-fork actions from diagram with only fork node (no post actions)'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } != null
    }

    def 'should handle extractPostForkActions with non-ActionNode after fork'() {
        given: 'a diagram with fork node followed by another fork node (no ActionNode in between)'
        def forkNode1 = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def forkNode2 = new ForkNode([
            [new ActionNode('branch 3')],
            [new ActionNode('branch 4')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode1, forkNode2])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.actionKey == 'branch 1' } != null
        net.transitions.find { it.actionKey == 'branch 2' } != null
    }

    def 'should compile fork with two-arg compileFork with empty postForkActions'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a1')],
            [new ActionNode('a2')]
        ] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode, [])

        then:
        net != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } != null
    }

    def 'should compile fork with two-arg compileFork with non-empty postForkActions'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a1')],
            [new ActionNode('a2')]
        ] as List<List<ActivityNode>>)
        def postActions = [new ActionNode('post1'), new ActionNode('post2')]

        when:
        def net = PetriNetCompiler.compileFork(forkNode, postActions)

        then:
        net != null
        net.transitions.find { it.actionKey == 'post1' } != null
        net.transitions.find { it.actionKey == 'post2' } != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } == null
    }

    def 'should compile fork with single-arg compileFork'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a1')],
            [new ActionNode('a2')]
        ] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } != null
    }

    def 'should handle generateActionTransitions with single action'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('only action')])

        when:
        def net = PetriNetCompiler.compileLinear(diagram)

        then:
        net.transitions.size() == 1
        net.transitions[0].actionKey == 'only action'
    }

    def 'should compile diagram with no conditional or fork nodes as linear'() {
        given:
        def diagram = new ActivityDiagram(true, true, [
            new ActionNode('action 1'),
            new ActionNode('action 2')
        ])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
    }

    def 'should handle extractPostForkActions with ActionNode before fork node'() {
        given: 'a diagram with ActionNode before ForkNode and ActionNode after'
        def preAction = new ActionNode('pre action')
        def forkNode = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def postAction = new ActionNode('post action')
        def diagram = new ActivityDiagram(true, true, true, true, [preAction, forkNode, postAction])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.actionKey == 'branch 1' } != null
        net.transitions.find { it.actionKey == 'branch 2' } != null
        net.transitions.find { it.actionKey == 'post action' } != null
    }

    def 'should handle extractPostForkActions with only fork node in diagram'() {
        given: 'a diagram with only a ForkNode (no other nodes)'
        def forkNode = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.label == 'T_fork_1_to_end' } != null
    }

    def 'should handle extractPostForkActions with multiple non-ActionNodes after fork'() {
        given: 'a diagram with fork node followed by multiple fork nodes'
        def forkNode1 = new ForkNode([
            [new ActionNode('branch 1')],
            [new ActionNode('branch 2')]
        ] as List<List<ActivityNode>>)
        def forkNode2 = new ForkNode([
            [new ActionNode('branch 3')],
            [new ActionNode('branch 4')]
        ] as List<List<ActivityNode>>)
        def forkNode3 = new ForkNode([
            [new ActionNode('branch 5')],
            [new ActionNode('branch 6')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode1, forkNode2, forkNode3])

        when:
        def net = PetriNetCompiler.compile(diagram)

        then:
        net != null
        net.transitions.find { it.actionKey == 'branch 1' } != null
        net.transitions.find { it.actionKey == 'branch 2' } != null
    }

    def 'should compile fork with branches containing single action each'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('only a')],
            [new ActionNode('only b')]
        ] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        net.transitions.find { it.actionKey == 'only a' } != null
        net.transitions.find { it.actionKey == 'only b' } != null
    }

    def 'should compile fork with branches containing many actions'() {
        given:
        def branch1 = (1..5).collect { new ActionNode("a${it}") }
        def branch2 = (1..3).collect { new ActionNode("b${it}") }
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = PetriNetCompiler.compileFork(forkNode)

        then:
        net != null
        (1..5).each { i -> assert net.transitions.find { t -> t.actionKey == "a${i}" } != null }
        (1..3).each { i -> assert net.transitions.find { t -> t.actionKey == "b${i}" } != null }
    }

    def 'extractActionNodes should filter only ActionNodes from mixed node list'() {
        given: 'a diagram with ActionNodes mixed with ForkNode and ConditionalNode'
        def action1 = new ActionNode('action 1')
        def action2 = new ActionNode('action 2')
        def forkNode = new ForkNode([
            [new ActionNode('branch a')],
            [new ActionNode('branch b')]
        ] as List<List<ActivityNode>>)
        def condNode = new ConditionalNode('guard', [new ActionNode('then')], [new ActionNode('else')])
        def diagram = new ActivityDiagram(true, true, [action1, forkNode, action2, condNode])

        when: 'compileLinear is called (which uses extractActionNodes internally)'
        def net = PetriNetCompiler.compileLinear(diagram)

        then: 'only ActionNodes are extracted as transitions, not ForkNode or ConditionalNode'
        net != null
        net.transitions.size() == 2
        net.transitions[0].actionKey == 'action 1'
        net.transitions[1].actionKey == 'action 2'
    }

    def 'extractActionNodes should return empty list when no ActionNodes present'() {
        given: 'a diagram with only a ForkNode (no direct ActionNodes)'
        def forkNode = new ForkNode([
            [new ActionNode('branch a')],
            [new ActionNode('branch b')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, [forkNode])

        when:
        PetriNetCompiler.compileLinear(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == PetriNetCompiler.ERR_NO_ACTIONS
    }

    def 'extractActionNodes should return all nodes when all are ActionNodes'() {
        given:
        def diagram = new ActivityDiagram(true, true, [
            new ActionNode('a'),
            new ActionNode('b'),
            new ActionNode('c')
        ])

        when:
        def net = PetriNetCompiler.compileLinear(diagram)

        then:
        net.transitions.size() == 3
        net.transitions.every { it.actionKey != null }
    }

    def 'assembleConditionalNet should create correct connections via builder.with closure'() {
        given:
        def condNode = new ConditionalNode('myGuard', [new ActionNode('then action')], [new ActionNode('else action')])

        when:
        def net = PetriNetCompiler.compileConditional(condNode)

        then: 'all connections from builder.with closure are properly established'
        net != null
        net.places.size() == 6
        net.transitions.size() == 6

        and: 'start connects to decision'
        def startToDecision = net.transitions.find { it.label == 'T_start_to_decision' }
        net.incidenceMatrix.getInputWeight(0, startToDecision.index) == 1
        net.incidenceMatrix.getOutputWeight(1, startToDecision.index) == 1

        and: 'decision connects to both branches'
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }
        def branchNo = net.transitions.find { it.label == 'T_branch_no' }
        net.incidenceMatrix.getInputWeight(1, branchYes.index) == 1
        net.incidenceMatrix.getInputWeight(1, branchNo.index) == 1

        and: 'endif connects to end (last connection in builder.with closure)'
        def endifToEnd = net.transitions.find { it.label == 'T_endif_to_end' }
        def endifPlace = net.places.find { it.label == 'P_endif' }
        net.incidenceMatrix.getInputWeight(endifPlace.index, endifToEnd.index) == 1
        net.incidenceMatrix.getOutputWeight(5, endifToEnd.index) == 1
    }
}
