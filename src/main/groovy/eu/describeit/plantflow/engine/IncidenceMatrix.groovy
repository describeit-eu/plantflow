package eu.describeit.plantflow.engine

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class IncidenceMatrix {
    final int[][] inputMatrix     // [numPlaces][numTransitions]
    final int[][] outputMatrix    // [numPlaces][numTransitions]
    final int[][] incidenceMatrix // [numPlaces][numTransitions]

    @JsonCreator
    IncidenceMatrix(
        @JsonProperty('inputMatrix') int[][] inMatrix,
        @JsonProperty('outputMatrix') int[][] outMatrix
    ) {
        int numPlaces = Math.max(inMatrix ? inMatrix.length : 0, outMatrix ? outMatrix.length : 0)

        int numTransitions = Math.max(
            getNumberOfTransitions(inMatrix), 
            getNumberOfTransitions(outMatrix)
        )

        this.inputMatrix     = new int[numPlaces][numTransitions]
        this.outputMatrix    = new int[numPlaces][numTransitions]
        this.incidenceMatrix = new int[numPlaces][numTransitions]

        for (int pIdx = 0; pIdx < numPlaces; pIdx++) {
            for (int tIdx = 0; tIdx < numTransitions; tIdx++) {
                int inWeight  = getMatrixValue(inMatrix, pIdx, tIdx)
                int outWeight = getMatrixValue(outMatrix, pIdx, tIdx)

                this.inputMatrix[pIdx][tIdx] = inWeight
                this.outputMatrix[pIdx][tIdx] = outWeight
                this.incidenceMatrix[pIdx][tIdx] = outWeight - inWeight
            }
        }
    }

    int getInputWeight(int pIdx, int tIdx) {
        return getMatrixValue(inputMatrix, pIdx, tIdx)
    }

    int getOutputWeight(int pIdx, int tIdx) {
        return getMatrixValue(outputMatrix, pIdx, tIdx)
    }

    int getIncidence(int pIdx, int tIdx) {
        return getMatrixValue(incidenceMatrix, pIdx, tIdx)
    }

    private int getNumberOfTransitions(int[][] matrix) {
        int numTransitions = 0
        if (matrix) {
            for (int[] row : matrix) {
                if (row?.length > numTransitions) {
                    numTransitions = row.length
                }
            }
        }
        return numTransitions
    }

    private int getMatrixValue(int[][] matrix, int pIdx, int tIdx) {
        assert pIdx >= 0 && tIdx >= 0

        log.debug('getMatrixValue() - matrix:{} pIdx:{} tIdx:{}', matrix, pIdx, tIdx)
        return (matrix && pIdx < matrix.length && tIdx < matrix[pIdx].length) ? matrix[pIdx][tIdx] : 0
    }
}
