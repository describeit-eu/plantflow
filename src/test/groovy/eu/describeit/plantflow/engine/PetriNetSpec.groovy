package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import spock.lang.Shared
import spock.lang.Specification

class PetriNetSpec extends Specification {

    @Shared def pStart = new Place(0, 'start')
    @Shared def pMid = new Place(1, 'mid')
    @Shared def pEnd = new Place(2, 'end')
    @Shared def t1 = new Transition(0, 'step1', 'step1', null)
    @Shared def t2 = new Transition(1, 'step2', 'step2', null)

    def 'should lookup place by index: #lookupIndex'() {
        given:
        PetriNet net = new TestPetriNet(places: [pStart, pMid, pEnd])

        expect:
        net.getPlaceByIndex(lookupIndex) == expectedPlace

        where:
        lookupIndex | expectedPlace
        0           | pStart
        1           | pMid
        2           | pEnd
        99          | null
    }

    def 'should lookup transition by index: #lookupIndex'() {
        given:
        PetriNet net = new TestPetriNet(transitions: [t1, t2])

        expect:
        net.getTransitionByIndex(lookupIndex) == expectedTransition

        where:
        lookupIndex | expectedTransition
        0           | t1
        1           | t2
        99          | null
    }

    def 'should determine if marking is empty across all net places: #description'() {
        given:
        def marking = new Marking(3)
        tokenPlaces.each { Place p ->
            marking.addToken(p, Token.of())
        }

        PetriNet net = new TestPetriNet(places: [pStart, pMid, pEnd], startPlace: pStart, endPlace: pEnd)

        expect:
        net.isNetEmpty(marking) == expectedEmpty

        where:
        description              | tokenPlaces      | expectedEmpty
        'no tokens in any place' | []               | true
        'token in start place'   | [pStart]         | false
        'token in end place'     | [pEnd]           | false
        'token in both places'   | [pStart, pEnd]   | false
    }

    def 'should execute default runUntilEnd workflow loop'() {
        given:
        def marking = new Marking(3)
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def customToken = Token.of([test: 123])

        def net = new TestPetriNet(
            places: [pStart, pMid, pEnd],
            transitions: [t1, t2],
            startPlace: pStart,
            endPlace: pEnd,
            remainingEnabled: [t1, t2]
        )

        when:
        def resultMarking = net.runUntilEnd(marking, registry, context, customToken)

        then:
        net.firedTransitions == [t1, t2]
        marking.getTokenCount(pStart) == 1
        marking.getTokens(pStart)[0] == customToken
        resultMarking == marking
    }

    def 'should auto-seed token when runUntilEnd is called on empty marking'() {
        given:
        def marking = new Marking(3)
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def net = new TestPetriNet(
            places: [pStart, pMid, pEnd],
            transitions: [],
            startPlace: pStart,
            endPlace: pEnd
        )

        when:
        net.runUntilEnd(marking, registry, context, null)

        then:
        marking.getTokenCount(pStart) == 1
        marking.getTokens(pStart)[0].payload == [:]
    }

    def 'should not auto-seed token when runUntilEnd is called with null token on non-empty marking'() {
        given:
        def marking = new Marking(3)
        def existingToken = Token.of([init: true])
        marking.addToken(pMid, existingToken)

        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def net = new TestPetriNet(
            places: [pStart, pMid, pEnd],
            transitions: [],
            startPlace: pStart,
            endPlace: pEnd
        )

        when:
        net.runUntilEnd(marking, registry, context, null)

        then:
        marking.getTokenCount(pStart) == 0
        marking.getTokenCount(pMid) == 1
        marking.getTokens(pMid)[0] == existingToken
    }

    def 'should return true for isNetEmpty when PetriNet has no places'() {
        given:
        def marking = new Marking(0)
        def net = new TestPetriNet(places: [])

        expect:
        net.isNetEmpty(marking)
    }

    def 'should delegate token management and query default methods to marking'() {
        given:
        def marking = new Marking(3)
        def net = new TestPetriNet(places: [pStart, pMid, pEnd], startPlace: pStart, endPlace: pEnd, marking: marking)
        def token1 = Token.of([key: 'val1'])
        def token2 = Token.of([key: 'val2'])

        expect: 'initially empty'
        net.isEmpty(pStart)
        net.isEmpty(0)
        net.getTokenCount(pStart) == 0
        net.getTokenCount(0) == 0
        net.getTokens(pStart).isEmpty()

        when: 'addToken is invoked via PetriNet interface default method'
        net.addToken(pStart, token1)

        then:
        !net.isEmpty(pStart)
        !net.isEmpty(0)
        net.getTokenCount(pStart) == 1
        net.getTokenCount(0) == 1
        net.getTokens(pStart) == [token1]

        when: 'seedToken is invoked with explicit token'
        net.seedToken(token2)

        then:
        net.getTokenCount(pStart) == 2
        net.getTokens(pStart) == [token1, token2]

        when: 'seedToken is invoked with null token'
        def freshMarking = new Marking(3)
        def freshNet = new TestPetriNet(places: [pStart, pMid, pEnd], startPlace: pStart, endPlace: pEnd, marking: freshMarking)
        freshNet.seedToken(null)

        then:
        freshNet.getTokenCount(pStart) == 1
        freshNet.getTokens(pStart)[0].payload == [:]
    }

