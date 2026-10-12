package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import eu.describeit.plantflow.UnregisteredHandlerException
import spock.lang.Specification

import static eu.describeit.plantflow.engine.ExecutionTestBase.*

class DefaultPetriNetSpec extends Specification {

    def 'should create DefaultPetriNetSpec with places, transitions and incidence matrix'() {
        given:
        def net = createLinearNet('start', 'end', 'process order', 'process order', null)
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

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [tGuard, tAction], incidenceMatrix, pStart, pEnd)

        def registry = createRegistry { reg ->
            reg.registerGuard('checkCondition') { ExecutionContext ctx, Token tok ->
                return tok.payload.valid == true
            }
            reg.registerAction('action1') { ExecutionContext ctx, Token tok -> tok }
            reg.registerAction('action2') { ExecutionContext ctx, Token tok -> tok }
        }

        def marking = createMarking(net.places.size())
        def context = createContext()

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
        def net = createLinearNet('start', 'end', 'transform', 'transformAction', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        def registry = createRegistry { reg ->
            reg.registerAction('transformAction') { ExecutionContext ctx, Token tok ->
                return [result: 'success']
            }
        }

        def marking = createMarking(net.places.size())
        def context = createContext()

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

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        def registry = createRegistry { reg ->
            reg.registerAction('action1') { ExecutionContext ctx, Token tok ->
                return tok.withPayload(tok.payload + [s1: true])
            }
            reg.registerAction('action2') { ExecutionContext ctx, Token tok ->
                return tok.withPayload(tok.payload + [s2: true])
            }
        }

        def marking = createMarking(net.places.size())
        def context = createContext()
        def seedToken = createToken([init: true])
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
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        if (actionKey && handlerResultSupplier) {
            registry.registerAction(actionKey) { ExecutionContext ctx, Token tok ->
                return handlerResultSupplier.call(tok)
            }
        }

        def marking = createMarking(net.places.size())
        def initialToken = createToken([source: 'input'])
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
        def tNoAction = new Transition(0, 'noAction')
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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
        def matrix = new IncidenceMatrix(null, null)
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

        def matrix = new IncidenceMatrix(null, null)
        def net = new EmptyConnectedPetriNet([pStart, pEnd], [tGuarded], matrix, pStart, pEnd, pStart)
        def registry = new HandlerRegistry()
        registry.registerAction('dummyAction') { ctx, tok -> tok }
        registry.registerGuard('checkNull') { ctx, tok -> tok == null }
        def marking = new Marking(net.places.size())

        expect:
        net.isEnabled(tGuarded, marking, registry, new ExecutionContext())
    }

    static class EmptyConnectedPetriNet extends DefaultPetriNet {
        private final Place inputPlace

        EmptyConnectedPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix matrix, Place start, Place end, Place inputPlace) {
            super(places, transitions, matrix, start, end)
            this.inputPlace = inputPlace
        }

