package eu.describeit.plantflow.engine

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic

@CompileStatic
class IncidenceMatrix {
    final List<Place> places
    final List<Transition> transitions
    final int[][] inputMatrix     // [numPlaces][numTransitions]
    final int[][] outputMatrix    // [numPlaces][numTransitions]
    final int[][] incidenceMatrix // [numPlaces][numTransitions]

    @JsonCreator
    IncidenceMatrix(
        @JsonProperty('places') List<Place> places,
        @JsonProperty('transitions') List<Transition> transitions,
        @JsonProperty('inputMatrix') int[][] inMatrix,
        @JsonProperty('outputMatrix') int[][] outMatrix
    ) {
        Closure<Integer> getWeight = { int[][] matrix, int p, int t ->
            return (matrix != null && p < matrix.length && t < matrix[p].length) ? matrix[p][t] : 0
        }

        this.places = places.asUnmodifiable()
        this.transitions = transitions.asUnmodifiable()

        int numPlaces = places.size()
        int numTransitions = transitions.size()

        this.inputMatrix     = new int[numPlaces][numTransitions]
        this.outputMatrix    = new int[numPlaces][numTransitions]
        this.incidenceMatrix = new int[numPlaces][numTransitions]

        for (int p = 0; p < numPlaces; p++) {
            for (int t = 0; t < numTransitions; t++) {
                int inWeight  = getWeight(inMatrix, p, t)
                int outWeight = getWeight(outMatrix, p, t)

                this.inputMatrix[p][t] = inWeight
                this.outputMatrix[p][t] = outWeight
                this.incidenceMatrix[p][t] = outWeight - inWeight
            }
        }
    }

    int getInputWeight(int placeIndex, int transitionIndex) {
        return inputMatrix[placeIndex][transitionIndex]
    }

    int getOutputWeight(int placeIndex, int transitionIndex) {
        return outputMatrix[placeIndex][transitionIndex]
    }

    int getIncidence(int placeIndex, int transitionIndex) {
        return incidenceMatrix[placeIndex][transitionIndex]
    }

    List<Place> getInputPlaces(int transitionIndex) {
        return getPlaces(inputMatrix, transitionIndex)
    }

    List<Place> getOutputPlaces(int transitionIndex) {
        return getPlaces(outputMatrix, transitionIndex)
    }
    
    private List<Place> getPlaces(int[][] inputMatrix, int transitionIndex) {
        List<Place> result = []
        for (int p = 0; p < places.size(); p++) {
            if (inputMatrix[p][transitionIndex] > 0) {
                result.add(places[p])
            }
        }
        return Collections.unmodifiableList(result)
    }
}
