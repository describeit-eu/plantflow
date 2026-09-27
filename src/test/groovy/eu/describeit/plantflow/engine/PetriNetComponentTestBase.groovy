package eu.describeit.plantflow.engine

/**
 * Base utility class for Petri net component tests (Place, Transition, Token, Marking, etc.).
 * Provides common test fixtures and validation methods.
 */
class PetriNetComponentTestBase {

    /**
     * Creates a Place with the given index and label.
     */
    Place createPlace(int index, String label) {
        return new Place(index, label)
    }

    /**
     * Creates a Transition with the given parameters.
     */
    Transition createTransition(int index, String label, String actionKey = null, String guardKey = null) {
        return new Transition(index, label, actionKey, guardKey)
    }

    /**
     * Creates a Token with the given parameters.
     */
    Token createToken(String id = null, Map<String, Object> payload = null) {
        if (id) {
            return new Token(id, null, payload ?: [:])
        }
        return Token.of(payload ?: [:])
    }

    /**
     * Creates a Marking with the given number of places.
     */
    Marking createMarking(int placeCount) {
        return new Marking(placeCount)
    }

    /**
     * Creates an IncidenceMatrix with the given matrices.
     */
    IncidenceMatrix createIncidenceMatrix(int[][] inputMatrix, int[][] outputMatrix) {
        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }

    /**
     * Validates that a component has the expected index.
     */
    void assertIndex(Object component, int expectedIndex) {
        expect:
        component.index == expectedIndex
    }

    /**
     * Validates that a component has the expected label.
     */
    void assertLabel(Object component, String expectedLabel) {
        expect:
        component.label == expectedLabel
    }

    /**
     * Validates that two components are equal.
     */
    void assertEqualComponents(Object component1, Object component2) {
        expect:
        component1 == component2
        component1.hashCode() == component2.hashCode()
    }

    /**
     * Validates that two components are not equal.
     */
    void assertNotEqualComponents(Object component1, Object component2) {
        expect:
        component1 != component2
    }

    /**
     * Validates that toString contains the expected substring.
     */
    void assertToStringContains(Object component, String expectedSubstring) {
        expect:
        component.toString().contains(expectedSubstring)
    }

    /**
     * Validates that creating a component with null label throws IllegalArgumentException.
     */
    void assertNullLabelThrows(Closure<Object> factory) {
        when:
        factory.call()

        then:
        thrown(IllegalArgumentException)
    }

    /**
     * Validates that creating a component with blank label throws IllegalArgumentException.
     */
    void assertBlankLabelThrows(Closure<Object> factory, String blankLabel) {
        when:
        factory.call(blankLabel)

        then:
        thrown(IllegalArgumentException)
    }

    /**
     * Validates that creating a component with null index throws IllegalArgumentException.
     */
    void assertNullIndexThrows(Closure<Object> factory) {
        when:
        factory.call()

        then:
        thrown(IllegalArgumentException)
    }

    /**
     * Creates a standard set of test places.
     */
    List<Place> createStandardPlaces() {
        return [
            new Place(0, 'start'),
            new Place(1, 'mid'),
            new Place(2, 'end')
        ]
    }

    /**
     * Creates a standard set of test transitions.
     */
    List<Transition> createStandardTransitions() {
        return [
            new Transition(0, 't1', 'action1', null),
            new Transition(1, 't2', 'action2', null),
            new Transition(2, 't3', 'action3', null)
        ]
    }
}
