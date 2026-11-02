package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class StatefulExecutionSpec extends Specification {

    def engine = new DefaultPetriNetEngine()

    def 'should create StatefulExecution with structure and engine'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'action', null)

        when:
        def execution = new StatefulExecution(net, engine)

        then:
        execution.structure.is(net)
        execution.engine.is(engine)
        execution.marking != null
        execution.isNetEmpty()
    }

    def 'should expose topology from structure'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'action', null)
        def execution = new StatefulExecution(net, engine)

        expect:
        execution.places == net.places
        execution.transitions == net.transitions
        execution.startPlace == net.startPlace
        execution.endPlace == net.endPlace
        execution.incidenceMatrix.is(net.incidenceMatrix)
        execution.getPlaceByIndex(0) == net.startPlace
        execution.getTransitionByIndex(0) == net.transitions[0]
    }

    def 'should seed token with default token when null'() {
        given:
        def net = simpleLinearNet()
        def execution = new StatefulExecution(net, engine)

        when:
        execution.seedToken(null)

        then:
        execution.getTokenCount(net.startPlace) == 1
        execution.getTokens(net.startPlace)[0].payload == [:]
    }

    def 'should seed token with explicit token'() {
        given:
        def net = simpleLinearNet()
        def execution = new StatefulExecution(net, engine)
        def token = Token.of([seeded: true])

        when:
        execution.seedToken(token)

        then:
        execution.getTokenCount(net.startPlace) == 1
        execution.getTokens(net.startPlace)[0] == token
    }

    def 'should delegate isEnabled to engine with internal marking'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)
        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry().registerAction('myAction') { ctx, tok -> tok }
        def context = new ExecutionContext()
        def transition = net.transitions[0]

        expect: 'not enabled without token'
        !execution.isEnabled(transition, registry, context)

        when: 'token seeded'
        execution.seedToken()

        then: 'enabled with token'
        execution.isEnabled(transition, registry, context)
    }

    def 'should delegate getEnabledTransitions to engine with internal marking'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)
        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry().registerAction('myAction') { ctx, tok -> tok }
        def context = new ExecutionContext()

        expect: 'no enabled transitions without token'
        execution.getEnabledTransitions(registry, context).isEmpty()

        when: 'token seeded'
        execution.seedToken()

        then: 'one enabled transition'
        execution.getEnabledTransitions(registry, context).size() == 1
    }

    def 'should fire transition through engine with internal marking'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)
        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry().registerAction('myAction') { ctx, tok -> tok.withPayload(tok.payload + [done: true]) }
        def context = new ExecutionContext()
        def transition = net.transitions[0]

        when: 'firing without token'
        def firedWithout = execution.fire(transition, registry, context)

        then:
        !firedWithout

        when: 'seeding and firing'
        execution.seedToken(Token.of([init: true]))
        def fired = execution.fire(transition, registry, context)

        then:
        fired
        execution.isEmpty(net.startPlace)
        execution.getTokenCount(net.endPlace) == 1
        execution.getTokens(net.endPlace)[0].payload == [init: true, done: true]
    }

    def 'should step through workflow'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def t1 = new Transition(0, 'step1', 'action1', null)
        def t2 = new Transition(1, 'step2', 'action2', null)

        def inputMatrix = [[1, 0], [0, 1], [0, 0]] as int[][]
        def outputMatrix = [[0, 0], [1, 0], [0, 1]] as int[][]
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], matrix, pStart, pEnd)

        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ctx, tok -> tok }
        registry.registerAction('action2') { ctx, tok -> tok }
        def context = new ExecutionContext()

        when: 'stepping without token'
        def step0 = execution.step(registry, context)

        then:
        !step0

        when: 'seeding and stepping through'
        execution.seedToken()
        def step1 = execution.step(registry, context)

        then:
        step1
        execution.isEmpty(pStart)
        execution.getTokenCount(pMid) == 1

        when:
        def step2 = execution.step(registry, context)

        then:
        step2
        execution.isEmpty(pMid)
        execution.getTokenCount(pEnd) == 1

        when: 'no more enabled transitions'
        def step3 = execution.step(registry, context)

        then:
        !step3
    }

    def 'should run until end with explicit token'() {
        given:
        def pStart = new Place(0, 'start')
        def pMid = new Place(1, 'mid')
        def pEnd = new Place(2, 'end')
        def t1 = new Transition(0, 'step1', 'action1', null)
        def t2 = new Transition(1, 'step2', 'action2', null)

        def inputMatrix = [[1, 0], [0, 1], [0, 0]] as int[][]
        def outputMatrix = [[0, 0], [1, 0], [0, 1]] as int[][]
        def matrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], matrix, pStart, pEnd)

        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ctx, tok -> tok.withPayload(tok.payload + [s1: true]) }
        registry.registerAction('action2') { ctx, tok -> tok.withPayload(tok.payload + [s2: true]) }
        def context = new ExecutionContext()

        when:
        def seedToken = Token.of([init: true])
        def finalMarking = execution.runUntilEnd(registry, context, seedToken)

        then:
        finalMarking.isEmpty(pStart)
        finalMarking.isEmpty(pMid)
        !finalMarking.isEmpty(pEnd)
        finalMarking.getTokens(pEnd)[0].payload == [init: true, s1: true, s2: true]
    }

    def 'should auto-seed token when runUntilEnd is called on empty net with null token'() {
        given:
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)
        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry().registerAction('myAction') { ctx, tok -> tok }
        def context = new ExecutionContext()

        when:
        execution.runUntilEnd(registry, context, null)

        then:
        execution.getTokenCount(net.endPlace) == 1
    }

    def 'should not auto-seed token when runUntilEnd is called with null token on non-empty net'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def matrix = new IncidenceMatrix(null, null)
        def net = new DefaultPetriNet([pStart, pEnd], [], matrix, pStart, pEnd)

        def execution = new StatefulExecution(net, engine)
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()

        and: 'token in end place'
        execution.marking.addToken(pEnd, Token.of([existing: true]))

        when:
        execution.runUntilEnd(registry, context, null)

        then: 'no token added to start place'
        execution.isEmpty(pStart)
        execution.getTokenCount(pEnd) == 1
    }

    def 'should report isNetEmpty correctly'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def matrix = new IncidenceMatrix(null, null)
        def net = new DefaultPetriNet([pStart, pEnd], [], matrix, pStart, pEnd)
        def execution = new StatefulExecution(net, engine)

        expect: 'initially empty'
        execution.isNetEmpty()

        when: 'token added'
        execution.addToken(pStart, Token.of())

        then: 'not empty'
        !execution.isNetEmpty()

        when: 'token removed'
        execution.marking.removeToken(pStart, execution.getTokens(pStart)[0])

        then: 'empty again'
        execution.isNetEmpty()
    }

    def 'should manage tokens via marking operations'() {
        given:
        def net = simpleLinearNet()
        def execution = new StatefulExecution(net, engine)
        def pStart = net.startPlace

        expect: 'initially empty'
        execution.isEmpty(pStart)
        execution.isEmpty(0)
        execution.getTokenCount(pStart) == 0
        execution.getTokenCount(0) == 0
        execution.getTokens(pStart).isEmpty()

        when: 'add token'
        def token = Token.of([key: 'val'])
        execution.addToken(pStart, token)

        then:
        !execution.isEmpty(pStart)
        !execution.isEmpty(0)
        execution.getTokenCount(pStart) == 1
        execution.getTokenCount(0) == 1
        execution.getTokens(pStart) == [token]
    }

    def 'should work with custom engine implementation'() {
        given: 'a mock engine that tracks calls'
        def net = simpleLinearNet('start', 'end', 'action', 'myAction', null)
        def trackingEngine = new TrackingEngine()
        def execution = new StatefulExecution(net, trackingEngine)
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        def transition = net.transitions[0]

        when:
        execution.seedToken()
        execution.isEnabled(transition, registry, context)
        execution.getEnabledTransitions(registry, context)
        execution.fire(transition, registry, context)
        execution.step(registry, context)

        then: 'all calls delegated to engine'
        trackingEngine.isEnabledCalls == 1
        trackingEngine.getEnabledCalls == 2
        trackingEngine.fireCalls == 2
    }

    static class TrackingEngine implements PetriNetEngine {
        int isEnabledCalls = 0
        int getEnabledCalls = 0
        int fireCalls = 0

        @Override
        boolean isEnabled(PetriNetStructure net, Transition t, Marking m, HandlerRegistry r, ExecutionContext c) {
            isEnabledCalls++
            return true
        }

        @Override
        List<Transition> getEnabledTransitions(PetriNetStructure net, Marking m, HandlerRegistry r, ExecutionContext c) {
            getEnabledCalls++
            return net.transitions
        }

        @Override
        boolean fire(PetriNetStructure net, Transition t, Marking m, HandlerRegistry r, ExecutionContext c) {
            fireCalls++
            return true
        }
    }
}
