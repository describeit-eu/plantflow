package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.DefaultPetriNet
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition

/**
 * Consolidated test utility for all PlantFlow tests.
 * Provides builders, factories, assertion helpers, and validation methods
 * in a single coherent interface.
 */
class PlantFlowTestKit {

    // ==================== Net Builders ====================

    /**
     * Creates a linear Petri net from the given action names.
     * Equivalent to parsing a linear PlantUML diagram.
     */
    static PetriNet linearNet(String... actions) {
        return parse(linearPuml(actions))
    }

    /**
     * Creates a conditional Petri net from the given guard and branch actions.
     */
    static PetriNet conditionalNet(String guard, List<String> thenActions, List<String> elseActions) {
        return parse(conditionalPuml(guard, thenActions as String[], elseActions as String[]))
    }

    /**
     * Creates a fork Petri net from the given branches and post-merge actions.
     */
    static PetriNet forkNet(List<List<String>> branches, List<String> postMergeActions = []) {
        return parse(forkPuml(branches, postMergeActions))
    }

    /**
     * Creates a simple linear Petri net with start -> transition -> end.
     */
    static DefaultPetriNet simpleLinearNet(
        String startLabel = 'start',
        String endLabel = 'end',
        String transitionLabel = 'action',
        String actionKey = null,
        String guardKey = null
    ) {
        def pStart = new Place(0, startLabel)
        def pEnd = new Place(1, endLabel)
        def t = new Transition(0, transitionLabel, actionKey, guardKey)

        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        return new DefaultPetriNet([pStart, pEnd], [t], incidenceMatrix, pStart, pEnd)
    }

    /**
     * Creates a multi-place Petri net with the given structure.
     */
    static DefaultPetriNet multiPlaceNet(
        List<String> placeLabels,
        List<String> transitionLabels,
        List<String> actionKeys,
        List<String> guardKeys,
        int[][] inputMatrix,
        int[][] outputMatrix
    ) {
        def places = []
        placeLabels.eachWithIndex { label, idx -> places.add(new Place(idx, label)) }
        def transitions = []
        transitionLabels.eachWithIndex { label, idx ->
            transitions.add(new Transition(idx, label, actionKeys ? actionKeys[idx] : null, guardKeys ? guardKeys[idx] : null))
        }

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        return new DefaultPetriNet(places, transitions, incidenceMatrix, places[0], places[placeLabels.size() - 1])
    }

    // ==================== Component Factories ====================

    /**
     * Creates a Place with the given index and label.
     */
    static Place place(int index, String label) {
        return new Place(index, label)
    }

    /**
     * Creates a Place with the given index and label (with defaults).
     */
    static Place createPlace(int index = 0, String label = 'TestPlace') {
        return new Place(index, label)
    }

    /**
     * Creates a Transition with the given parameters.
     */
    static Transition transition(int index, String label, String actionKey = null, String guardKey = null) {
        return new Transition(index, label, actionKey, guardKey)
    }

    /**
     * Creates a Transition with the given parameters (with defaults).
     */
    static Transition createTransition(
        int index = 0,
        String label = 'TestTransition',
        String actionKey = null,
        String guardKey = null
    ) {
        return new Transition(index, label, actionKey, guardKey)
    }

    /**
     * Creates a Token with the given payload.
     */
    static Token token(Map<String, Object> payload = null) {
        return Token.of(payload ?: [:])
    }

    /**
     * Creates a Token with the given id and payload.
     */
    static Token token(String id, Map<String, Object> payload = null) {
        return new Token(id, null, payload ?: [:])
    }

    /**
     * Creates a Token with the given id and payload (legacy createToken signature).
     */
    static Token createToken(String id = null, Map<String, Object> payload = null) {
        if (id) {
            return new Token(id, null, payload ?: [:])
        }
        return Token.of(payload ?: [:])
    }

    /**
     * Creates a Token with the given payload (legacy createToken signature with Map only).
     */
    static Token createToken(Map<String, Object> payload) {
        return Token.of(payload ?: [:])
    }

