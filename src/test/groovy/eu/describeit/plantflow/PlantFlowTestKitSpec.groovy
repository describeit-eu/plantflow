package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.DefaultPetriNet
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Meta-tests for PlantFlowTestKit: verifies that all builders, factories,
 * and assertion helpers work correctly.
 */
class PlantFlowTestKitSpec extends Specification {

    // ==================== Net Builders ====================

    def 'linearNet should create a net with correct structure for single action'() {
        when:
        def net = linearNet('process order')

        then:
        net != null
        net.places.size() == 2
        net.transitions.size() == 1
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        net.transitions[0].actionKey == 'process order'
    }

    def 'linearNet should create a net with correct structure for multiple actions'() {
        when:
        def net = linearNet('step 1', 'step 2', 'step 3')

        then:
        net.places.size() == 4
        net.transitions.size() == 3
        verifyLinearChain(net, ['step 1', 'step 2', 'step 3'])
    }

    def 'conditionalNet should create a net with correct conditional structure'() {
        when:
        def net = conditionalNet('isValid', ['then action'], ['else action'])

        then:
        net != null
        net.places.size() == 6
        net.transitions.size() == 6
        verifyConditionalStructure(net, 'isValid')
    }

    def 'forkNet should create a net with correct fork structure for two branches'() {
        when:
        def net = forkNet([['branch a'], ['branch b']], ['after merge'])

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        verifyForkStructure(net, 2)
    }

    def 'forkNet should create a net with correct fork structure for three branches'() {
        when:
        def net = forkNet([['a1'], ['b1'], ['c1']])

        then:
        net != null
        verifyForkStructure(net, 3)
    }

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

    def 'multiPlaceNet should create a net with the given structure'() {
        given:
        def inputMatrix = [[1, 0], [0, 1], [0, 0]] as int[][]
        def outputMatrix = [[0, 0], [1, 0], [0, 1]] as int[][]

        when:
        def net = multiPlaceNet(['start', 'mid', 'end'], ['t1', 't2'], ['a1', 'a2'], (List<String>) null, inputMatrix, outputMatrix)

        then:
        net.places.size() == 3
        net.transitions.size() == 2
        net.places[0].label == 'start'
        net.places[1].label == 'mid'
        net.places[2].label == 'end'
        net.transitions[0].actionKey == 'a1'
        net.transitions[1].actionKey == 'a2'
    }

    // ==================== Component Factories ====================

    def 'place should create a Place with correct index and label'() {
        when:
        def p = place(5, 'myPlace')

        then:
        p.index == 5
        p.label == 'myPlace'
    }

