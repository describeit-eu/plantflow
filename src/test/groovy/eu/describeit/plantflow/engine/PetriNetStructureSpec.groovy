package eu.describeit.plantflow.engine

import spock.lang.Specification

class PetriNetStructureSpec extends Specification {

    def 'should lookup place by index using default method'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def matrix = new IncidenceMatrix(null, null)
        PetriNetStructure net = new DefaultPetriNet([pStart, pMid, pEnd], [], matrix, pStart, pEnd)

        expect:
        net.getPlaceByIndex(0) == pStart
        net.getPlaceByIndex(1) == pMid
        net.getPlaceByIndex(2) == pEnd
        net.getPlaceByIndex(99) == null
        net.getPlaceByIndex(-1) == null
    }

    def 'should lookup transition by index using default method'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def t1 = new Transition(0, 't1', null, null)
        def t2 = new Transition(1, 't2', null, null)
        def matrix = new IncidenceMatrix(null, null)
        PetriNetStructure net = new DefaultPetriNet([pStart, pEnd], [t1, t2], matrix, pStart, pEnd)

        expect:
        net.getTransitionByIndex(0) == t1
        net.getTransitionByIndex(1) == t2
        net.getTransitionByIndex(99) == null
        net.getTransitionByIndex(-1) == null
    }

    def 'should expose topology via interface methods'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def t1 = new Transition(0, 'action', 'act', null)
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        PetriNetStructure net = new DefaultPetriNet([pStart, pEnd], [t1], matrix, pStart, pEnd)

        expect:
        net.places == [pStart, pEnd]
        net.transitions == [t1]
        net.startPlace == pStart
        net.endPlace == pEnd
        net.incidenceMatrix.is(matrix)
    }

    def 'should have exactly 5 abstract methods plus 2 default methods'() {
        given:
        def abstractMethods = PetriNetStructure.methods.findAll { it.default == false && !it.synthetic }.collect { it.name }.sort()
        def defaultMethods = PetriNetStructure.methods.findAll { it.default == true && !it.synthetic }.collect { it.name }.sort()

        expect: '5 abstract topology methods'
        abstractMethods == ['getEndPlace', 'getIncidenceMatrix', 'getPlaces', 'getStartPlace', 'getTransitions']

        and: '2 default lookup methods'
        defaultMethods == ['getPlaceByIndex', 'getTransitionByIndex']
    }

    def 'should not have any execution or marking methods'() {
        given:
        def methodNames = PetriNetStructure.methods.collect { it.name }

        expect: 'no execution methods'
        !methodNames.contains('isEnabled')
        !methodNames.contains('fire')
        !methodNames.contains('getEnabledTransitions')
        !methodNames.contains('runUntilEnd')
        !methodNames.contains('step')

        and: 'no marking methods'
        !methodNames.contains('getMarking')
        !methodNames.contains('addToken')
        !methodNames.contains('seedToken')
        !methodNames.contains('getTokens')
        !methodNames.contains('getTokenCount')
        !methodNames.contains('isEmpty')
        !methodNames.contains('isNetEmpty')
    }
}
