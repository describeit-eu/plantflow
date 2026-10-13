package eu.describeit.plantflow.engine

import spock.lang.Specification

class IncidenceMatrixSpec extends Specification {

    def 'should create incidence matrix from input and output matrices'() {
        given:
        def inputMatrix = [[1, 0], [0, 1]] as int[][]
        def outputMatrix = [[0, 1], [1, 0]] as int[][]

        when:
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        then:
        matrix.getInputWeight(0, 0) == 1
        matrix.getInputWeight(0, 1) == 0
        matrix.getInputWeight(1, 0) == 0
        matrix.getInputWeight(1, 1) == 1
        matrix.getOutputWeight(0, 0) == 0
        matrix.getOutputWeight(0, 1) == 1
        matrix.getOutputWeight(1, 0) == 1
        matrix.getOutputWeight(1, 1) == 0
        matrix.getIncidence(0, 0) == -1
        matrix.getIncidence(0, 1) == 1
        matrix.getIncidence(1, 0) == 1
        matrix.getIncidence(1, 1) == -1
    }

    def 'should handle null matrices'() {
        when:
        def matrix = new IncidenceMatrix(null, null)

        then:
        matrix.getInputWeight(0, 0) == 0
        matrix.getOutputWeight(0, 0) == 0
        matrix.getIncidence(0, 0) == 0
    }

    def 'should handle empty matrices'() {
        when:
        def matrix = new IncidenceMatrix(new int[0][0], new int[0][0])

        then:
        matrix.getInputWeight(0, 0) == 0
        matrix.getOutputWeight(0, 0) == 0
        matrix.getIncidence(0, 0) == 0
    }

    def 'getMatrixValue should return 0 for out-of-bounds indices'() {
        given:
        def matrix = new IncidenceMatrix([[1]] as int[][], [[0]] as int[][])

        expect: 'pIdx beyond matrix length returns 0'
        matrix.getInputWeight(5, 0) == 0
        matrix.getOutputWeight(5, 0) == 0
        matrix.getIncidence(5, 0) == 0

        and: 'tIdx beyond row length returns 0'
        matrix.getInputWeight(0, 5) == 0
        matrix.getOutputWeight(0, 5) == 0
        matrix.getIncidence(0, 5) == 0

        and: 'both beyond bounds returns 0'
        matrix.getInputWeight(5, 5) == 0
    }

    def 'getMatrixValue should handle boundary indices correctly'() {
        given: 'a matrix with 2 places and 3 transitions'
        def inputMatrix = [
            [1, 0, 1],
            [0, 1, 0]
        ] as int[][]
        def outputMatrix = [
            [0, 1, 0],
            [1, 0, 1]
        ] as int[][]
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        expect: 'boundary: pIdx == matrix.length - 1 (last valid row)'
        matrix.getInputWeight(1, 0) == 0
        matrix.getInputWeight(1, 1) == 1
        matrix.getInputWeight(1, 2) == 0

        and: 'boundary: tIdx == row length - 1 (last valid column)'
        matrix.getInputWeight(0, 2) == 1
        matrix.getOutputWeight(1, 2) == 1

        and: 'boundary: pIdx == matrix.length (one past end)'
        matrix.getInputWeight(2, 0) == 0

        and: 'boundary: tIdx == row length (one past end)'
        matrix.getInputWeight(0, 3) == 0
    }

    def 'should handle jagged matrices with different row lengths'() {
        given:
        def inputMatrix = [
            [1, 1, 1],
            [0, 1]
        ] as int[][]
        def outputMatrix = [
            [0, 0],
            [1, 0, 1]
        ] as int[][]

        when:
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        then: 'missing values are treated as 0'
        matrix.getInputWeight(0, 0) == 1
        matrix.getInputWeight(0, 2) == 1
        matrix.getInputWeight(1, 0) == 0
        matrix.getInputWeight(1, 1) == 1
        matrix.getInputWeight(1, 2) == 0
        matrix.getOutputWeight(0, 0) == 0
        matrix.getOutputWeight(0, 1) == 0
        matrix.getOutputWeight(1, 0) == 1
        matrix.getOutputWeight(1, 2) == 1
    }

    def 'should compute incidence as output minus input'() {
        given:
        def inputMatrix = [[2], [0]] as int[][]
        def outputMatrix = [[0], [3]] as int[][]

        when:
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        then:
        matrix.getIncidence(0, 0) == -2
        matrix.getIncidence(1, 0) == 3
    }

    def 'should handle single place single transition matrix'() {
        given:
        def inputMatrix = [[1]] as int[][]
        def outputMatrix = [[1]] as int[][]

        when:
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        then:
        matrix.getInputWeight(0, 0) == 1
        matrix.getOutputWeight(0, 0) == 1
        matrix.getIncidence(0, 0) == 0
    }

    def 'should handle weighted arcs'() {
        given:
        def inputMatrix = [[3], [0], [0]] as int[][]
        def outputMatrix = [[0], [2], [1]] as int[][]

        when:
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        then:
        matrix.getInputWeight(0, 0) == 3
        matrix.getOutputWeight(1, 0) == 2
        matrix.getOutputWeight(2, 0) == 1
        matrix.getIncidence(0, 0) == -3
        matrix.getIncidence(1, 0) == 2
        matrix.getIncidence(2, 0) == 1
    }
}
