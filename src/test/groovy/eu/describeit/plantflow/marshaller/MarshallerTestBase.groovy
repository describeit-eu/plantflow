package eu.describeit.plantflow.marshaller

import eu.describeit.plantflow.Marshaller
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition

/**
 * Base utility class for marshaller tests.
 * Provides common fixtures and helper methods for testing JSON serialization/deserialization.
 */
class MarshallerTestBase {

    /**
     * Creates a simple Place for marshalling tests.
     */
    static Place createPlace(int index = 0, String label = 'TestPlace') {
        return new Place(index, label)
    }

    /**
     * Creates a simple Transition for marshalling tests.
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
     * Creates a simple IncidenceMatrix for marshalling tests.
     */
    static IncidenceMatrix createIncidenceMatrix(
        int[][] inputMatrix = null,
        int[][] outputMatrix = null
    ) {
        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }

    /**
     * Creates a simple Marking for marshalling tests.
     */
    static Marking createMarking(int placeCount = 1) {
        return new Marking(placeCount)
    }

    /**
     * Creates a Token for marshalling tests.
     */
    static Token createToken(String id = null, Map<String, Object> payload = null) {
        if (id) {
            return new Token(id, null, payload ?: [:])
        }
        return Token.of(payload ?: [:])
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
     * Serializes and deserializes an object, verifying round-trip equality.
     */
    static <T> T roundTrip(T obj, Class<T> type) {
        def json = Marshaller.toJson(obj)
        def deserialized = Marshaller.fromJson(json, type)
        assert deserialized != null : "Deserialization returned null"
        return deserialized
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
}
