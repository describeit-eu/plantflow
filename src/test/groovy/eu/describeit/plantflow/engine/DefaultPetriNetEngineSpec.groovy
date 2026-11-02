package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import eu.describeit.plantflow.UnregisteredHandlerException
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class DefaultPetriNetEngineSpec extends Specification {

    def engine = new DefaultPetriNetEngine()

    def 'should evaluate isEnabled based on token marking, guards, and actions'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')

        def tGuard = new Transition(0, 'guarded', 'action1', 'checkCondition')
        def tAction = new Transition(1, 'actionOnly', 'action2', null)

        def inputMatrix = [
            [1, 0],
            [0, 1],
            [0, 0],
        ] as int[][]
        def outputMatrix = [
            [0, 0],
            [1, 0],
            [0, 1],
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [tGuard, tAction], incidenceMatrix, pStart, pEnd)

        def registry = registry { reg ->
            reg.registerGuard('checkCondition') { ExecutionContext ctx, Token tok ->
                return tok.payload.valid == true
            }
            reg.registerAction('action1') { ExecutionContext ctx, Token tok -> tok }
            reg.registerAction('action2') { ExecutionContext ctx, Token tok -> tok }
        }

        def marking = marking(net.places.size())
        def context = context()

        expect:
        !engine.isEnabled(net, null, marking, registry, context)
        !engine.isEnabled(net, tGuard, marking, registry, context)
        engine.getEnabledTransitions(net, marking, registry, context).isEmpty()

        when: 'invalid token added to start place'
        def invalidToken = Token.of([valid: false])
        marking.addToken(pStart, invalidToken)

        then: 'guard rejects transition'
        !engine.isEnabled(net, tGuard, marking, registry, context)
        engine.getEnabledTransitions(net, marking, registry, context).isEmpty()

        when: 'valid token replaces invalid token'
        marking.removeToken(pStart, invalidToken)
        def validToken = Token.of([valid: true])
        marking.addToken(pStart, validToken)

        then: 'guarded transition is enabled'
        engine.isEnabled(net, tGuard, marking, registry, context)
        !engine.isEnabled(net, tAction, marking, registry, context)
        engine.getEnabledTransitions(net, marking, registry, context) == [tGuard]
    }

    def 'should fire transition: consume tokens, execute action handler, and produce tokens'() {
        given:
        def net = simpleLinearNet('start', 'end', 'transform', 'transformAction', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        def registry = registry { reg ->
            reg.registerAction('transformAction') { ExecutionContext ctx, Token tok ->
                return [result: 'success']
            }
        }

        def marking = marking(net.places.size())
        def context = context()

        when: 'firing when not enabled'
        def firedWithoutToken = engine.fire(net, tAction, marking, registry, context)

        then:
        !firedWithoutToken
        marking.isEmpty(pStart)
        marking.isEmpty(pEnd)

        when: 'firing with token in place'
        def token = Token.of([init: true])
        marking.addToken(pStart, token)
        def firedWithToken = engine.fire(net, tAction, marking, registry, context)

        then:
        firedWithToken
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].payload == [result: 'success']
    }

    @Unroll
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

        def marking = marking(net.places.size())
        def initialToken = token([source: 'input'])
        marking.addToken(pStart, initialToken)

        when:
        def fired = engine.fire(net, tAction, marking, registry, new ExecutionContext())

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
        def enabled = engine.isEnabled(net, tNoAction, marking, registry, new ExecutionContext())
        def fired = engine.fire(net, tNoAction, marking, registry, new ExecutionContext())

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
        def fired = engine.fire(net, tSink, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
    }

    def 'should return empty list when net has zero transitions'() {
        given:
        def p = new Place(0, 'p')
        def matrix = new IncidenceMatrix(null, null)
        def net = new DefaultPetriNet([p], [], matrix, p, p)
        def registry = new HandlerRegistry()
        def marking = new Marking(1)

        expect:
        engine.getEnabledTransitions(net, marking, registry, new ExecutionContext()).isEmpty()
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
        def fired = engine.fire(net, tSource, marking, registry, new ExecutionContext())

        then:
        fired
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].payload == [generated: true]
    }

    @Unroll
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
        engine.isEnabled(net, tGuarded, marking, registry, new ExecutionContext()) == expectedEnabled
        !engine.fire(net, null, marking, registry, new ExecutionContext())

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
        !engine.isEnabled(net, tAction, marking, registry, new ExecutionContext())
        !engine.fire(net, tAction, marking, registry, new ExecutionContext())

        when: 'sufficient tokens (2) in start place'
        marking.addToken(pStart, Token.of())

        then: 'transition is enabled and fires successfully consuming 2 and producing 2'
        engine.isEnabled(net, tAction, marking, registry, new ExecutionContext())
        engine.fire(net, tAction, marking, registry, new ExecutionContext())
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
            [1, 1],
            [0, 0],
            [0, 0]
        ] as int[][]
        def outputMatrix = [
            [0, 0],
            [1, 0],
            [0, 1]
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
        def enabled = engine.getEnabledTransitions(net, marking, registry, context)

        then: 'both transitions are enabled'
        enabled.size() == 2
        enabled[0].index == 0
        enabled[1].index == 1

        when: 'firing the first enabled transition'
        engine.fire(net, enabled[0], marking, registry, context)

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

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of())

        when: 'checking if transition is enabled'
        engine.isEnabled(net, tGuarded, marking, registry, new ExecutionContext())

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

        def marking = new Marking(net.places.size())
        marking.addToken(pStart, Token.of())

        when: 'firing the transition'
        engine.fire(net, tAction, marking, registry, new ExecutionContext())

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
        def fired = engine.fire(net, tJoin, marking, registry, new ExecutionContext())

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
        engine.fire(net, tJoin, marking, registry, new ExecutionContext())

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
        def fired = engine.fire(net, tFork, marking, registry, new ExecutionContext())

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
        def net = simpleLinearNet('start', 'end', 'transform', 'transformAction', null)
        def pStart = net.startPlace
        def pEnd = net.endPlace
        def tAction = net.transitions[0]

        def customToken = Token.of([custom: 'result'])
        def registry = new HandlerRegistry()
        registry.registerAction('transformAction') { ExecutionContext ctx, Token tok -> customToken }

        def marking = marking(net.places.size())
        marking.addToken(pStart, Token.of([input: 'data']))

        when:
        def fired = engine.fire(net, tAction, marking, registry, new ExecutionContext())

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
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action2') { ExecutionContext ctx, Token tok -> tok }

        def marking = marking(net.places.size())
        def context = context()

        when: 'no tokens - no transitions enabled'
        def enabledEmpty = engine.getEnabledTransitions(net, marking, registry, context)

        then:
        enabledEmpty.isEmpty()

        when: 'token in start - only t1 enabled'
        marking.addToken(pStart, Token.of())
        def enabledOne = engine.getEnabledTransitions(net, marking, registry, context)

        then:
        enabledOne.size() == 1
        enabledOne[0] == t1

        when: 'fire t1, token moves to mid - only t2 enabled'
        engine.fire(net, t1, marking, registry, context)
        def enabledTwo = engine.getEnabledTransitions(net, marking, registry, context)

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
        !engine.isEnabled(net, tConsume, marking, registry, new ExecutionContext())
        !engine.fire(net, tConsume, marking, registry, new ExecutionContext())

        when: '3 tokens - enabled and fires'
        marking.addToken(pStart, Token.of([id: 3]))

        then:
        engine.isEnabled(net, tConsume, marking, registry, new ExecutionContext())

        when:
        def fired = engine.fire(net, tConsume, marking, registry, new ExecutionContext())

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
        def fired = engine.fire(net, tJoin, marking, registry, new ExecutionContext())

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
        engine.isEnabled(net, tGuarded, marking, registry, new ExecutionContext())
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
        def fired = engine.fire(net, tAction, marking, registry, new ExecutionContext())

        then:
        fired
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
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
        engine.fire(net, tJoin, marking, registry, new ExecutionContext())

        then:
        marking.getTokenCount(pEnd) == 1
        def result = marking.getTokens(pEnd)[0]
        result.payload.order == 3
        result.payload.common == 'c'
    }

    def 'should implement PetriNetEngine interface'() {
        expect:
        engine instanceof PetriNetEngine
    }

    def 'should return false for isEnabled with null transition'() {
        given:
        def net = simpleLinearNet()
        def marking = new Marking(net.places.size())
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()

        expect:
        !engine.isEnabled(net, null, marking, registry, context)
    }

    def 'should return false for fire with null transition'() {
        given:
        def net = simpleLinearNet()
        def marking = new Marking(net.places.size())
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()

        expect:
        !engine.fire(net, null, marking, registry, context)
    }
}
