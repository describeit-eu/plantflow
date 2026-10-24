package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Meta-tests for PlantFlowTestKit: verifies that all builders, factories,
 * and assertion helpers work correctly.
 */
class PlantFlowTestKitSpec extends Specification {

    // ==================== Net Builders ====================

    def 'simpleLinearNet should create a basic 2-place 1-transition net'() {
        when:
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)

        then:
        net.places.size() == 2
        net.transitions.size() == 1
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        net.transitions[0].label == 'action'
        net.transitions[0].actionKey == 'myAction'
    }

    def 'simpleLinearNet should use defaults when no arguments given'() {
        when:
        def net = simpleLinearNet()

        then:
        net.places.size() == 2
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        net.transitions[0].label == 'action'
    }

    // ==================== Component Factories ====================

    def 'place should create a Place with correct index and label'() {
        when:
        def p = place(5, 'myPlace')

        then:
        p.index == 5
        p.label == 'myPlace'
    }

    def 'transition should create a Transition with correct parameters'() {
        when:
        def t = transition(3, 'myTransition', 'actionKey', 'guardKey')

        then:
        t.index == 3
        t.label == 'myTransition'
        t.actionKey == 'actionKey'
        t.guardKey == 'guardKey'
    }

    def 'transition should create a Transition with null action and guard keys'() {
        when:
        def t = transition(0, 'structural')

        then:
        t.actionKey == null
        t.guardKey == null
    }

    def 'token with payload should create a Token with the given payload'() {
        when:
        def t = token([key: 'value', count: 42])

        then:
        t.payload == [key: 'value', count: 42]
        t.id != null
    }

    def 'token with no payload should create a Token with empty payload'() {
        when:
        def t = token()

        then:
        t.payload == [:]
    }

    def 'token with id and payload should create a Token with the given id and payload'() {
        when:
        def t = token('my-id', [key: 'value'])

        then:
        t.id == 'my-id'
        t.payload == [key: 'value']
    }

    def 'marking should create a Marking with the given place count'() {
        when:
        def m = marking(5)

        then:
        m != null
        m.tokens.length == 5
    }

    def 'incidenceMatrix should create an IncidenceMatrix with the given matrices'() {
        given:
        def input = [[1, 0], [0, 1]] as int[][]
        def output = [[0, 1], [1, 0]] as int[][]

        when:
        def matrix = incidenceMatrix(input, output)

        then:
        matrix.getInputWeight(0, 0) == 1
        matrix.getOutputWeight(0, 0) == 0
        matrix.getIncidence(0, 0) == -1
    }

    // ==================== Registry Builders ====================

    def 'registry should create an empty HandlerRegistry when no configurator given'() {
        when:
        def reg = registry()

        then:
        reg != null
    }

    def 'registry should apply configurator closure'() {
        when:
        def reg = registry { r ->
            r.registerAction('myAction') { ctx, tok -> tok }
        }

        then:
        reg.getAction('myAction') != null
    }

    def 'payloadRegistry should create handlers that add payload flags'() {
        given:
        def executionLog = []
        def reg = payloadRegistry(['action one', 'action two'], executionLog)
        def ctx = context()
        def tok = token([initial: true])

        when:
        def result1 = reg.getAction('action one').execute(ctx, tok)

        then:
        result1.payload.action_one == true
        result1.payload.initial == true
        executionLog == ['action one']

        when:
        def result2 = reg.getAction('action two').execute(ctx, result1)

        then:
        result2.payload.action_two == true
        result2.payload.action_one == true
        executionLog == ['action one', 'action two']
    }

    def 'payloadRegistry should work without execution log'() {
        given:
        def reg = payloadRegistry(['my action'])
        def ctx = context()
        def tok = token()

        when:
        def result = reg.getAction('my action').execute(ctx, tok)

        then:
        result.payload.my_action == true
    }

    // ==================== Context Factory ====================

    def 'context should create an ExecutionContext with initial values'() {
        when:
        def ctx = context([key: 'value', count: 42])

        then:
        ctx['key'] == 'value'
        ctx['count'] == 42
    }

    def 'context should create an empty ExecutionContext when no values given'() {
        when:
        def ctx = context()

        then:
        ctx != null
    }

    // ==================== PlantUML Builders ====================

    def 'linearPuml should generate correct PlantUML for single action'() {
        when:
        def puml = linearPuml('process order')

        then:
        puml.contains('@startuml')
        puml.contains('start')
        puml.contains(':process order;')
        puml.contains('end')
        puml.contains('@enduml')
    }

    def 'linearPuml should generate correct PlantUML for multiple actions'() {
        when:
        def puml = linearPuml('step 1', 'step 2')

        then:
        puml.contains(':step 1;')
        puml.contains(':step 2;')
    }

    def 'conditionalPuml with single actions should generate correct PlantUML'() {
        when:
        def puml = conditionalPuml('isValid', 'then action', 'else action')

        then:
        puml.contains('if ( isValid ) then (yes)')
        puml.contains(':then action;')
        puml.contains('else (no)')
        puml.contains(':else action;')
        puml.contains('endif')
    }

    def 'conditionalPuml with arrays should generate correct PlantUML for multiple actions'() {
        when:
        def puml = conditionalPuml('guard', ['t1', 't2'] as String[], ['e1', 'e2'] as String[])

        then:
        puml.contains('if ( guard ) then (yes)')
        puml.contains(':t1;')
        puml.contains(':t2;')
        puml.contains(':e1;')
        puml.contains(':e2;')
    }

    def 'forkPuml with branches and post-merge actions should generate correct PlantUML'() {
        when:
        def puml = forkPuml([['a1', 'a2'], ['b1']], ['after merge'])

        then:
        puml.contains('fork')
        puml.contains(':a1;')
        puml.contains(':a2;')
        puml.contains('fork again')
        puml.contains(':b1;')
        puml.contains('end merge')
        puml.contains(':after merge;')
    }

    def 'forkPuml with three branch lists should generate correct PlantUML'() {
        when:
        def puml = forkPuml(['a'], ['b'], ['c'])

        then:
        puml.contains(':a;')
        puml.contains(':b;')
        puml.contains(':c;')
        puml.count('fork again') == 2
    }

    def 'forkPuml with branch count should generate correct PlantUML'() {
        when:
        def puml = forkPuml(3)

        then:
        puml.contains(':action 1;')
        puml.contains(':action 2;')
        puml.contains(':action 3;')
    }

    // ==================== Parser Helpers ====================

    def 'parse from string should parse PlantUML content'() {
        when:
        def net = parse(linearPuml('test action'))

        then:
        net != null
        net.transitions.size() == 1
        net.transitions[0].actionKey == 'test action'
    }

    def 'parse from file should parse PlantUML file'() {
        when:
        def net = parse(new File('src/test/data/puml/sequence.puml'))

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
    }

    // ==================== AST Factories ====================

    def 'action should create an ActionNode with trimmed action'() {
        when:
        def node = action('  do something  ')

        then:
        node.action == 'do something'
        node.actionKey == 'do something'
    }

    def 'actions should create a list of ActionNodes'() {
        when:
        def nodes = actions('a', 'b', 'c')

        then:
        nodes.size() == 3
        nodes*.action == ['a', 'b', 'c']
    }

    def 'conditional with String arrays should create a ConditionalNode'() {
        when:
        def node = conditional('guard', ['then1', 'then2'] as String[], ['else1'] as String[])

        then:
        node.guardCondition == 'guard'
        node.thenActions.size() == 2
        node.thenActions[0].action == 'then1'
        node.elseActions.size() == 1
        node.elseActions[0].action == 'else1'
    }

    def 'conditional with ActionNode lists should create a ConditionalNode'() {
        given:
        def thenList = [action('then1')]
        def elseList = [action('else1')]

        when:
        def node = conditional('guard', thenList, elseList)

        then:
        node.guardCondition == 'guard'
        node.thenActions == thenList
        node.elseActions == elseList
    }

    def 'conditional with null lists should create a ConditionalNode with empty lists'() {
        when:
        def node = conditional('guard', (List<ActionNode>) null, (List<ActionNode>) null)

        then:
        node.thenActions.isEmpty()
        node.elseActions.isEmpty()
    }

    def 'fork with list should create a ForkNode'() {
        given:
        def branches = [action('a'), action('b')]

        when:
        def node = fork(branches)

        then:
        node.branches.size() == 2
    }

    def 'fork with varargs should create a ForkNode'() {
        when:
        def node = fork(action('a'), action('b'))

        then:
        node.branches.size() == 2
    }

    def 'fork with null should create a ForkNode with empty branches'() {
        when:
        def node = fork((List) null)

        then:
        node.branches.isEmpty()
    }

    def 'diagram with flags and nodes should create an ActivityDiagram'() {
        given:
        def nodes = [action('test')]

        when:
        def diagram = diagram(true, true, nodes)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes == nodes
    }

    def 'diagram with builder closure should create an ActivityDiagram'() {
        when:
        def diagram = diagram { d ->
            d.withStart().withEnd().addNode(action('test'))
        }

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes.size() == 1
    }

    // ==================== PlantFlow Execution Helpers ====================

    def 'runLinearFlow should create and run a linear workflow'() {
        given:
        def registry = payloadRegistry(['step one', 'step two'])

        when:
        def engine = runLinearFlow(['step one', 'step two'], registry)

        then:
        engine.isCompleted()
        engine.getEndToken().payload.step_one == true
        engine.getEndToken().payload.step_two == true
    }

    def 'runFlow should create and run a workflow from PUML'() {
        given:
        def registry = payloadRegistry(['my action'])

        when:
        def engine = runFlow(linearPuml('my action'), registry)

        then:
        engine.isCompleted()
        engine.getEndToken().payload.my_action == true
    }

    def 'verifyCompleted should pass for completed workflow'() {
        given:
        def registry = payloadRegistry(['step'])
        def engine = runLinearFlow(['step'], registry)

        when:
        verifyCompleted(engine)

        then:
        noExceptionThrown()
    }

    def 'verifyCompleted should fail for incomplete workflow'() {
        given:
        def engine = PlantFlow.from(linearPuml('step'), new HandlerRegistry())

        when:
        verifyCompleted(engine)

        then:
        thrown(AssertionError)
    }

    def 'verifyEndTokenPayload should pass for matching payload'() {
        given:
        def registry = payloadRegistry(['step'])
        def engine = runLinearFlow(['step'], registry, Token.of([init: true]))

        when:
        verifyEndTokenPayload(engine, [init: true, step: true])

        then:
        noExceptionThrown()
    }

    def 'verifyEndTokenPayload should fail for mismatched payload'() {
        given:
        def registry = payloadRegistry(['step'])
        def engine = runLinearFlow(['step'], registry)

        when:
        verifyEndTokenPayload(engine, [wrong: true])

        then:
        thrown(AssertionError)
    }

    // ==================== Net Assertion Helpers ====================

    def 'countTransitionIO should count inputs and outputs correctly'() {
        given:
        def net = parse(linearPuml('action'))
        def t = net.transitions[0]

        when:
        def io = countTransitionIO(net, t)

        then:
        io[0] == 1
        io[1] == 1
    }

    def 'verifyTransitionIO should pass for correct IO counts'() {
        given:
        def net = parse(linearPuml('action'))
        def t = net.transitions[0]

        when:
        verifyTransitionIO(net, t, 1, 1)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionIO should fail for incorrect IO counts'() {
        given:
        def net = parse(linearPuml('action'))
        def t = net.transitions[0]

        when:
        verifyTransitionIO(net, t, 2, 1)

        then:
        thrown(AssertionError)
    }

    def 'findTransitionByLabel should find a transition by its label'() {
        given:
        def net = parse(linearPuml('my action'))

        when:
        def t = findTransitionByLabel(net, 'my action')

        then:
        t != null
        t.label == 'my action'
    }

    def 'findTransitionByLabel should return null for non-existent label'() {
        given:
        def net = parse(linearPuml('my action'))

        expect:
        findTransitionByLabel(net, 'nonexistent') == null
    }

    def 'findTransitionByAction should find a transition by its action key'() {
        given:
        def net = parse(linearPuml('my action'))

        when:
        def t = findTransitionByAction(net, 'my action')

        then:
        t != null
        t.actionKey == 'my action'
    }

    def 'findPlaceByLabel should find a place by its label'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        def p = findPlaceByLabel(net, 'start')

        then:
        p != null
        p.label == 'start'
    }

    def 'verifyLinearChain should pass for correct action sequence'() {
        given:
        def net = parse(linearPuml('step 1', 'step 2', 'step 3'))

        when:
        verifyLinearChain(net, ['step 1', 'step 2', 'step 3'])

        then:
        noExceptionThrown()
    }

    def 'verifyLinearChain should fail for incorrect action sequence'() {
        given:
        def net = parse(linearPuml('step 1', 'step 2'))

        when:
        verifyLinearChain(net, ['step 1', 'wrong'])

        then:
        thrown(AssertionError)
    }

    def 'verifyForkStructure should pass for correct fork structure'() {
        given:
        def net = parse(forkPuml([['a'], ['b']]))

        when:
        verifyForkStructure(net, 2)

        then:
        noExceptionThrown()
    }

    def 'verifyConditionalStructure should pass for correct conditional structure'() {
        given:
        def net = parse(conditionalPuml('myGuard', 'then', 'else'))

        when:
        verifyConditionalStructure(net, 'myGuard')

        then:
        noExceptionThrown()
    }

    def 'validateNetStructure should pass for correct structure'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        validateNetStructure(net, 2, 1)

        then:
        noExceptionThrown()
    }

    def 'validateNetStructure should fail for incorrect place count'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        validateNetStructure(net, 3, 1)

        then:
        thrown(AssertionError)
    }

    def 'validatePlaceLabels should pass for correct labels'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        validatePlaceLabels(net, ['start', 'end'])

        then:
        noExceptionThrown()
    }

    def 'validateTransitionLabels should pass for correct labels'() {
        given:
        def net = parse(linearPuml('my action'))

        when:
        validateTransitionLabels(net, ['my action'])

        then:
        noExceptionThrown()
    }

    def 'validateTransitionAction should pass for correct action'() {
        given:
        def net = parse(linearPuml('my action'))

        when:
        validateTransitionAction(net, 'my action', 0)

        then:
        noExceptionThrown()
    }

    def 'validateTransitionGuard should pass for correct guard'() {
        given:
        def net = parse(conditionalPuml('myGuard', 'then', 'else'))
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }

        when:
        validateTransitionGuard(net, 'myGuard', branchYes.index)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionConnection with Place objects should pass for correct connection'() {
        given:
        def net = parse(linearPuml('action'))
        def t = net.transitions[0]
        def start = findPlaceByLabel(net, 'start')
        def end = findPlaceByLabel(net, 'end')

        when:
        verifyTransitionConnection(net, t, start, end)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionConnection with Place objects should fail for incorrect connection'() {
        given:
        def net = parse(linearPuml('action'))
        def t = net.transitions[0]
        def start = findPlaceByLabel(net, 'start')
        def end = findPlaceByLabel(net, 'end')

        when:
        verifyTransitionConnection(net, t, end, start)

        then:
        thrown(AssertionError)
    }

    def 'verifyTransitionConnection with indices should pass for correct connection'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        verifyTransitionConnection(net, 0, 0, 1)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionConnection with indices should fail for incorrect connection'() {
        given:
        def net = parse(linearPuml('action'))

        when:
        verifyTransitionConnection(net, 0, 1, 0)

        then:
        thrown(AssertionError)
    }
}
