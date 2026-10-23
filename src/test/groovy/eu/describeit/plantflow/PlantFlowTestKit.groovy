package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.DefaultPetriNet
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.PetriNetStructure
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

    // ==================== Component Factories ====================

    /**
     * Creates a Place with the given index and label.
     */
    static Place place(int index, String label) {
        return new Place(index, label)
    }

    /**
     * Creates a Transition with the given parameters.
     */
    static Transition transition(int index, String label, String actionKey = null, String guardKey = null) {
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
     * Creates a Marking with the given number of places.
     */
    static Marking marking(int placeCount) {
        return new Marking(placeCount)
    }

    /**
     * Creates an IncidenceMatrix with the given matrices.
     */
    static IncidenceMatrix incidenceMatrix(int[][] inputMatrix, int[][] outputMatrix) {
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

    // ==================== Context Factory ====================

    /**
     * Creates an ExecutionContext with optional initial values.
     */
    static ExecutionContext context(Map<String, Object> initialValues = null) {
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

    // ==================== Parser Helpers ====================

    /**
     * Parses PlantUML content from a string.
     */
    static PetriNetStructure parse(String pumlContent) {
        return new ActivityDiagramParser().parse(pumlContent)
    }

    /**
     * Parses PlantUML content from a file.
     */
    static PetriNetStructure parse(File file) {
        return new ActivityDiagramParser().parse(file)
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

    // ==================== PlantFlow Execution Helpers ====================

    /**
     * Creates a PlantFlow from a linear PUML and runs it until completion.
     */
    static PlantFlow runLinearFlow(List<String> actions, HandlerRegistry registry, Token token = null, ExecutionContext ctx = null) {
        def engine = PlantFlow.from(linearPuml(actions as String[]), registry)
        engine.runUntilEnd(token, ctx)
        return engine
    }

    /**
     * Creates a PlantFlow from PUML content and runs it until completion.
     */
    static PlantFlow runFlow(String puml, HandlerRegistry registry, Token token = null, ExecutionContext ctx = null) {
        def engine = PlantFlow.from(puml, registry)
        engine.runUntilEnd(token, ctx)
        return engine
    }

    /**
     * Verifies that a workflow has completed with an end token.
     */
    static void verifyCompleted(PlantFlow engine) {
        assert engine.isCompleted() : "Expected workflow to be completed"
        assert engine.getEndToken() != null : "Expected end token to exist"
    }

    /**
     * Verifies the end token payload matches the expected map.
     */
    static void verifyEndTokenPayload(PlantFlow engine, Map<String, Object> expectedPayload) {
        def token = engine.getEndToken()
        assert token != null : "Expected end token"
        assert token.payload == expectedPayload : "Expected payload ${expectedPayload} but got ${token.payload}"
    }

    // ==================== Net Assertion Helpers ====================

    /**
     * Counts the number of input and output places for a transition.
     * Returns [inputCount, outputCount].
     */
    static int[] countTransitionIO(PetriNetStructure net, Transition transition) {
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
    static void verifyTransitionIO(PetriNetStructure net, Transition transition, int expectedInputs, int expectedOutputs) {
        def io = countTransitionIO(net, transition)
        assert io[0] == expectedInputs : "Expected ${expectedInputs} inputs but got ${io[0]} for transition ${transition.label}"
        assert io[1] == expectedOutputs : "Expected ${expectedOutputs} outputs but got ${io[1]} for transition ${transition.label}"
    }

    /**
     * Finds a transition by its label.
     */
    static Transition findTransitionByLabel(PetriNetStructure net, String label) {
        net.transitions.find { it.label == label }
    }

    /**
     * Finds a transition by its action key.
     */
    static Transition findTransitionByAction(PetriNetStructure net, String actionKey) {
        net.transitions.find { it.actionKey == actionKey }
    }

    /**
     * Finds a place by its label.
     */
    static Place findPlaceByLabel(PetriNetStructure net, String label) {
        net.places.find { it.label == label }
    }

    /**
     * Verifies that a net contains a linear chain of actions in order.
     */
    static void verifyLinearChain(PetriNetStructure net, List<String> expectedActions) {
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
    static void verifyForkStructure(PetriNetStructure net, int branchCount) {
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
    static void verifyConditionalStructure(PetriNetStructure net, String guard) {
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }
        assert branchYes != null : "Expected T_branch_yes transition"
        assert branchYes.guardKey == guard : "Expected guard '${guard}' but got '${branchYes.guardKey}'"

        def branchNo = net.transitions.find { it.label == 'T_branch_no' }
        assert branchNo != null : "Expected T_branch_no transition"
        assert branchNo.guardKey == "!(${guard})" : "Expected guard '!(${guard})' but got '${branchNo.guardKey}'"
    }

    /**
     * Validates that a parsed PetriNetStructure has the expected structure.
     */
    static void validateNetStructure(PetriNetStructure net, int expectedPlaceCount, int expectedTransitionCount) {
        assert net != null : "Parsed PetriNetStructure should not be null"
        assert net.places.size() == expectedPlaceCount : "Expected ${expectedPlaceCount} places but got ${net.places.size()}"
        assert net.transitions.size() == expectedTransitionCount : "Expected ${expectedTransitionCount} transitions but got ${net.transitions.size()}"
        assert net.startPlace != null : "Start place should not be null"
        assert net.endPlace != null : "End place should not be null"
    }

    /**
     * Validates that places have the expected labels.
     */
    static void validatePlaceLabels(PetriNetStructure net, List<String> expectedLabels) {
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
    static void validateTransitionLabels(PetriNetStructure net, List<String> expectedLabels) {
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
    static void validateTransitionAction(PetriNetStructure net, String expectedAction, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].actionKey == expectedAction : \
            "Expected action key '${expectedAction}' but got '${net.transitions[transitionIndex].actionKey}'"
    }

    /**
     * Validates that a transition has the expected guard key.
     */
    static void validateTransitionGuard(PetriNetStructure net, String expectedGuard, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].guardKey == expectedGuard : \
            "Expected guard key '${expectedGuard}' but got '${net.transitions[transitionIndex].guardKey}'"
    }

    /**
     * Verifies that a transition consumes from the expected input place and produces to the expected output place.
     */
    static void verifyTransitionConnection(PetriNetStructure net, Transition transition, Place inputPlace, Place outputPlace) {
        assert net.incidenceMatrix.getInputWeight(inputPlace.index, transition.index) == 1 : \
            "Transition ${transition.label} should consume from place ${inputPlace.label}"
        assert net.incidenceMatrix.getOutputWeight(outputPlace.index, transition.index) == 1 : \
            "Transition ${transition.label} should produce to place ${outputPlace.label}"
    }

    /**
     * Verifies incidence matrix connections for a transition by indices.
     */
    static void verifyTransitionConnection(PetriNetStructure net, int transitionIndex, int inputPlaceIndex, int outputPlaceIndex) {
        def t = net.transitions[transitionIndex]
        assert net.incidenceMatrix.getInputWeight(inputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should consume from place ${inputPlaceIndex}"
        assert net.incidenceMatrix.getOutputWeight(outputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should produce to place ${outputPlaceIndex}"
    }
}