    def 'createPlace should create a Place with defaults'() {
        when:
        def p = createPlace()

        then:
        p.index == 0
        p.label == 'TestPlace'
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

    def 'createTransition should create a Transition with defaults'() {
        when:
        def t = createTransition()

        then:
        t.index == 0
        t.label == 'TestTransition'
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

    def 'createToken with id should create a Token with the given id'() {
        when:
        def t = createToken('token-1', [key1: 'value1'])

        then:
        t.id == 'token-1'
        t.payload == [key1: 'value1']
    }

    def 'createToken without id should create a Token via Token.of'() {
        when:
        def t = createToken(null, [key: 'value'])

        then:
        t.id != null
        t.payload == [key: 'value']
    }

    def 'marking should create a Marking with the given place count'() {
        when:
        def m = marking(5)

        then:
        m != null
        m.tokens.length == 5
    }

    def 'createMarking should create a Marking with default place count'() {
        when:
        def m = createMarking()

        then:
        m.tokens.length == 1
    }

    def 'markingWithToken should create a Marking with a token in the specified place'() {
        given:
        def p = place(1, 'target')
        def t = token([data: 'test'])

        when:
        def m = markingWithToken(3, p, t)

        then:
        m.getTokenCount(p) == 1
        m.getTokens(p) == [t]
    }

    def 'markingWithStartToken should create a Marking with a token in the start place'() {
        given:
        def net = linearNet('action')

        when:
        def m = markingWithStartToken(net)

        then:
        m.getTokenCount(net.startPlace) == 1
    }

    def 'markingWithStartToken should use the provided token'() {
        given:
        def net = linearNet('action')
        def customToken = token([custom: true])

        when:
        def m = markingWithStartToken(net, customToken)

        then:
        m.getTokens(net.startPlace) == [customToken]
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

    def 'createIncidenceMatrix should create an IncidenceMatrix with null matrices'() {
        when:
        def matrix = createIncidenceMatrix(null, null)

        then:
        matrix.getInputWeight(0, 0) == 0
        matrix.getOutputWeight(0, 0) == 0
    }

    def 'create2x2Matrix should create a 2x2 IncidenceMatrix with defaults'() {
        when:
        def matrix = create2x2Matrix()

        then:
        matrix.getInputWeight(0, 0) == 1
        matrix.getInputWeight(1, 1) == 0
        matrix.getOutputWeight(1, 1) == 1
    }

    def 'create2x2Matrix should create a 2x2 IncidenceMatrix with custom values'() {
        when:
        def matrix = create2x2Matrix(2, 0, 0, 3, 0, 0, 4, 0)

        then:
        matrix.getInputWeight(0, 0) == 2
        matrix.getInputWeight(1, 1) == 3
        matrix.getOutputWeight(1, 0) == 4
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

    def 'createRegistry should be an alias for registry'() {
        when:
        def reg = createRegistry { r ->
            r.registerAction('test') { ctx, tok -> tok }
        }

        then:
        reg.getAction('test') != null
    }

    def 'passThroughRegistry should create a registry with pass-through handlers'() {
        when:
        def reg = passThroughRegistry('action1', 'action2')
        def ctx = context()
        def tok = token([data: 'test'])

        then:
        reg.getAction('action1').execute(ctx, tok) == tok
        reg.getAction('action2').execute(ctx, tok) == tok
    }

    def 'simpleRegistry should create a registry with a single pass-through handler'() {
        when:
        def reg = simpleRegistry('myAction')
        def ctx = context()
        def tok = token()

        then:
        reg.getAction('myAction').execute(ctx, tok) == tok
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

    def 'registerPayloadActions should be an alias for payloadRegistry'() {
        given:
        def reg = registerPayloadActions(['test action'])

        when:
        def result = reg.getAction('test action').execute(context(), token())

        then:
        result.payload.test_action == true
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

    def 'createContext should be an alias for context'() {
        when:
        def ctx = createContext([test: 'value'])

        then:
        ctx['test'] == 'value'
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

    def 'createLinearPuml should be an alias for linearPuml'() {
        expect:
        createLinearPuml('action') == linearPuml('action')
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

    def 'createForkPumlWithBranches should be an alias for forkPuml'() {
        expect:
        createForkPumlWithBranches([['a'], ['b']]) == forkPuml([['a'], ['b']], [])
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

    def 'createParser should create a new ActivityDiagramParser'() {
        expect:
        createParser() != null
        createParser() instanceof ActivityDiagramParser
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

    // ==================== Marshaller Helpers ====================

    def 'toJson should serialize an object to JSON'() {
        given:
        def p = place(0, 'Start')

        when:
        def json = toJson(p)

        then:
        json.contains('"index"')
        json.contains('"label"')
        json.contains('"Start"')
    }

    def 'toJson with pretty should produce pretty-printed JSON'() {
        given:
        def p = place(0, 'Start')

        when:
        def json = toJson(p, true)

        then:
        json.contains('\n')
    }

    def 'fromJson should deserialize a JSON string'() {
        given:
        def json = '{"index":5,"label":"TestPlace"}'

        when:
        def p = fromJson(json, Place)

        then:
        p.index == 5
        p.label == 'TestPlace'
    }

    def 'roundTrip should serialize and deserialize an object'() {
        given:
        def p = place(3, 'RoundTrip')

        when:
        def result = roundTrip(p, Place)

        then:
        result != null
        result.index == 3
        result.label == 'RoundTrip'
    }

    def 'serializeAndVerify should serialize and verify substring'() {
        given:
        def p = place(0, 'VerifyMe')

        when:
        def json = serializeAndVerify(p, 'VerifyMe')

        then:
        json.contains('VerifyMe')
    }

    def 'serializeAndVerify should fail when substring is missing'() {
        given:
        def p = place(0, 'TestPlace')

        when:
        serializeAndVerify(p, 'NonExistentField')

        then:
        thrown(AssertionError)
    }

    def 'verifyJsonContainsFields should pass when all fields are present'() {
        given:
        def json = '{"field1":"value1","field2":"value2"}'

        when:
        verifyJsonContainsFields(json, ['field1', 'field2'])

        then:
        noExceptionThrown()
    }

    def 'verifyJsonContainsFields should fail when a field is missing'() {
        given:
        def json = '{"field1":"value1"}'

        when:
        verifyJsonContainsFields(json, ['field1', 'field2'])

        then:
        thrown(AssertionError)
    }

    def 'verifyJsonExcludesFields should pass when no excluded fields are present'() {
        given:
        def json = '{"field1":"value1"}'

        when:
        verifyJsonExcludesFields(json, ['field2', 'field3'])

        then:
        noExceptionThrown()
    }

    def 'verifyJsonExcludesFields should fail when an excluded field is present'() {
        given:
        def json = '{"field1":"value1","field2":"value2"}'

        when:
        verifyJsonExcludesFields(json, ['field2'])

        then:
        thrown(AssertionError)
    }

    // ==================== Net Assertion Helpers ====================

    def 'countTransitionIO should count inputs and outputs correctly'() {
        given:
        def net = linearNet('action')
        def t = net.transitions[0]

        when:
        def io = countTransitionIO(net, t)

        then:
        io[0] == 1  // 1 input (start place)
        io[1] == 1  // 1 output (end place)
    }

    def 'verifyTransitionIO should pass for correct IO counts'() {
        given:
        def net = linearNet('action')
        def t = net.transitions[0]

        when:
        verifyTransitionIO(net, t, 1, 1)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionIO should fail for incorrect IO counts'() {
        given:
        def net = linearNet('action')
        def t = net.transitions[0]

        when:
        verifyTransitionIO(net, t, 2, 1)

        then:
        thrown(AssertionError)
    }

    def 'findTransitionByLabel should find a transition by its label'() {
        given:
        def net = linearNet('my action')

        when:
        def t = findTransitionByLabel(net, 'my action')

        then:
        t != null
        t.label == 'my action'
    }

    def 'findTransitionByLabel should return null for non-existent label'() {
        given:
        def net = linearNet('my action')

        expect:
        findTransitionByLabel(net, 'nonexistent') == null
    }

    def 'findTransitionByAction should find a transition by its action key'() {
        given:
        def net = linearNet('my action')

        when:
        def t = findTransitionByAction(net, 'my action')

        then:
        t != null
        t.actionKey == 'my action'
    }

    def 'findPlaceByLabel should find a place by its label'() {
        given:
        def net = linearNet('action')

        when:
        def p = findPlaceByLabel(net, 'start')

        then:
        p != null
        p.label == 'start'
    }

    def 'verifyLinearChain should pass for correct action sequence'() {
        given:
        def net = linearNet('step 1', 'step 2', 'step 3')

        when:
        verifyLinearChain(net, ['step 1', 'step 2', 'step 3'])

        then:
        noExceptionThrown()
    }

    def 'verifyLinearChain should fail for incorrect action sequence'() {
        given:
        def net = linearNet('step 1', 'step 2')

        when:
        verifyLinearChain(net, ['step 1', 'wrong'])

        then:
        thrown(AssertionError)
    }

    def 'verifyForkStructure should pass for correct fork structure'() {
        given:
        def net = forkNet([['a'], ['b']])

        when:
        verifyForkStructure(net, 2)

        then:
        noExceptionThrown()
    }

    def 'verifyConditionalStructure should pass for correct conditional structure'() {
        given:
        def net = conditionalNet('myGuard', ['then'], ['else'])

        when:
        verifyConditionalStructure(net, 'myGuard')

        then:
        noExceptionThrown()
    }

    def 'validateNetStructure should pass for correct structure'() {
        given:
        def net = linearNet('action')

        when:
        validateNetStructure(net, 2, 1)

        then:
        noExceptionThrown()
    }

    def 'validateNetStructure should fail for incorrect place count'() {
        given:
        def net = linearNet('action')

        when:
        validateNetStructure(net, 3, 1)

        then:
        thrown(AssertionError)
    }

    def 'validatePlaceLabels should pass for correct labels'() {
        given:
        def net = linearNet('action')

        when:
        validatePlaceLabels(net, ['start', 'end'])

        then:
        noExceptionThrown()
    }

    def 'validateTransitionLabels should pass for correct labels'() {
        given:
        def net = linearNet('my action')

        when:
        validateTransitionLabels(net, ['my action'])

        then:
        noExceptionThrown()
    }

    def 'validateTransitionAction should pass for correct action'() {
        given:
        def net = linearNet('my action')

        when:
        validateTransitionAction(net, 'my action', 0)

        then:
        noExceptionThrown()
    }

    def 'validateTransitionGuard should pass for correct guard'() {
        given:
        def net = conditionalNet('myGuard', ['then'], ['else'])
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }

        when:
        validateTransitionGuard(net, 'myGuard', branchYes.index)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionConnection should pass for correct connection'() {
        given:
        def net = linearNet('action')

        when:
        verifyTransitionConnection(net, 0, 0, 1)

        then:
        noExceptionThrown()
    }

    def 'verifyTransitionConnection should fail for incorrect connection'() {
        given:
        def net = linearNet('action')

        when:
        verifyTransitionConnection(net, 0, 1, 0)

        then:
        thrown(AssertionError)
    }

    // ==================== Component Assertions ====================

    def 'assertIndex should pass for correct index'() {
        given:
        def p = place(5, 'test')

        when:
        assertIndex(p, 5)

        then:
        noExceptionThrown()
    }

    def 'assertIndex should fail for incorrect index'() {
        given:
        def p = place(5, 'test')

        when:
        assertIndex(p, 3)

        then:
        thrown(AssertionError)
    }

    def 'assertLabel should pass for correct label'() {
        given:
        def p = place(0, 'myLabel')

        when:
        assertLabel(p, 'myLabel')

        then:
        noExceptionThrown()
    }

    def 'assertLabel should fail for incorrect label'() {
        given:
        def p = place(0, 'myLabel')

        when:
        assertLabel(p, 'wrong')

        then:
        thrown(AssertionError)
    }

    def 'assertEqualComponents should pass for equal components'() {
        given:
        def p1 = place(0, 'same')
        def p2 = place(0, 'same')

        when:
        assertEqualComponents(p1, p2)

        then:
        noExceptionThrown()
    }

    def 'assertEqualComponents should fail for unequal components'() {
        given:
        def p1 = place(0, 'one')
        def p2 = place(1, 'two')

        when:
        assertEqualComponents(p1, p2)

        then:
        thrown(AssertionError)
    }

    def 'assertNotEqualComponents should pass for unequal components'() {
        given:
        def p1 = place(0, 'one')
        def p2 = place(1, 'two')

        when:
        assertNotEqualComponents(p1, p2)

        then:
        noExceptionThrown()
    }

    def 'assertNotEqualComponents should fail for equal components'() {
        given:
        def p1 = place(0, 'same')
        def p2 = place(0, 'same')

        when:
        assertNotEqualComponents(p1, p2)

        then:
        thrown(AssertionError)
    }

    def 'assertToStringContains should pass when toString contains substring'() {
        given:
        def p = place(0, 'myPlace')

        when:
        assertToStringContains(p, 'myPlace')

        then:
        noExceptionThrown()
    }

    def 'assertToStringContains should fail when toString does not contain substring'() {
        given:
        def p = place(0, 'myPlace')

        when:
        assertToStringContains(p, 'nonexistent')

        then:
        thrown(AssertionError)
    }

    def 'assertNullLabelThrows should pass when factory throws IllegalArgumentException for null'() {
        when:
        assertNullLabelThrows { -> new Place(0, null) }

        then:
        noExceptionThrown()
    }

    def 'assertNullLabelThrows should fail when factory does not throw'() {
        when:
        assertNullLabelThrows { -> new Place(0, 'valid') }

        then:
        thrown(AssertionError)
    }

    def 'assertBlankLabelThrows should pass when factory throws for blank label'() {
        when:
        assertBlankLabelThrows({ String label -> new Place(0, label) }, '   ')

        then:
        noExceptionThrown()
    }

    def 'assertBlankLabelThrows should fail when factory does not throw'() {
        when:
        assertBlankLabelThrows({ String label -> new Place(0, label) }, 'valid')

        then:
        thrown(AssertionError)
    }

    def 'assertNullIndexThrows should pass when factory throws for null index'() {
        when:
        assertNullIndexThrows { -> new Transition(null, 'label') }

        then:
        noExceptionThrown()
    }

    def 'assertNullIndexThrows should fail when factory does not throw'() {
        when:
        assertNullIndexThrows { -> new Transition(0, 'label') }

        then:
        thrown(AssertionError)
    }

    // ==================== Standard Fixtures ====================

    def 'createStandardPlaces should create three standard places'() {
        when:
        def places = createStandardPlaces()

        then:
        places.size() == 3
        places[0].label == 'start'
        places[1].label == 'mid'
        places[2].label == 'end'
    }

    def 'createStandardTransitions should create three standard transitions'() {
        when:
        def transitions = createStandardTransitions()

        then:
        transitions.size() == 3
        transitions[0].label == 't1'
        transitions[0].actionKey == 'action1'
        transitions[1].label == 't2'
        transitions[1].actionKey == 'action2'
        transitions[2].label == 't3'
        transitions[2].actionKey == 'action3'
    }
}
