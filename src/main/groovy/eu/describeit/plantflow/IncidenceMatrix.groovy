package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
class IncidenceMatrix {
    final List<Place> places
    final List<Transition> transitions
    final int[][] inputMatrix
    final int[][] outputMatrix
    final int[][] incidenceMatrix

    IncidenceMatrix(List<Place> places, List<Transition> transitions, int[][] inputMatrix, int[][] outputMatrix) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))

        int numPlaces = places.size()
        int numTransitions = transitions.size()

        this.inputMatrix = new int[numPlaces][numTransitions]
        this.outputMatrix = new int[numPlaces][numTransitions]
        this.incidenceMatrix = new int[numPlaces][numTransitions]

        for (int p = 0; p < numPlaces; p++) {
            for (int t = 0; t < numTransitions; t++) {
                int inWeight = (inputMatrix != null && p < inputMatrix.length && t < inputMatrix[p].length) ? inputMatrix[p][t] : 0
                int outWeight = (outputMatrix != null && p < outputMatrix.length && t < outputMatrix[p].length) ? outputMatrix[p][t] : 0
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