        @Override
        protected List<Place> getInputPlaces(int transitionIndex) {
            return [inputPlace]
        }
    }

    def 'should handle firing when token availability check is bypassed and place is empty'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'action', 'action', null)
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
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

    def 'should deterministically select and fire first enabled transition when multiple are enabled'() {
        given: 'a net with two parallel transitions from the same place'
        def pStart = new Place(0, 'start')
        def pEnd1 = new Place(1, 'end1')
        def pEnd2 = new Place(2, 'end2')

        def tFirst = new Transition(0, 'first', 'first', null)
        def tSecond = new Transition(1, 'second', 'second', null)

        def inputMatrix = [
            [1, 1], // P_start
            [0, 0], // P_end1
            [0, 0]  // P_end2
        ] as int[][]
        def outputMatrix = [
            [0, 0], // P_start
            [1, 0], // P_end1
            [0, 1]  // P_end2
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd1, pEnd2], [tFirst, tSecond], incidenceMatrix, pStart, pEnd1)

        def registry = new HandlerRegistry()
        registry.registerAction('first') { ExecutionContext ctx, Token tok ->
            ctx['fired'] = 'first'
            return tok
        }
        registry.registerAction('second') { ExecutionContext ctx, Token tok ->
            ctx['fired'] = 'second'
            return tok
        }

        def marking = new Marking(net.places.size())
        def context = new ExecutionContext()

        when: 'adding token to start place and checking enabled transitions'
        marking.addToken(pStart, Token.of())
        def enabled = net.getEnabledTransitions(marking, registry, context)

        then: 'both transitions are enabled'
        enabled.size() == 2
        enabled[0].index == 0  // First transition in declaration order
        enabled[1].index == 1  // Second transition in declaration order

        when: 'firing the first enabled transition'
        net.fire(enabled[0], marking, registry, context)

        then: 'the first transition was fired'
        context['fired'] == 'first'
        marking.getTokenCount(pStart) == 0
        marking.getTokenCount(pEnd1) == 1
        marking.getTokenCount(pEnd2) == 0
    }

    def 'should throw UnregisteredHandlerException when checking isEnabled with unregistered guard'() {
        given: 'a net with a transition that has an unregistered guard'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tGuarded = new Transition(0, 'guarded', 'action', 'unregistered_guard')

        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tGuarded], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action') { ExecutionContext ctx, Token tok -> tok }
        // Guard is NOT registered

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of())

        when: 'checking if transition is enabled'
        net.isEnabled(tGuarded, marking, registry, new ExecutionContext())

        then: 'it throws UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered_guard')
    }

    def 'should throw UnregisteredHandlerException when firing transition with unregistered action'() {
        given: 'a net with a transition that has an unregistered action'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'action', 'unregistered_action', null)

        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        // Action is NOT registered

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of())

        when: 'firing the transition'
        net.fire(tAction, marking, registry, new ExecutionContext())

        then: 'it throws UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered_action')
    }

    def 'should fail fast in runUntilEnd when encountering unregistered handler'() {
        given: 'a net with a transition that has an unregistered action'
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def tFirst = new Transition(0, 'first', 'first', null)
        def tUnregistered = new Transition(1, 'unregistered', 'unregistered_action', null)
        def tThird = new Transition(2, 'third', 'third', null)

        def inputMatrix = [
            [1, 0, 0], // P_start
            [0, 1, 0], // P_mid
            [0, 0, 1]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0, 0, 0], // P_start
            [1, 0, 0], // P_mid
            [0, 1, 0]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [tFirst, tUnregistered, tThird], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('first') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('third') { ExecutionContext ctx, Token tok -> tok }
        // unregistered_action is NOT registered

        def marking = new Marking(net.places.size())

        when: 'running until end'
        net.runUntilEnd(marking, registry, new ExecutionContext(), Token.of())

        then: 'it throws UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered_action')
    }

    def 'should merge multiple consumed tokens from multiple input places'() {
        given: 'a join transition with two input places each having a token'
        def pStart = new Place(0, 'start')
        def pBranch1 = new Place(1, 'branch1')
        def pBranch2 = new Place(2, 'branch2')
        def pEnd = new Place(3, 'end')

        def tJoin = new Transition(0, 'join', 'joinAction', null)

        def inputMatrix = [[0], [1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [0], [1]] as int[][]

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pBranch1, pBranch2, pEnd], [tJoin], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('joinAction') { ExecutionContext ctx, Token tok -> tok }

        def marking = new Marking(net.places.size())
        marking.addToken(pBranch1, Token.of([branch: 1, shared: 'from-branch1']))
        marking.addToken(pBranch2, Token.of([branch: 2, shared: 'from-branch2', extra: 'only-in-2']))

        when:
        def fired = net.fire(tJoin, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pBranch1)
        marking.isEmpty(pBranch2)
        marking.getTokenCount(pEnd) == 1

        and: 'tokens are deep-merged with later token winning on conflicts'
        def result = marking.getTokens(pEnd)[0]
        result.payload.branch == 2
        result.payload.shared == 'from-branch2'
        result.payload.extra == 'only-in-2'
    }

    def 'should merge tokens with nested maps from multiple input places'() {
        given: 'a join transition with two input places having nested map payloads'
        def pStart = new Place(0, 'start')
        def pA = new Place(1, 'a')
        def pB = new Place(2, 'b')
        def pEnd = new Place(3, 'end')

        def tJoin = new Transition(0, 'join', 'joinAction', null)

        def inputMatrix = [[0], [1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pA, pB, pEnd], [tJoin], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('joinAction') { ExecutionContext ctx, Token tok -> tok }

        def marking = new Marking(net.places.size())
        marking.addToken(pA, Token.of([config: [host: 'a-host', port: 8080], fromA: true]))
        marking.addToken(pB, Token.of([config: [port: 9090, ssl: true], fromB: true]))

        when:
        net.fire(tJoin, marking, registry, new ExecutionContext())

        then:
        def result = marking.getTokens(pEnd)[0]
        result.payload.fromA == true
        result.payload.fromB == true
        def config = result.payload.config as Map
        config.host == 'a-host'
        config.port == 9090
        config.ssl == true
    }

    def 'should fire transition with multiple output places producing same token'() {
        given: 'a transition with 1 input and 2 output places'
        def pStart = new Place(0, 'start')
        def pOut1 = new Place(1, 'out1')
        def pOut2 = new Place(2, 'out2')
        def tFork = new Transition(0, 'fork', 'forkAction', null)

        def inputMatrix = [[1], [0], [0]] as int[][]
        def outputMatrix = [[0], [1], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pOut1, pOut2], [tFork], incidenceMatrix, pStart, pOut2)

        def registry = new HandlerRegistry()
        registry.registerAction('forkAction') { ExecutionContext ctx, Token tok -> [forked: true] }

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of([original: true]))

        when:
        def fired = net.fire(tFork, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pOut1) == 1
        marking.getTokenCount(pOut2) == 1
        marking.getTokens(pOut1)[0].payload == [forked: true]
        marking.getTokens(pOut2)[0].payload == [forked: true]
    }

    def 'should return Token directly from action handler without modification'() {
        given:
        def net = createLinearNet('start', 'end', 'transform', 'transformAction', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        def customToken = Token.of([custom: 'result'])
        def registry = new HandlerRegistry()
        registry.registerAction('transformAction') { ExecutionContext ctx, Token tok -> customToken }

        def marking = createMarking(net.places.size())
        marking.addToken(pStart, Token.of([input: 'data']))

        when:
        def fired = net.fire(tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].is(customToken)
    }

    def 'should handle getEnabledTransitions with mix of enabled and disabled transitions'() {
        given: 'a net with two transitions, only one enabled'
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

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action2') { ExecutionContext ctx, Token tok -> tok }

        def marking = createMarking(net.places.size())
        def context = createContext()

        when: 'no tokens - no transitions enabled'
        def enabledEmpty = net.getEnabledTransitions(marking, registry, context)

        then:
        enabledEmpty.isEmpty()

        when: 'token in start - only t1 enabled'
        marking.addToken(pStart, Token.of())
        def enabledOne = net.getEnabledTransitions(marking, registry, context)

        then:
        enabledOne.size() == 1
        enabledOne[0] == t1

        when: 'fire t1, token moves to mid - only t2 enabled'
        net.fire(t1, marking, registry, context)
        def enabledTwo = net.getEnabledTransitions(marking, registry, context)

        then:
        enabledTwo.size() == 1
        enabledTwo[0] == t2
    }

    def 'should handle fire with weighted input arcs consuming multiple tokens from same place'() {
        given: 'a transition requiring 3 tokens from one place'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tConsume = new Transition(0, 'consume3', 'consume3Action', null)

        def inputMatrix = [[3], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tConsume], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('consume3Action') { ExecutionContext ctx, Token tok -> [consumed: 3] }

        def marking = new Marking(net.places.size())

        when: 'only 2 tokens - not enabled'
        marking.addToken(pStart, Token.of([id: 1]))
        marking.addToken(pStart, Token.of([id: 2]))

        then:
        !net.isEnabled(tConsume, marking, registry, new ExecutionContext())
        !net.fire(tConsume, marking, registry, new ExecutionContext())

        when: '3 tokens - enabled and fires'
        marking.addToken(pStart, Token.of([id: 3]))

        then:
        net.isEnabled(tConsume, marking, registry, new ExecutionContext())

        when:
        def fired = net.fire(tConsume, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
    }

    def 'should handle transition with no action key producing merged token'() {
        given: 'a structural transition without action handler'
        def pStart = new Place(0, 'start')
        def pA = new Place(1, 'a')
        def pB = new Place(2, 'b')
        def pEnd = new Place(3, 'end')

        def tJoin = new Transition(0, 'join', null, null)

        def inputMatrix = [[0], [1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pA, pB, pEnd], [tJoin], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        def marking = new Marking(net.places.size())
        marking.addToken(pA, Token.of([fromA: true, shared: 'a']))
        marking.addToken(pB, Token.of([fromB: true, shared: 'b']))

        when:
        def fired = net.fire(tJoin, marking, registry, new ExecutionContext())

        then:
        fired
        marking.getTokenCount(pEnd) == 1
        def result = marking.getTokens(pEnd)[0]
        result.payload.fromA == true
        result.payload.fromB == true
        result.payload.shared == 'b'
    }

    def 'should handle isEnabled with transition having guard that receives merged context'() {
        given: 'a guarded transition with two input places'
        def pStart = new Place(0, 'start')
        def pA = new Place(1, 'a')
        def pB = new Place(2, 'b')
        def pEnd = new Place(3, 'end')

        def tGuarded = new Transition(0, 'guarded', 'action', 'checkBoth')

        def inputMatrix = [[0], [1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pA, pB, pEnd], [tGuarded], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action') { ExecutionContext ctx, Token tok -> tok }
        registry.registerGuard('checkBoth') { ExecutionContext ctx, Token tok -> tok != null }

        def marking = new Marking(net.places.size())
        marking.addToken(pA, Token.of([a: true]))
        marking.addToken(pB, Token.of([b: true]))

        expect:
        net.isEnabled(tGuarded, marking, registry, new ExecutionContext())
    }

    def 'should fire transition with weighted input arc consuming exactly requiredWeight tokens'() {
        given: 'a transition with input weight 1 (boundary: exactly 1 token needed)'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'single', 'singleAction', null)

        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('singleAction') { ExecutionContext ctx, Token tok -> tok }

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of([v: 1]))

        when: 'exactly 1 token present (boundary: w < 1 means loop runs once)'
        def fired = net.fire(tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
    }

    def 'should fire transition with weighted input arc consuming multiple tokens (boundary: w < requiredWeight)'() {
        given: 'a transition with input weight 3'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tAction = new Transition(0, 'triple', 'tripleAction', null)

        def inputMatrix = [[3], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('tripleAction') { ExecutionContext ctx, Token tok -> tok }

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of([id: 1]))
        marking.addToken(pStart, Token.of([id: 2]))
        marking.addToken(pStart, Token.of([id: 3]))

        when: 'firing consumes all 3 tokens'
        def fired = net.fire(tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
    }

    def 'should merge exactly two consumed tokens (boundary: i < consumedTokens.size())'() {
        given: 'a join transition with exactly 2 input places each having 1 token'
        def pA = new Place(0, 'a')
        def pB = new Place(1, 'b')
        def pEnd = new Place(2, 'end')

        def tJoin = new Transition(0, 'join', null, null)

        def inputMatrix = [[1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pA, pB, pEnd], [tJoin], incidenceMatrix, pA, pEnd)

        def registry = new HandlerRegistry()
        def marking = new Marking(net.places.size())
        marking.addToken(pA, Token.of([fromA: true, shared: 'a']))
        marking.addToken(pB, Token.of([fromB: true, shared: 'b']))

        when:
        def fired = net.fire(tJoin, marking, registry, new ExecutionContext())

        then: 'exactly 2 tokens merged (loop runs for i=1, boundary at i < 2)'
        fired
        marking.getTokenCount(pEnd) == 1
        def result = marking.getTokens(pEnd)[0]
        result.payload.fromA == true
        result.payload.fromB == true
        result.payload.shared == 'b'
    }

    def 'should merge three consumed tokens from three input places'() {
        given: 'a join transition with 3 input places'
        def pA = new Place(0, 'a')
        def pB = new Place(1, 'b')
        def pC = new Place(2, 'c')
        def pEnd = new Place(3, 'end')

        def tJoin = new Transition(0, 'join', null, null)

        def inputMatrix = [[1], [1], [1], [0]] as int[][]
        def outputMatrix = [[0], [0], [0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pA, pB, pC, pEnd], [tJoin], incidenceMatrix, pA, pEnd)

        def registry = new HandlerRegistry()
        def marking = new Marking(net.places.size())
        marking.addToken(pA, Token.of([order: 1, common: 'a']))
        marking.addToken(pB, Token.of([order: 2, common: 'b']))
        marking.addToken(pC, Token.of([order: 3, common: 'c']))

        when:
        net.fire(tJoin, marking, registry, new ExecutionContext())

        then:
        marking.getTokenCount(pEnd) == 1
        def result = marking.getTokens(pEnd)[0]
        result.payload.order == 3
        result.payload.common == 'c'
    }

    def 'getInputPlaces should only return places with positive input weight'() {
        given: 'a net with places having different input weights for a transition'
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def tAction = new Transition(0, 'action', 'action', null)

        def inputMatrix = [
            [1], // P_start: weight 1 (input)
            [0], // P_mid: weight 0 (not input)
            [0]  // P_end: weight 0 (not input)
        ] as int[][]
        def outputMatrix = [
            [0],
            [1],
            [0]
        ] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new InputPlacesTestNet([pStart, pMid, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        when:
        def inputPlaces = net.exposeGetInputPlaces(0)

        then: 'only places with weight > 0 are returned (boundary: weight == 0 excluded)'
        inputPlaces.size() == 1
        inputPlaces[0] == pStart
    }

    def 'getInputPlaces should return empty list when no place has positive input weight'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def tSource = new Transition(0, 'source', null, null)

        def inputMatrix = [[0], [0]] as int[][]
        def outputMatrix = [[1], [0]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new InputPlacesTestNet([pStart, pEnd], [tSource], incidenceMatrix, pStart, pEnd)

        when:
        def inputPlaces = net.exposeGetInputPlaces(0)

        then:
        inputPlaces.isEmpty()
    }

    def 'getOutputPlaces should only return places with positive output weight'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def tAction = new Transition(0, 'action', 'action', null)

        def inputMatrix = [[1], [0], [0]] as int[][]
        def outputMatrix = [[0], [1], [0]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new OutputPlacesTestNet([pStart, pMid, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        when:
        def outputPlaces = net.exposeGetOutputPlaces(0)

        then:
        outputPlaces.size() == 1
        outputPlaces[0] == pMid
    }

    static class InputPlacesTestNet extends DefaultPetriNet {
        InputPlacesTestNet(List<Place> places, List<Transition> transitions, IncidenceMatrix matrix, Place start, Place end) {
            super(places, transitions, matrix, start, end)
        }

        List<Place> exposeGetInputPlaces(int transitionIndex) {
            return getInputPlaces(transitionIndex)
        }
    }

    static class OutputPlacesTestNet extends DefaultPetriNet {
        OutputPlacesTestNet(List<Place> places, List<Transition> transitions, IncidenceMatrix matrix, Place start, Place end) {
            super(places, transitions, matrix, start, end)
        }

        List<Place> exposeGetOutputPlaces(int transitionIndex) {
            return getOutputPlaces(transitionIndex)
        }
    }
}