    def 'should delegate execution convenience methods to current marking'() {
        given:
        def marking = new Marking(3)
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def net = new TestPetriNet(
            places: [pStart, pMid, pEnd],
            transitions: [t1, t2],
            startPlace: pStart,
            endPlace: pEnd,
            marking: marking,
            remainingEnabled: [t1]
        )

        expect:
        net.isEnabled(t1, registry, context)
        !net.isEnabled(t2, registry, context)
        net.getEnabledTransitions(registry, context) == [t1]

        when:
        def fired = net.fire(t1, registry, context)

        then:
        fired
        net.firedTransitions == [t1]
        !net.fire(t2, registry, context)

        when:
        net.remainingEnabled = [t2]
        def finalMarking = net.runUntilEnd(registry, context, Token.of([run: true]))

        then:
        net.firedTransitions == [t1, t2]
        finalMarking == marking
    }

    def 'should manage ExecutionContext variables with default, get, set, and property syntax: #scenario'() {
        when:
        def context = constructorCall()

        then:
        context.get('existing') == expectedInitial
        context['existing'] == expectedInitial

        when:
        context.set('newKey', 'newValue')
        context['anotherKey'] = 42

        then:
        context.get('newKey') == 'newValue'
        context['anotherKey'] == 42
        context.toString() != null

        where:
        scenario                | constructorCall                                  | expectedInitial
        'default constructor'   | { -> new ExecutionContext() }                    | null
        'null initial map'      | { -> new ExecutionContext(null) }                | null
        'populated initial map' | { -> new ExecutionContext([existing: 'found']) } | 'found'
    }

    def 'should initialize IncidenceMatrix and handle boundary/null matrices correctly: #scenario'() {
        when:
        def matrix = new IncidenceMatrix(inMatrix as int[][], outMatrix as int[][])

        then:
        matrix.getInputWeight( 0, 0) == expectedIn00
        matrix.getOutputWeight(0, 0) == expectedOut00
        matrix.getIncidence(   0, 0) == (expectedOut00 - expectedIn00)
        matrix.getInputWeight( 2, 1) == expectedIn21
        matrix.getOutputWeight(2, 1) == expectedOut21
        matrix.getIncidence(   2, 1) == (expectedOut21 - expectedIn21)

        where:
        scenario                   | inMatrix                 | outMatrix                | expectedIn00 | expectedOut00 | expectedIn21 | expectedOut21
        'fully populated matrices' | [[1, 0], [0, 1], [0, 0]] | [[0, 0], [1, 0], [0, 1]] | 1            | 0             | 0            | 1
        'null input and output'    | null                     | null                     | 0            | 0             | 0            | 0
        'null input only'          | null                     | [[2, 0], [0, 0], [0, 3]] | 0            | 2             | 0            | 3
        'null output only'         | [[3, 0], [0, 0], [0, 4]] | null                     | 3            | 0             | 4            | 0
        'truncated rows (fewer p)' | [[1, 0]]                 | [[0, 1]]                 | 1            | 0             | 0            | 0
        'jagged columns (fewer t)' | [[1], [0], [0]]          | [[0], [1], [0]]          | 1            | 0             | 0            | 0
    }

    static class TestPetriNet implements PetriNet {
        List<Place> places = []
        List<Transition> transitions = []
        Place startPlace
        Place endPlace
        Marking marking
        List<Transition> remainingEnabled = []
        List<Transition> firedTransitions = []

        @Override
        Marking getMarking() {
            if (marking == null) {
                marking = new Marking(places)
            }
            return marking
        }

        @Override
        List<Place> getPlaces() { return places }

        @Override
        List<Transition> getTransitions() { return transitions }

        @Override
        Place getStartPlace() { return startPlace }

        @Override
        Place getEndPlace() { return endPlace }

        @Override
        boolean isEnabled(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
            return remainingEnabled.contains(transition)
        }

        @Override
        List<Transition> getEnabledTransitions(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
            return remainingEnabled ? [remainingEnabled[0]] : []
        }

        @Override
        boolean fire(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
            if (remainingEnabled.contains(transition)) {
                remainingEnabled.remove(transition)
                firedTransitions.add(transition)
                return true
            }
            return false
        }
    }
}
