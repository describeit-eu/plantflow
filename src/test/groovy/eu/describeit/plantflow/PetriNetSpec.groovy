package eu.describeit.plantflow

import spock.lang.Specification

class PetriNetSpec extends Specification {

    def "should create PetriNet with places, transitions and algebraic incidence matrix"() {
        given:
        def pStart = new Place("P_start", 0, "start")
        def pEnd = new Place("P_end", 1, "end")
        def tAction = new Transition("T_0", 0, "process order")

        // Input matrix: P_start -> T_0 (1 token consumed from P_start)
        // Output matrix: T_0 -> P_end (1 token produced into P_end)
        def inputMatrix = [
            [1], // P_start
            [0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0], // P_start
            [1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)

        when:
        def net = new PetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        then:
        net.places.size() == 2
        net.transitions.size() == 1
        net.startPlace == pStart
        net.endPlace == pEnd

        and:
        incidenceMatrix.getInputWeight(0, 0) == 1
        incidenceMatrix.getInputWeight(1, 0) == 0
        incidenceMatrix.getOutputWeight(0, 0) == 0
        incidenceMatrix.getOutputWeight(1, 0) == 1
        incidenceMatrix.getIncidence(0, 0) == -1
        incidenceMatrix.getIncidence(1, 0) == 1

        and:
        incidenceMatrix.getInputPlaces(0) == [pStart]
        incidenceMatrix.getOutputPlaces(0) == [pEnd]
    }
}
