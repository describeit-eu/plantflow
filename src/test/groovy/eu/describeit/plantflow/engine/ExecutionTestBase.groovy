package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition

/**
 * Base utility class for Petri net execution tests.
 * Provides common fixtures and helper methods for testing execution scenarios.
 */
class ExecutionTestBase {

    /**
     * Creates a simple linear Petri net with start -> transition -> end.
     * Optionally registers action handler.
     */
    static DefaultPetriNet createLinearNet(
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
    static DefaultPetriNet createMultiPlaceNet(
        List<String> placeLabels,
        List<String> transitionLabels,
        List<String> actionKeys = null,
        List<String> guardKeys = null,
        int[][] inputMatrix,
        int[][] outputMatrix
    ) {
        def places = placeLabels.collectWithIndex { label, idx -> new Place(idx, label) }
        def transitions = transitionLabels.collectWithIndex { label, idx ->
            new Transition(idx, label, actionKeys ? actionKeys[idx] : null, guardKeys ? guardKeys[idx] : null)
        }

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        return new DefaultPetriNet(places, transitions, incidenceMatrix, places[0], places[placeLabels.size() - 1])
    }

    /**
     * Creates a HandlerRegistry with common action and guard handlers.
     */
    static HandlerRegistry createRegistry(Closure<HandlerRegistry> configurator = null) {
        def registry = new HandlerRegistry()
        if (configurator) {
            configurator.delegate = registry
            configurator.call(registry)
        }
        return registry
    }

    /**
     * Creates a simple HandlerRegistry with a pass-through action handler.
     */
    static HandlerRegistry simpleRegistry(String actionKey = 'action') {
        def registry = new HandlerRegistry()
        registry.registerAction(actionKey) { ExecutionContext ctx, Token tok -> tok }
        return registry
    }

    /**
     * Creates an ExecutionContext with optional initial values.
     */
    static ExecutionContext createContext(Map<String, Object> initialValues = null) {
        return new ExecutionContext(initialValues)
    }

    /**
     * Creates a Marking with the given number of places.
     */
    static Marking createMarking(int placeCount) {
        return new Marking(placeCount)
    }

    /**
     * Creates a Token with the given payload.
     */
    static Token createToken(Map<String, Object> payload = null) {
        return Token.of(payload ?: [:])
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
    static Marking markingWithStartToken(DefaultPetriNet net, Token token = null) {
        def marking = new Marking(net.places.size())
        marking.addToken(net.startPlace, token ?: Token.of())
        return marking
    }
}