    /**
     * Creates a Marking with the given number of places.
     */
    static Marking marking(int placeCount) {
        return new Marking(placeCount)
    }

    /**
     * Creates a Marking with the given number of places (with default).
     */
    static Marking createMarking(int placeCount = 1) {
        return new Marking(placeCount)
    }

    /**
     * Creates a Marking and adds a token to the specified place.
     */
    static Marking markingWithToken(int placeCount, Place place, Token token) {
        def marking = new Marking(placeCount)
        marking.addToken(place, token)
        return marking
    }

    /**
     * Creates a Marking and adds a token to the start place of the given net.
     */
    static Marking markingWithStartToken(PetriNet net, Token token = null) {
        def marking = new Marking(net.places.size())
        marking.addToken(net.startPlace, token ?: Token.of())
        return marking
    }

    /**
     * Creates an IncidenceMatrix with the given matrices.
     */
    static IncidenceMatrix incidenceMatrix(int[][] inputMatrix, int[][] outputMatrix) {
        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }

    /**
     * Creates an IncidenceMatrix with the given matrices (with defaults).
     */
    static IncidenceMatrix createIncidenceMatrix(int[][] inputMatrix = null, int[][] outputMatrix = null) {
        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }

    /**
     * Creates a test IncidenceMatrix with a 2x2 structure.
     */
    static IncidenceMatrix create2x2Matrix(
        int p0t0Input = 1, int p0t1Input = 0,
        int p1t0Input = 0, int p1t1Input = 0,
        int p0t0Output = 0, int p0t1Output = 0,
        int p1t0Output = 0, int p1t1Output = 1
    ) {
        def inputMatrix = [
            [p0t0Input, p0t1Input],
            [p1t0Input, p1t1Input]
        ] as int[][]
        def outputMatrix = [
            [p0t0Output, p0t1Output],
            [p1t0Output, p1t1Output]
        ] as int[][]
        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }

    // ==================== Registry Builders ====================

    /**
     * Creates a HandlerRegistry with optional configuration closure.
     */
    static HandlerRegistry registry(Closure<HandlerRegistry> configurator = null) {
        def registry = new HandlerRegistry()
        if (configurator) {
            configurator.delegate = registry
            configurator.call(registry)
        }
        return registry
    }

    /**
     * Creates a HandlerRegistry with optional configuration closure (legacy name).
     */
    static HandlerRegistry createRegistry(Closure<HandlerRegistry> configurator = null) {
        return registry(configurator)
    }

    /**
     * Creates a simple HandlerRegistry with pass-through action handlers.
     */
    static HandlerRegistry passThroughRegistry(String... actionKeys) {
        def registry = new HandlerRegistry()
        actionKeys.each { String key ->
            registry.registerAction(key) { ExecutionContext ctx, Token tok -> tok }
        }
        return registry
    }

    /**
     * Creates a simple HandlerRegistry with a single pass-through action handler.
     */
    static HandlerRegistry simpleRegistry(String actionKey = 'action') {
        return passThroughRegistry(actionKey)
    }

