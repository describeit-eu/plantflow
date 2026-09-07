package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
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
        def marking = new Marking([pStart, pMid, pEnd])
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

    def 'should not auto-seed token when runUntilEnd is called with null token on non-empty marking'() {
        given:
        def marking = new Marking([pStart, pMid, pEnd])
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
        net.runUntilEnd(marking, registry, context)

        then:
        marking.getTokenCount(pStart) == 0
        marking.getTokenCount(pMid) == 1
        marking.getTokens(pMid)[0] == existingToken
    }

    def 'should return true for isNetEmpty when PetriNet has no places'() {
        given:
        def marking = new Marking([])
        def net = new TestPetriNet(places: [])

        expect:
        net.isNetEmpty(marking)
    }

    def 'should initialize Place with id, index and label'() {
        when:
        def place = new Place('P_1', 0, 'Custom Label')

        then:
        place.id == 'P_1'
        place.index == 0
        place.label == 'Custom Label'
    }

    def 'should reject Place creation with invalid label: #scenario'() {
        when:
        new Place('P_1', 0, invalidLabel)

        then:
        def ex = thrown(IllegalArgumentException)
        ['label cannot be null', 'label cannot be blank'].contains(ex.message)

        where:
        scenario      | invalidLabel
        'null label'  | null
        'empty label' | ''
        'blank label' | '   '
    }

    def 'should satisfy equals, hashCode, and toString for Place'() {
        given:
        def place1 = new Place('P_1', 0, 'Label A')
        def place2 = new Place('P_1', 0, 'Label A')
        def place3 = new Place('P_2', 1, 'Label B')

        expect:
        place1 == place2
        place1.hashCode() == place2.hashCode()
        place1 != place3
        place1.toString().contains('id:P_1')
        place1.toString().contains('index:0')
    }

    def 'should initialize Transition with id, index, label and optional keys: #scenario'() {
        when:
        def transition = constructorCall()

        then:
        transition.id == 'T_1'
        transition.index == 0
        transition.label == 'Step One'
        transition.actionKey == expectedActionKey
        transition.guardKey == expectedGuardKey

        where:
        scenario                       | constructorCall                                                      | expectedActionKey | expectedGuardKey
        'omitted optional keys'        | { -> new Transition('T_1', 0, 'Step One') }                          | 'Step One'        | null
        'explicit action and guard'    | { -> new Transition('T_1', 0, 'Step One', 'customAct', 'chkGuard') } | 'customAct'       | 'chkGuard'
//        'null action, explicit guard'  | { -> new Transition('T_1', 0, 'Step One', null, 'chkGuard') }        | 'Step One'        | 'chkGuard'
//        'empty action, null guard'     | { -> new Transition('T_1', 0, 'Step One', '', null) }                | 'Step One'        | null
    }

    def 'should reject Transition creation with invalid label: #scenario'() {
        when:
        new Transition('T_1', 0, invalidLabel)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('label cannot be null')

        where:
        scenario      | invalidLabel
        'null label'  | null
        'empty label' | ''
        'blank label' | '   '
    }

    def 'should satisfy equals, hashCode, and toString for Transition'() {
        given:
        def trans1 = new Transition('T_1', 0, 'Step 1', 'act1', 'grd1')
        def trans2 = new Transition('T_1', 0, 'Step 1', 'act1', 'grd1')
        def trans3 = new Transition('T_2', 1, 'Step 2', 'act2', 'grd2')

        expect:
        trans1 == trans2
        trans1.hashCode() == trans2.hashCode()
        trans1 != trans3
        trans1.toString().contains('id:T_1')
        trans1.toString().contains('label:Step 1')
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
        given:
        def places = [pStart, pMid, pEnd]
        def transitions = [t1, t2]

        when:
        def matrix = new IncidenceMatrix(places, transitions, inMatrix, outMatrix)

        then:
        matrix.getInputWeight(0, 0) == expectedIn00
        matrix.getOutputWeight(0, 0) == expectedOut00
        matrix.getIncidence(0, 0) == (expectedOut00 - expectedIn00)
        matrix.getInputWeight(2, 1) == expectedIn21
        matrix.getOutputWeight(2, 1) == expectedOut21
        matrix.getIncidence(2, 1) == (expectedOut21 - expectedIn21)

        where:
        scenario                     | inMatrix                                       | outMatrix                                      | expectedIn00 | expectedOut00 | expectedIn21 | expectedOut21
        'fully populated matrices'   | ([[1, 0], [0, 1], [0, 0]] as int[][])          | ([[0, 0], [1, 0], [0, 1]] as int[][])          | 1            | 0             | 0            | 1
        'null input and output'      | (int[][]) null                                 | (int[][]) null                                 | 0            | 0             | 0            | 0
        'null input only'            | (int[][]) null                                 | ([[2, 0], [0, 0], [0, 3]] as int[][])          | 0            | 2             | 0            | 3
        'null output only'           | ([[3, 0], [0, 0], [0, 4]] as int[][])          | (int[][]) null                                 | 3            | 0             | 4            | 0
        'truncated rows (fewer p)'   | ([[1, 0]] as int[][])                          | ([[0, 1]] as int[][])                          | 1            | 0             | 0            | 0
        'jagged columns (fewer t)'   | ([[1], [0], [0]] as int[][])                  | ([[0], [1], [0]] as int[][])                  | 1            | 0             | 0            | 0
    }

    def 'should query input and output connected places from IncidenceMatrix'() {
        given:
        def places = [pStart, pMid, pEnd]
        def transitions = [t1, t2]
        int[][] inMatrix = [[1, 0], [0, 2], [0, 0]]
        int[][] outMatrix = [[0, 0], [1, 0], [0, 1]]
        def matrix = new IncidenceMatrix(places, transitions, inMatrix, outMatrix)

        expect:
        matrix.getInputPlaces(0) == [pStart]
        matrix.getOutputPlaces(0) == [pMid]
        matrix.getInputPlaces(1) == [pMid]
        matrix.getOutputPlaces(1) == [pEnd]
    }

    def 'IncidenceMatrix places and transitions lists should be unmodifiable'() {
        given:
        def matrix = new IncidenceMatrix([pStart], [t1], null, null)

        when:
        matrix.places.add(pMid)

        then:
        thrown(UnsupportedOperationException)

        when:
        matrix.transitions.add(t2)

        then:
        thrown(UnsupportedOperationException)
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
