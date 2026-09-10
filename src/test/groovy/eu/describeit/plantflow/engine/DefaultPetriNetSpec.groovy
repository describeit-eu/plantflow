package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import spock.lang.Specification

class DefaultPetriNetSpec extends Specification {

    def 'should create DefaultPetriNetSpec with places, transitions and incidence matrix'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'process order', 'process order')

        def inputMatrix = [
            [1], // P_start
            [0],  // P_end
        ] as int[][]
        def outputMatrix = [
            [0], // P_start
            [1],  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)

        when:
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        then:
        net.places.size() == 2
        net.transitions == [tAction]
        net.startPlace == pStart
        net.endPlace == pEnd
        net.incidenceMatrix == incidenceMatrix

        and:
        net.getPlaceByIndex(0) == pStart
        net.getPlaceByIndex(1) == pEnd
        net.getPlaceByIndex(99) == null
        net.getTransitionByIndex(0) == tAction
        net.getTransitionByIndex(99) == null
    }

    def 'should evaluate isEnabled and getEnabledTransitions based on token marking, guards, and actions'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')

        def tGuard = new Transition(0, 'guarded', 'action1', 'checkCondition')
        def tAction = new Transition(1, 'actionOnly', 'action2', null)

        def inputMatrix = [
            [1, 0], // P_start
            [0, 1], // P_mid
            [0, 0],  // P_end
        ] as int[][]
        def outputMatrix = [
            [0, 0], // P_start
            [1, 0], // P_mid
            [0, 1],  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pMid, pEnd], [tGuard, tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [tGuard, tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerGuard('checkCondition') { ExecutionContext ctx, Token tok ->
            return tok.payload.valid == true
        }
        registry.registerAction('action1') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action2') { ExecutionContext ctx, Token tok -> tok }

        def marking = new Marking(net.places.size())
        def context = new ExecutionContext()

        expect:
        !net.isEnabled(null, marking, registry, context)
        !net.isEnabled(tGuard, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context).isEmpty()

        when: 'invalid token added to start place'
        def invalidToken = Token.of([valid: false])
        marking.addToken(pStart, invalidToken)

        then: 'guard rejects transition'
        !net.isEnabled(tGuard, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context).isEmpty()

        when: 'valid token replaces invalid token'
        marking.removeToken(pStart, invalidToken)
        def validToken = Token.of([valid: true])
        marking.addToken(pStart, validToken)

        then: 'guarded transition is enabled'
        net.isEnabled(tGuard, marking, registry, context)
        !net.isEnabled(tAction, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context) == [tGuard]
    }

    def 'should fire transition: consume tokens, execute action handler, and produce tokens'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'transform', 'transformAction', null)

        def inputMatrix = [
            [1], // P_start
            [0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0], // P_start
            [1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('transformAction') { ExecutionContext ctx, Token tok ->
            return [result: 'success']
        }

        def marking = new Marking(net.places.size())
        def context = new ExecutionContext()

        when: 'firing when not enabled'
        def firedWithoutToken = net.fire(tAction, marking, registry, context)

        then:
        !firedWithoutToken
        marking.isEmpty(pStart)
        marking.isEmpty(pEnd)

        when: 'firing with token in place'
        def token = Token.of([init: true])
        marking.addToken(pStart, token)
        def firedWithToken = net.fire(tAction, marking, registry, context)

        then:
        firedWithToken
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].payload == [result: 'success']
    }

    def 'should execute workflow to end using runUntilEnd'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def t1 = new Transition(0, 'step1', 'action1', null)
        def t2 = new Transition(1, 'step2', 'action2', null)

        def inputMatrix = [
            [1, 0], // P_start
            [0, 1], // P_mid
            [0, 0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0, 0], // P_start
            [1, 0], // P_mid
            [0, 1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pMid, pEnd], [t1, t2], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [s1: true])
        }
        registry.registerAction('action2') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [s2: true])
        }

        def marking = new Marking(net.places.size())
        def context = new ExecutionContext()
        def seedToken = Token.of([init: true])
        marking.addToken(pStart, seedToken)

        when:
        def finalMarking = net.runUntilEnd(marking, registry, context, null)

        then:
        finalMarking.isEmpty(pStart)
        finalMarking.isEmpty(pMid)
        !finalMarking.isEmpty(pEnd)
        finalMarking.getTokens(pEnd)[0].payload == [init: true, s1: true, s2: true]
    }

    def 'should handle action output tokens with various return types: #scenario'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, actionKey, actionKey, null)
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        if (actionKey && handlerResultSupplier) {
            registry.registerAction(actionKey) { ExecutionContext ctx, Token tok ->
                return handlerResultSupplier.call(tok)
            }
        }

        def marking = new Marking(net.places.size())
        def initialToken = Token.of([source: 'input'])
        marking.addToken(pStart, initialToken)

        when:
        def fired = net.fire(tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.getTokenCount(pEnd) == 1
        def produced = marking.getTokens(pEnd)[0]
        produced.payload == expectedPayload
        (produced.id == initialToken.id) == expectSameTokenId

        where:
        scenario         | actionKey | handlerResultSupplier                     | expectedPayload   | expectSameTokenId
        'returns Token'  | 'act1'    | { Token t -> Token.of([custom: true]) }   | [custom: true]    | false
        'returns Map'    | 'act2'    | { Token t -> [merged: 'yes'] }            | [merged: 'yes']   | true
        'returns null'   | 'act3'    | { Token t -> null }                       | [source: 'input'] | true
        'returns String' | 'act4'    | { Token t -> 'non-map result' }           | [source: 'input'] | true
    }

    def 'should handle actionless transition without action handler'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tNoAction = new ActionlessTestTransition(0, 'noAction')
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tNoAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tNoAction], incidenceMatrix, pStart, pEnd)
        def registry = new HandlerRegistry()
        def marking = new Marking(net.places.size())
        def token = Token.of([pass: 'through'])
        marking.addToken(pStart, token)

        when:
        def enabled = net.isEnabled(tNoAction, marking, registry, new ExecutionContext())
        def fired = net.fire(tNoAction, marking, registry, new ExecutionContext())

        then:
        enabled
        fired
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0] == token
    }

    def 'should fire sink transition with 0 output places'() {
        given: 'a transition with 1 input place and 0 output places'
        def pStart = new Place(0, 'start')
        def tSink = new Transition(0, 'sinkAction', 'sinkAction', null)
        def inputMatrix = [[1]] as int[][]
        def outputMatrix = [[0]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart], [tSink], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart], [tSink], incidenceMatrix, pStart, pStart)
        def registry = new HandlerRegistry().registerAction('sinkAction') { ctx, tok -> tok }
        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of())

        when:
        def fired = net.fire(tSink, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
    }

    def 'should return empty list when PetriNet has zero transitions'() {
        given:
        def p = new Place(0, 'p')
        def matrix = new IncidenceMatrix([p], [], null, null)
        def net = new DefaultPetriNet([p], [], matrix, p, p)
        def registry = new HandlerRegistry()
        def marking = new Marking(1)

        expect:
        net.getEnabledTransitions(marking, registry, new ExecutionContext()).isEmpty()
    }

    def 'should evaluate guard with empty token when input place is connected but empty'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tGuarded = new Transition(0, 'guardedStep', 'dummyAction', 'checkNull')

        def customMatrix = new EmptyConnectedIncidenceMatrix([pStart, pEnd], [tGuarded], pStart, pEnd)
        def net = new DefaultPetriNet([pStart, pEnd], [tGuarded], customMatrix, pStart, pEnd)
        def registry = new HandlerRegistry()
        registry.registerAction('dummyAction') { ctx, tok -> tok }
        registry.registerGuard('checkNull') { ctx, tok -> tok == null }
        def marking = new Marking(net.places.size())

        expect:
        net.isEnabled(tGuarded, marking, registry, new ExecutionContext())
    }

    static class ActionlessTestTransition extends Transition {
        ActionlessTestTransition(int index, String label) {
            super(index, label, 'dummyAction')
        }

        @Override
        String getActionKey() {
            return null
        }
    }

    static class EmptyConnectedIncidenceMatrix extends IncidenceMatrix {
        private final Place inputPlace
        private final Place outputPlace

        EmptyConnectedIncidenceMatrix(List<Place> places, List<Transition> transitions, Place inputPlace, Place outputPlace) {
            super(places, transitions, null, null)
            this.inputPlace = inputPlace
            this.outputPlace = outputPlace
        }

        @Override
        List<Place> getInputPlaces(int transitionIndex) {
            return [inputPlace]
        }

        @Override
        List<Place> getOutputPlaces(int transitionIndex) {
            return [outputPlace]
        }

        @Override
        int getInputWeight(int placeIndex, int transitionIndex) {
            return 0
        }

        @Override
        int getOutputWeight(int placeIndex, int transitionIndex) {
            return 1
        }
    }

    def 'should handle firing when token availability check is bypassed and place is empty'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'action', 'action', null)
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)
        def net = new UncheckedPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)
        def registry = new HandlerRegistry().registerAction('action') { ctx, tok -> tok }
        def marking = new Marking(net.places.size())

        when: 'firing without any tokens present in start place'
        def fired = net.fire(tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.getTokenCount(pEnd) == 1
    }

    static class UncheckedPetriNet extends DefaultPetriNet {
        UncheckedPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix matrix, Place start, Place end) {
            super(places, transitions, matrix, start, end)
        }

        @Override
        boolean isEnabled(Transition t, Marking m, HandlerRegistry r, ExecutionContext c) {
            return true
        }
    }

    def 'should fire transition without input places generating fallback token'() {
        given: 'a transition with 0 input places and 1 output place'
        def pEnd = new Place(0, 'end')
        def tSource = new Transition(0, 'produceAction', 'produceAction', null)
        def inputMatrix = [[0]] as int[][]
        def outputMatrix = [[1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pEnd], [tSource], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pEnd], [tSource], incidenceMatrix, pEnd, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('produceAction') { ExecutionContext ctx, Token tok ->
            return [generated: true]
        }

        def marking = new Marking(net.places.size())

        when:
        def fired = net.fire(tSource, marking, registry, new ExecutionContext())

        then:
        fired
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].payload == [generated: true]
    }

    def 'should evaluate guard with null token when input places are empty: #scenario'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tGuarded = new Transition(0, 'guardedStep', 'dummyAction', 'allowNullToken')

        def inputMatrix = [[hasInput ? 1 : 0], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tGuarded], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tGuarded], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('dummyAction') { ExecutionContext ctx, Token tok -> [:] }
        registry.registerGuard('allowNullToken') { ExecutionContext ctx, Token tok ->
            return allow && tok == null
        }

        def marking = new Marking(net.places.size())

        expect:
        net.isEnabled(tGuarded, marking, registry, new ExecutionContext()) == expectedEnabled
        !net.fire(null, marking, registry, new ExecutionContext())

        where:
        scenario                  | hasInput | allow | expectedEnabled
        'no input places allowed' | false    | true  | true
        'no input places denied'  | false    | false | false
    }

    def 'should fire transition with weighted arcs consuming and producing multiple tokens'() {
        given: 'places and transition with arc weight 2'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'consumeTwo', 'consumeTwo', null)

        def inputMatrix = [[2], [0]] as int[][]
        def outputMatrix = [[0], [2]] as int[][]
        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry().registerAction('consumeTwo') { ctx, tok ->
            return [processedCount: 2]
        }
        def marking = new Marking(net.places.size())

        when: 'insufficient tokens in start place'
        marking.addToken(pStart, Token.of())

        then: 'transition is not enabled and fire fails'
        !net.isEnabled(tAction, marking, registry, new ExecutionContext())
        !net.fire(tAction, marking, registry, new ExecutionContext())

        when: 'sufficient tokens (2) in start place'
        marking.addToken(pStart, Token.of())

        then: 'transition is enabled and fires successfully consuming 2 and producing 2'
        net.isEnabled(tAction, marking, registry, new ExecutionContext())
        net.fire(tAction, marking, registry, new ExecutionContext())
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 2
    }
}
