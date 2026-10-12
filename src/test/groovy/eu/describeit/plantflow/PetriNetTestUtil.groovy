package eu.describeit.plantflow

import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Transition

/**
 * Shared test utilities for PetriNet assertions.
 */
class PetriNetTestUtil {

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
    static def findPlaceByLabel(PetriNet net, String label) {
        net.places.find { it.label == label }
    }
}
