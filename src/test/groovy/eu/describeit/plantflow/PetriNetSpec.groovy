package eu.describeit.plantflow

import spock.lang.Shared
import spock.lang.Specification

class PetriNetSpec extends Specification {

    @Shared def pStart = new Place('P_start', 0, 'start')
    @Shared def pMid = new Place('P_mid', 1, 'mid')
    @Shared def pEnd = new Place('P_end', 2, 'end')
    @Shared def t1 = new Transition('T_0', 0, 'step1')
    @Shared def t2 = new Transition('T_1', 1, 'step2')

    def 'should lookup place by id: #lookupId'() {
        given:
        PetriNet net = new TestPetriNet(places: [pStart, pMid, pEnd])

        expect:
        net.getPlaceById(lookupId) == expectedPlace

        where:
        lookupId    | expectedPlace
        'P_start'   | pStart
        'P_mid'     | pMid
        'P_end'     | pEnd
        'P_unknown' | null
    }

    def 'should lookup transition by id: #lookupId'() {
        given:
        PetriNet net = new TestPetriNet(transitions: [t1, t2])

        expect:
        net.getTransitionById(lookupId) == expectedTransition

        where:
        lookupId    | expectedTransition
        'T_0'       | t1
        'T_1'       | t2
        'T_unknown' | null
    }

    def 'should determine if marking is empty across all net places: #description'() {
        given:
        def marking = new Marking([pStart, pMid, pEnd])
        tokenPlaces.each { Place p ->
            marking.addToken(p, RecordToken.of())
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
        def marking = new Marking([pStart, pMid, pEnd])
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def customToken = RecordToken.of([test: 123])

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
        def marking = new Marking([pStart, pMid, pEnd])
        def net = new TestPetriNet(
            places: [pStart, pMid, pEnd],
            transitions: [],
            startPlace: pStart,
            endPlace: pEnd
        )

        when:
        net.runUntilEnd(marking)

        then:
        marking.getTokenCount(pStart) == 1
        marking.getTokens(pStart)[0].payload == [:]
    }

    static class TestPetriNet implements PetriNet {
        List<Place> places = []
        List<Transition> transitions = []
        Place startPlace
        Place endPlace
        List<Transition> remainingEnabled = []
        List<Transition> firedTransitions = []

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
