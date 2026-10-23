package eu.describeit.plantflow.engine

import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class DefaultPetriNetSpec extends Specification {

    def 'should create DefaultPetriNet with places, transitions and incidence matrix'() {
        given:
        def net = simpleLinearNet('start', 'end', 'process order', 'process order', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        expect:
        net.places.size() == 2
        net.transitions == [tAction]
        net.startPlace == pStart
        net.endPlace == pEnd

        and:
        net.getPlaceByIndex(0) == pStart
        net.getPlaceByIndex(1) == pEnd
        net.getPlaceByIndex(99) == null
        net.getTransitionByIndex(0) == tAction
        net.getTransitionByIndex(99) == null
    }

    def 'should implement PetriNetStructure interface'() {
        given:
        def net = simpleLinearNet()

        expect:
        net instanceof PetriNetStructure
    }

    def 'should expose incidence matrix'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'action', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        expect:
        net.incidenceMatrix.getInputWeight(pStart.index, tAction.index) == 1
        net.incidenceMatrix.getOutputWeight(pEnd.index, tAction.index) == 1
        net.incidenceMatrix.getInputWeight(pEnd.index, tAction.index) == 0
        net.incidenceMatrix.getOutputWeight(pStart.index, tAction.index) == 0
    }

    def 'should have unmodifiable places list'() {
        given:
        def net = simpleLinearNet()

        when:
        net.places.add(new Place(99, 'extra'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'should have unmodifiable transitions list'() {
        given:
        def net = simpleLinearNet()

        when:
        net.transitions.add(new Transition(99, 'extra'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'should not have marking field or execution methods'() {
        given:
        def net = simpleLinearNet()

        expect: 'no marking property'
        !net.metaClass.respondsTo(net, 'getMarking')

        and: 'no execution methods'
        !net.metaClass.respondsTo(net, 'isEnabled', Transition, Marking, Class, Class)
        !net.metaClass.respondsTo(net, 'fire', Transition, Marking, Class, Class)
        !net.metaClass.respondsTo(net, 'getEnabledTransitions', Marking, Class, Class)
    }

    def 'should create net with zero transitions'() {
        given:
        def p = new Place(0, 'p')
        def matrix = new IncidenceMatrix(null, null)

        when:
        def net = new DefaultPetriNet([p], [], matrix, p, p)

        then:
        net.places.size() == 1
        net.transitions.isEmpty()
        net.startPlace == p
        net.endPlace == p
    }

    def 'should create net with multiple places and transitions'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def t1 = new Transition(0, 'step1', 'action1', null)
        def t2 = new Transition(1, 'step2', 'action2', null)

        def inputMatrix = [
            [1, 0],
            [0, 1],
            [0, 0]
        ] as int[][]
        def outputMatrix = [
            [0, 0],
            [1, 0],
            [0, 1]
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)

        when:
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        then:
        net.places == [pStart, pMid, pEnd]
        net.transitions == [t1, t2]
        net.startPlace == pStart
        net.endPlace == pEnd
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(1, 0) == 1
        net.incidenceMatrix.getInputWeight(1, 1) == 1
        net.incidenceMatrix.getOutputWeight(2, 1) == 1
    }

    def 'should lookup place by index with boundary values'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def matrix = new IncidenceMatrix(null, null)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [], matrix, pStart, pEnd)

        expect:
        net.getPlaceByIndex(0) == pStart
        net.getPlaceByIndex(1) == pMid
        net.getPlaceByIndex(2) == pEnd
        net.getPlaceByIndex(-1) == null
        net.getPlaceByIndex(3) == null
        net.getPlaceByIndex(99) == null
    }

    def 'should lookup transition by index with boundary values'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def t1 = new Transition(0, 't1', null, null)
        def t2 = new Transition(1, 't2', null, null)
        def matrix = new IncidenceMatrix(null, null)
        def net = new DefaultPetriNet([pStart, pEnd], [t1, t2], matrix, pStart, pEnd)

        expect:
        net.getTransitionByIndex(0) == t1
        net.getTransitionByIndex(1) == t2
        net.getTransitionByIndex(-1) == null
        net.getTransitionByIndex(2) == null
        net.getTransitionByIndex(99) == null
    }
}