    /**
     * Creates a HandlerRegistry with one action handler per action name.
     * Each handler adds a payload flag keyed by the action name (spaces replaced
     * by underscores) and optionally appends the action name to the execution log.
     */
    static HandlerRegistry payloadRegistry(List<String> actions, List<String> executionLog = null) {
        def registry = new HandlerRegistry()
        actions.each { String action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog?.add(action)
                def key = action.replace(' ', '_')
                return tok.withPayload(tok.payload + [(key): true])
            }
        }
        return registry
    }

    /**
     * Creates a HandlerRegistry with payload actions (legacy name).
     */
    static HandlerRegistry registerPayloadActions(List<String> actions, List<String> executionLog = null) {
        return payloadRegistry(actions, executionLog)
    }

    // ==================== Context Factory ====================

    /**
     * Creates an ExecutionContext with optional initial values.
     */
    static ExecutionContext context(Map<String, Object> initialValues = null) {
        return new ExecutionContext(initialValues)
    }

    /**
     * Creates an ExecutionContext with optional initial values (legacy name).
     */
    static ExecutionContext createContext(Map<String, Object> initialValues = null) {
        return new ExecutionContext(initialValues)
    }

    // ==================== PlantUML Builders ====================

    /**
     * Creates a simple linear diagram PlantUML string with the given actions.
     */
    static String linearPuml(String... actions) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        actions.each { action ->
            sb << ":${action};\n"
        }
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a simple linear diagram PlantUML string (legacy name).
     */
    static String createLinearPuml(String... actions) {
        return linearPuml(actions)
    }

    /**
     * Creates a simple conditional diagram PlantUML string with single actions.
     */
    static String conditionalPuml(String guard, String thenAction, String elseAction) {
        return """@startuml
start
if ( ${guard} ) then (yes)
  :${thenAction};
else (no)
  :${elseAction};
endif
end
@enduml
"""
    }

    /**
     * Creates a conditional diagram PlantUML string with multiple actions in branches.
     */
    static String conditionalPuml(String guard, String[] thenActions, String[] elseActions) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << "if ( ${guard} ) then (yes)\n"
        thenActions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'else (no)\n'
        elseActions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'endif\n'
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a conditional diagram PlantUML string (legacy name, single action version).
     */
    static String createConditionalPuml(String guard, String thenAction, String elseAction) {
        return conditionalPuml(guard, thenAction, elseAction)
    }

    /**
     * Creates a conditional diagram PlantUML string (legacy name, array version).
     */
    static String createConditionalPuml(String guard, String[] thenActions, String[] elseActions) {
        return conditionalPuml(guard, thenActions, elseActions)
    }

    /**
     * Creates a fork diagram PlantUML string with three branches.
     */
    static String forkPuml(List<String> branch1Actions, List<String> branch2Actions, List<String> branch3Actions) {
        return forkPuml([branch1Actions, branch2Actions, branch3Actions], [])
    }

    /**
     * Creates a fork diagram PlantUML string with variable number of branches
     * and optional post-merge actions.
     */
    static String forkPuml(List<List<String>> branches, List<String> postMergeActions = []) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << 'fork\n'

        branches.eachWithIndex { branchActions, index ->
            if (index > 0) {
                sb << 'fork again\n'
            }
            branchActions.each { action ->
                sb << "  :${action};\n"
            }
        }

        sb << 'end merge\n'
        postMergeActions.each { action ->
            sb << ":${action};\n"
        }
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a fork diagram PlantUML string with the given number of branches.
     * Each branch contains a single action with the format "action N".
     */
    static String forkPuml(int branchCount) {
        def branches = []
        (1..branchCount).each { i -> branches.add(["action ${i}"]) }
        return forkPuml(branches, [])
    }

    /**
     * Creates a fork diagram PlantUML string (legacy name, three branches).
     */
    static String createForkPuml(List<String> branch1Actions, List<String> branch2Actions, List<String> branch3Actions) {
        return forkPuml(branch1Actions, branch2Actions, branch3Actions)
    }

    /**
     * Creates a fork diagram PlantUML string (legacy name, variable branches).
     */
    static String createForkPumlWithBranches(List<List<String>> branches, List<String> postMergeActions = []) {
        return forkPuml(branches, postMergeActions)
    }

    /**
     * Creates a fork diagram PlantUML string (legacy name, branch count).
     */
    static String createForkPumlWithBranchCount(int branchCount) {
        return forkPuml(branchCount)
    }

    // ==================== Parser Helpers ====================

    /**
     * Creates a new ActivityDiagramParser instance.
     */
    static ActivityDiagramParser createParser() {
        return new ActivityDiagramParser()
    }

    /**
     * Parses PlantUML content from a string.
     */
    static PetriNet parse(String pumlContent) {
        return createParser().parse(pumlContent)
    }

    /**
     * Parses PlantUML content from a file.
     */
    static PetriNet parse(File file) {
        return createParser().parse(file)
    }

    // ==================== AST Factories ====================

    /**
     * Creates an ActionNode with the given action string.
     */
    static ActionNode action(String action) {
        return new ActionNode(action)
    }

    /**
     * Creates a list of ActionNodes from the given action strings.
     */
    static List<ActionNode> actions(String... actionStrings) {
        return actionStrings.collect { new ActionNode(it) }
    }

    /**
     * Creates a ConditionalNode with the given guard condition and action lists.
     */
    static ConditionalNode conditional(String guardCondition, List<ActionNode> thenActions, List<ActionNode> elseActions) {
        return new ConditionalNode(guardCondition, thenActions, elseActions)
    }

    /**
     * Creates a ConditionalNode with string-based action lists.
     */
    static ConditionalNode conditional(String guardCondition, String[] thenActionStrings, String[] elseActionStrings) {
        return new ConditionalNode(guardCondition, actions(*thenActionStrings), actions(*elseActionStrings))
    }

    /**
     * Creates a ForkNode with the given list of branches.
     */
    static ForkNode fork(List<ActivityNode> branches) {
        return new ForkNode(branches)
    }

    /**
     * Creates a ForkNode with the given activity node branches (varargs).
     */
    static ForkNode fork(ActivityNode... branches) {
        return new ForkNode(Arrays.asList(branches))
    }

    /**
     * Creates a simple ActivityDiagram with the given flags and nodes.
     */
    static ActivityDiagram diagram(boolean hasStart, boolean hasEnd, List<ActivityNode> nodes) {
        return new ActivityDiagram(hasStart, hasEnd, nodes)
    }

    /**
     * Creates an ActivityDiagram using fluent builder pattern.
     */
    static ActivityDiagram diagram(Closure<ActivityDiagram> builder) {
        def diagram = new ActivityDiagram()
        builder.delegate = diagram
        builder.call(diagram)
        return diagram
    }

    // ==================== Marshaller Helpers ====================

    /**
     * Serializes an object to JSON string.
     */
    static String toJson(Object obj, boolean pretty = false) {
        return Marshaller.toJson(obj, pretty)
    }

    /**
     * Deserializes a JSON string to an object of the specified type.
     */
    static <T> T fromJson(String json, Class<T> type) {
        return Marshaller.fromJson(json, type)
    }

    /**
     * Serializes and deserializes an object, verifying round-trip produces non-null result.
     */
    static <T> T roundTrip(T obj, Class<T> type) {
        def json = Marshaller.toJson(obj)
        def deserialized = Marshaller.fromJson(json, type)
        assert deserialized != null : "Deserialization returned null"
        return deserialized
    }

    /**
     * Serializes an object to JSON and verifies it contains the expected substring.
     */
    static String serializeAndVerify(Object obj, String expectedSubstring) {
        def json = Marshaller.toJson(obj)
        assert json.contains(expectedSubstring) : "Expected JSON to contain '$expectedSubstring' but was: $json"
        return json
    }

    /**
     * Verifies that JSON contains all expected fields.
     */
    static void verifyJsonContainsFields(String json, List<String> expectedFields) {
        expectedFields.each { field ->
            assert json.contains(field) : "Expected JSON to contain field '$field' but was: $json"
        }
    }

    /**
     * Verifies that JSON does NOT contain specified fields.
     */
    static void verifyJsonExcludesFields(String json, List<String> excludedFields) {
        excludedFields.each { field ->
            assert !json.contains(field) : "Expected JSON to NOT contain field '$field' but was: $json"
        }
    }

    // ==================== Net Assertion Helpers ====================

    /**
     * Counts the number of input and output places for a transition.
     * Returns [inputCount, outputCount].
     */
    static int[] countTransitionIO(PetriNet net, Transition transition) {
        int inputCount = 0
        int outputCount = 0
        net.places.each { place ->
            if (net.incidenceMatrix.getInputWeight(place.index, transition.index) > 0) inputCount++
            if (net.incidenceMatrix.getOutputWeight(place.index, transition.index) > 0) outputCount++
        }
        [inputCount, outputCount] as int[]
    }

    /**
     * Verifies that a transition has exactly the expected number of inputs and outputs.
     */
    static void verifyTransitionIO(PetriNet net, Transition transition, int expectedInputs, int expectedOutputs) {
        def io = countTransitionIO(net, transition)
        assert io[0] == expectedInputs : "Expected ${expectedInputs} inputs but got ${io[0]} for transition ${transition.label}"
        assert io[1] == expectedOutputs : "Expected ${expectedOutputs} outputs but got ${io[1]} for transition ${transition.label}"
    }

    /**
     * Finds a transition by its label.
     */
    static Transition findTransitionByLabel(PetriNet net, String label) {
        net.transitions.find { it.label == label }
    }

    /**
     * Finds a transition by its action key.
     */
    static Transition findTransitionByAction(PetriNet net, String actionKey) {
        net.transitions.find { it.actionKey == actionKey }
    }

    /**
     * Finds a place by its label.
     */
    static Place findPlaceByLabel(PetriNet net, String label) {
        net.places.find { it.label == label }
    }

    /**
     * Verifies that a net contains a linear chain of actions in order.
     */
    static void verifyLinearChain(PetriNet net, List<String> expectedActions) {
        assert net.transitions.size() == expectedActions.size() : \
            "Expected ${expectedActions.size()} transitions but got ${net.transitions.size()}"
        expectedActions.eachWithIndex { action, idx ->
            assert net.transitions[idx].actionKey == action : \
                "Expected transition ${idx} action '${action}' but got '${net.transitions[idx].actionKey}'"
        }
    }

    /**
     * Verifies fork structure: checks that the net has a fork transition with the expected branch count.
     */
    static void verifyForkStructure(PetriNet net, int branchCount) {
        def forkTransition = net.transitions.find { it.label.startsWith('T_fork') && !it.label.contains('branch') && !it.label.contains('merge') && !it.label.contains('to_end') }
        assert forkTransition != null : "Expected a fork transition in the net"
        verifyTransitionIO(net, forkTransition, 1, branchCount)

        def mergeSyncTransition = net.transitions.find { it.label.contains('merge_sync') }
        assert mergeSyncTransition != null : "Expected a merge sync transition in the net"
        verifyTransitionIO(net, mergeSyncTransition, branchCount, 1)
    }

    /**
     * Verifies conditional structure: checks that the net has the expected guard on branch transitions.
     */
    static void verifyConditionalStructure(PetriNet net, String guard) {
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }
        assert branchYes != null : "Expected T_branch_yes transition"
        assert branchYes.guardKey == guard : "Expected guard '${guard}' but got '${branchYes.guardKey}'"

        def branchNo = net.transitions.find { it.label == 'T_branch_no' }
        assert branchNo != null : "Expected T_branch_no transition"
        assert branchNo.guardKey == "!(${guard})" : "Expected guard '!(${guard})' but got '${branchNo.guardKey}'"
    }

    /**
     * Validates that a parsed PetriNet has the expected structure.
     */
    static void validateNetStructure(PetriNet net, int expectedPlaceCount, int expectedTransitionCount) {
        assert net != null : "Parsed PetriNet should not be null"
        assert net.places.size() == expectedPlaceCount : "Expected ${expectedPlaceCount} places but got ${net.places.size()}"
        assert net.transitions.size() == expectedTransitionCount : "Expected ${expectedTransitionCount} transitions but got ${net.transitions.size()}"
        assert net.startPlace != null : "Start place should not be null"
        assert net.endPlace != null : "End place should not be null"
    }

    /**
     * Validates that places have the expected labels.
     */
    static void validatePlaceLabels(PetriNet net, List<String> expectedLabels) {
        assert net.places.size() == expectedLabels.size() : \
            "Expected ${expectedLabels.size()} places but got ${net.places.size()}"
        expectedLabels.eachWithIndex { label, idx ->
            assert net.places[idx].label == label : \
                "Expected place ${idx} label '${label}' but got '${net.places[idx].label}'"
        }
    }

    /**
     * Validates that transitions have the expected labels.
     */
    static void validateTransitionLabels(PetriNet net, List<String> expectedLabels) {
        assert net.transitions.size() == expectedLabels.size() : \
            "Expected ${expectedLabels.size()} transitions but got ${net.transitions.size()}"
        expectedLabels.eachWithIndex { label, idx ->
            assert net.transitions[idx].label == label : \
                "Expected transition ${idx} label '${label}' but got '${net.transitions[idx].label}'"
        }
    }

    /**
     * Validates that a transition has the expected action key.
     */
    static void validateTransitionAction(PetriNet net, String expectedAction, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].actionKey == expectedAction : \
            "Expected action key '${expectedAction}' but got '${net.transitions[transitionIndex].actionKey}'"
    }

    /**
     * Validates that a transition has the expected guard key.
     */
    static void validateTransitionGuard(PetriNet net, String expectedGuard, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].guardKey == expectedGuard : \
            "Expected guard key '${expectedGuard}' but got '${net.transitions[transitionIndex].guardKey}'"
    }

    /**
     * Verifies incidence matrix connections for a transition.
     */
    static void verifyTransitionConnection(PetriNet net, int transitionIndex, int inputPlaceIndex, int outputPlaceIndex) {
        def t = net.transitions[transitionIndex]
        assert net.incidenceMatrix.getInputWeight(inputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should consume from place ${inputPlaceIndex}"
        assert net.incidenceMatrix.getOutputWeight(outputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should produce to place ${outputPlaceIndex}"
    }

    // ==================== Component Assertions ====================

    /**
     * Validates that a component has the expected index.
     */
    static void assertIndex(Object component, int expectedIndex) {
        assert component.index == expectedIndex : "Expected index ${expectedIndex} but got ${component.index}"
    }

    /**
     * Validates that a component has the expected label.
     */
    static void assertLabel(Object component, String expectedLabel) {
        assert component.label == expectedLabel : "Expected label '${expectedLabel}' but got '${component.label}'"
    }

    /**
     * Validates that two components are equal.
     */
    static void assertEqualComponents(Object component1, Object component2) {
        assert component1 == component2 : "Expected components to be equal"
        assert component1.hashCode() == component2.hashCode() : "Expected hashCodes to be equal"
    }

    /**
     * Validates that two components are not equal.
     */
    static void assertNotEqualComponents(Object component1, Object component2) {
        assert component1 != component2 : "Expected components to be not equal"
    }

    /**
     * Validates that toString contains the expected substring.
     */
    static void assertToStringContains(Object component, String expectedSubstring) {
        assert component.toString().contains(expectedSubstring) : \
            "Expected toString to contain '${expectedSubstring}' but was: ${component.toString()}"
    }

    /**
     * Validates that creating a component with null label throws IllegalArgumentException.
     */
    static void assertNullLabelThrows(Closure<Object> factory) {
        try {
            factory.call()
            assert false : "Expected IllegalArgumentException for null label"
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Validates that creating a component with blank label throws IllegalArgumentException.
     */
    static void assertBlankLabelThrows(Closure<Object> factory, String blankLabel) {
        try {
            factory.call(blankLabel)
            assert false : "Expected IllegalArgumentException for blank label '${blankLabel}'"
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Validates that creating a component with null index throws IllegalArgumentException.
     */
    static void assertNullIndexThrows(Closure<Object> factory) {
        try {
            factory.call()
            assert false : "Expected IllegalArgumentException for null index"
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // ==================== Standard Fixtures ====================

    /**
     * Creates a standard set of test places.
     */
    static List<Place> createStandardPlaces() {
        return [
            new Place(0, 'start'),
            new Place(1, 'mid'),
            new Place(2, 'end')
        ]
    }

    /**
     * Creates a standard set of test transitions.
     */
    static List<Transition> createStandardTransitions() {
        return [
            new Transition(0, 't1', 'action1', null),
            new Transition(1, 't2', 'action2', null),
            new Transition(2, 't3', 'action3', null)
        ]
    }
}
