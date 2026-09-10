package eu.describeit.plantflow

import eu.describeit.plantflow.engine.DefaultPetriNet
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition
import spock.lang.Specification
import java.time.Instant

class PlantFlowSpec extends Specification {

    def 'should execute straight-line activity workflow tracer bullet'() {
        given: 'a linear activity diagram'
        def puml = '''
            @startuml
            start
            :process order;
            end
            @enduml
        '''

        and: 'a handler registry with registered action'
        def registry = new HandlerRegistry()
        registry.registerAction('process order') { ExecutionContext ctx, Token tok ->
            ctx['status'] = 'PROCESSED'
            def currentTotal = (tok.payload['total'] as double)
            return tok.withPayload([orderId: tok.payload['orderId'], total: currentTotal, processed: true])
        }

        and: 'an initial seed token and context'
        def initialTimestamp = Instant.parse('2026-08-24T10:00:00Z')
        def seedToken = new Token('order-tok-1', initialTimestamp, [orderId: 'ORD-999', total: 150.0])
        def context = new ExecutionContext([initiator: 'tester'])

        when: 'the workflow engine is instantiated and executed'
        def engine = PlantFlow.from(puml, registry)
        def finalEngine = engine.runUntilEnd(seedToken, context)

        then: 'the start place is empty and the end place contains the transformed token'
        finalEngine == engine
        engine.petriNet.isEmpty(engine.petriNet.startPlace)
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        and: 'the token in P_end has the same ID and updated payload'
        def endTokens = engine.getEndTokens()
        endTokens.size() == 1
        def resultToken = endTokens[0]
        resultToken.id == 'order-tok-1'
        resultToken.payload == [orderId: 'ORD-999', total: 150.0, processed: true]

        and: 'the execution context was updated by the action handler'
        context['status'] == 'PROCESSED'
        context['initiator'] == 'tester'
        engine.isCompleted()
        engine.getEndToken() == resultToken
    }

    def 'should advance multi-step sequential workflow through all intermediate places'() {
        given:
        def puml = '''
            @startuml
            start
            :validate customer;
            :charge credit card;
            end
            @enduml
        '''

        def executionLog = []
        def registry = new HandlerRegistry()
        registry.registerAction('validate customer') { ExecutionContext ctx, Token tok ->
            executionLog.add('validated')
            ctx['customerValid'] = true
            return tok.withPayload(tok.payload + [customerValid: true])
        }
        registry.registerAction('charge credit card') { ExecutionContext ctx, Token tok ->
            executionLog.add('charged')
            ctx['charged'] = true
            return tok.withPayload(tok.payload + [chargedAmount: 200])
        }

        def engine = PlantFlow.from(puml, registry)
        def seedToken = Token.of([customerId: 'CUST-1'])

        when:
        def finalEngine = engine.runUntilEnd(seedToken)

        then:
        finalEngine == engine
        executionLog == ['validated', 'charged']
        engine.petriNet.isEmpty(engine.petriNet.startPlace)
        engine.petriNet.isEmpty(1)
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        and:
        def endToken = engine.getEndToken()
        endToken.payload == [customerId: 'CUST-1', customerValid: true, chargedAmount: 200]
        engine.executionContext['customerValid'] == true
        engine.executionContext['charged'] == true
        engine.isCompleted()
    }

    def 'should execute workflow loaded directly from sequence.puml file'() {
        given:
        def file = new File('src/test/data/puml/sequence.puml')
        def calls = []
        def engine = PlantFlow.from(file)
            .registerAction('Hello world') { ExecutionContext ctx, Token tok ->
                calls.add('hello')
                return [greeting: 'Hello, World!']
            }
            .registerAction('groovy goodness') { ExecutionContext ctx, Token tok ->
                calls.add('groovy')
                return tok.payload + [goodness: true]
            }

        when:
        engine.runUntilEnd()

        then:
        calls == ['hello', 'groovy']
        engine.isCompleted()
        engine.getEndToken().payload == [greeting: 'Hello, World!', goodness: true]
    }

    def 'should throw UnregisteredHandlerException when action handler is missing during execution'() {
        given:
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())

        when:
        engine.runUntilEnd(Token.of([key: 'val']))

        then:
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should throw UnregisteredHandlerException when evaluating isEnabled for unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())
        def transition = engine.petriNet.transitions[0]

        when: 'checking if the transition is enabled'
        engine.isEnabled(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should throw UnregisteredHandlerException when evaluating getEnabledTransitions with unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())

        when: 'querying enabled transitions'
        engine.getEnabledTransitions()

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should execute step by step'() {
        given:
        def puml = '''
            @startuml
            start
            :step one;
            :step two;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok ->
            return tok.withPayload([step1: true])
        }
        registry.registerAction('step two') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [step2: true])
        }

        def engine = PlantFlow.from(puml, registry)
        def token = Token.of([init: true])
        engine.seedToken(token)

        expect:
        engine.petriNet.getTokenCount(engine.petriNet.startPlace) == 1
        engine.petriNet.getTokenCount(1) == 0
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 0

        when:
        boolean stepped1 = engine.step()

        then:
        stepped1
        engine.petriNet.getTokenCount(engine.petriNet.startPlace) == 0
        engine.petriNet.getTokenCount(1) == 1
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 0

        when:
        boolean stepped2 = engine.step()

        then:
        stepped2
        engine.petriNet.getTokenCount(engine.petriNet.startPlace) == 0
        engine.petriNet.getTokenCount(1) == 0
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 1

        when:
        boolean stepped3 = engine.step()

        then:
        !stepped3
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 1
        engine.isCompleted()
    }

    def 'should fallback to default token when seedToken is called with: #scenario'() {
        given:
        def puml = '''
            @startuml
            start
            :step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry().registerAction('step') { ctx, tok -> tok })

        when:
        engine.seedToken(token)

        then:
        def startTokens = engine.petriNet.getTokens(engine.petriNet.startPlace)
        startTokens.size() == 1
        if (token != null) {
            assert startTokens[0].id == token.id
        } else {
            assert startTokens[0].id != null
        }

        where:
        scenario        | token
        'null token'    | null
        'custom token'  | Token.of([foo: 'bar'])
    }

    def 'should verify transition readiness and step execution on blocked workflow'() {
        given:
        def puml = '''
            @startuml
            start
            :single step;
            end
            @enduml
        '''
        def registry = new HandlerRegistry().registerAction('single step') { ctx, tok -> tok }
        def engine = PlantFlow.from(puml, registry)
        def transition = engine.petriNet.transitions[0]

        expect: 'unseeded workflow transition is not enabled'
        !engine.isEnabled(transition)
        engine.getEndToken() == null
        !engine.isCompleted()

        and: 'stepping without tokens returns false'
        !engine.step()

        when: 'token is seeded'
        engine.seedToken(Token.of())

        then: 'transition becomes enabled'
        engine.isEnabled(transition)
        !engine.isCompleted()

        when: 'stepping enables execution'
        def stepped = engine.step()

        then:
        stepped
        !engine.isEnabled(transition)
        engine.isCompleted()
        engine.getEndToken() != null

        and: 'stepping after completion returns false'
        !engine.step()
    }

    def 'should accurately report completion status across workflow lifecycle states'() {
        given: 'a two-step sequential workflow'
        def puml = '''
            @startuml
            start
            :step one;
            :step two;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
            .registerAction('step one') { ctx, tok -> tok }
            .registerAction('step two') { ctx, tok -> tok }
        def engine = PlantFlow.from(puml, registry)

        expect: 'initial unseeded state is incomplete with no end token'
        !engine.isCompleted()
        engine.getEndToken() == null

        when: 'seeded but not started'
        engine.seedToken(Token.of())

        then: 'still incomplete with no end token'
        !engine.isCompleted()
        engine.getEndToken() == null

        when: 'step 1 executed (token in intermediate place)'
        engine.step()

        then: 'incomplete at intermediate place'
        !engine.isCompleted()
        engine.getEndToken() == null

        when: 'step 2 executed (token at end place)'
        engine.step()

        then: 'completed with end token present'
        engine.isCompleted()
        engine.getEndToken() != null
    }

    def 'should evaluate isCompleted as false when end place has tokens but transitions remain enabled'() {
        given: 'a petri net where end place has a token but another transition is still enabled'
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def t1 = new Transition(0, 'step', 'act', null)
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]
        def incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [t1], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry().registerAction('act') { ctx, tok -> tok }
        def engine = new PlantFlow(net, registry)

        when: 'adding token to end place while start place also has a token enabling T_0'
        engine.petriNet.addToken(pEnd, Token.of())
        engine.petriNet.addToken(pStart, Token.of())

        then: 'end place is not empty, but enabled transitions is not empty, so isCompleted is false'
        !engine.petriNet.isEmpty(pEnd)
        !engine.getEnabledTransitions().isEmpty()
        !engine.isCompleted()
        engine.getEndToken() != null
    }

    def 'should instantiate PlantFlow using various constructor and factory overloads: #scenario'() {
        expect:
        flowInstance.petriNet != null
        flowInstance.handlerRegistry != null
        flowInstance.executionContext != null

        where:
        scenario                        | flowInstance
        'PetriNet only'                 | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'))
        'PetriNet + registry'           | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry())
        'PetriNet + registry + context' | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'String only'                   | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml')
        'String + registry'             | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml', new HandlerRegistry())
        'String + registry + context'   | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml', new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'File only'                     | new PlantFlow(new File('src/test/data/puml/sequence.puml'))
        'File + registry'               | new PlantFlow(new File('src/test/data/puml/sequence.puml'), new HandlerRegistry())
        'File + registry + context'     | new PlantFlow(new File('src/test/data/puml/sequence.puml'), new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'from(File) overload'           | PlantFlow.from(new File('src/test/data/puml/sequence.puml'))
        'from(String) overload'         | PlantFlow.from('@startuml\nstart\n:a;\nend\n@enduml')
    }

    def 'should throw IllegalArgumentException when constructor receives null argument: #scenario'() {
        when:
        constructorCall.call()

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedParam)

        where:
        scenario                  | constructorCall                                                                                                                             | expectedParam
        'null PetriNet'           | { new PlantFlow((PetriNet) null) }                                                                                                          | 'petriNet'
        'null HandlerRegistry'    | { new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), (HandlerRegistry) null) }                         | 'handlerRegistry'
        'null ExecutionContext'   | { new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry(), (ExecutionContext) null) } | 'executionContext'
    }

    def 'should fire specific transition directly via fire(Transition)'() {
        given:
        def puml = '''
            @startuml
            start
            :direct fire;
            end
            @enduml
        '''
        def registry = new HandlerRegistry().registerAction('direct fire') { ctx, tok -> tok }
        def engine = PlantFlow.from(puml, registry)
        def transition = engine.petriNet.transitions[0]

        when: 'firing transition without tokens'
        def firedWithoutToken = engine.fire(transition)

        then:
        !firedWithoutToken

        when: 'firing transition with token'
        engine.seedToken(Token.of())
        def firedWithToken = engine.fire(transition)

        then:
        firedWithToken
        engine.isCompleted()
    }

    def 'should return false when step is invoked with no enabled transitions'() {
        given:
        def puml = '''
            @startuml
            start
            :step;
            end
            @enduml
        '''
        def registry = new HandlerRegistry().registerAction('step') { ctx, tok -> tok }
        def engine = PlantFlow.from(puml, registry)

        expect: 'no tokens in start place means no enabled transitions'
        !engine.step()
    }

    def 'should return false when step is invoked and fire fails'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def t1 = new Transition(0, 'step', 'act', null)
        def matrix = new IncidenceMatrix([[1], [0]] as int[][], [[0], [1]] as int[][])
        def net = new FailingFirePetriNet([pStart, pEnd], [t1], matrix, pStart, pEnd)
        def registry = new HandlerRegistry().registerAction('act') { ctx, tok -> tok }
        def engine = new PlantFlow(net, registry)
        engine.seedToken()

        expect:
        !engine.step()
    }

    static class FailingFirePetriNet extends DefaultPetriNet {
        FailingFirePetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix matrix, Place start, Place end) {
            super(places, transitions, matrix, start, end)
        }

        @Override
        boolean fire(Transition t, Marking m, HandlerRegistry r, ExecutionContext c) {
            return false
        }
    }
}
