package eu.describeit.plantflow

import eu.describeit.plantflow.engine.*
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Stateful execution tests for PlantFlow.
 * Tests the stateful API: runUntilEnd, step, seedToken, fire, isEnabled, isCompleted.
 * Consolidated from: PlantFlowExecutionSpec, PlantFlowSpec, PlantFlowErrorHandlingSpec,
 * PlantFlowConstructorSpec, PlantFlowParsingSpec, ForkEndMergePumlExecutionSpec.
 */
class PlantFlowExecutionSpec extends Specification {

    // ==================== Constructor Tests (from PlantFlowConstructorSpec) ====================

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

    // ==================== End-to-End Integration Tests (from PlantFlowSpec) ====================

    def 'should execute end-to-end workflow from PlantUML string'() {
        given:
        def puml = '''
            @startuml
            start
            :process order;
            :validate order;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('process order') { ExecutionContext ctx, Token tok ->
            ctx['status'] = 'PROCESSED'
            return tok.withPayload(tok.payload + [processed: true])
        }
        registry.registerAction('validate order') { ExecutionContext ctx, Token tok ->
            ctx['valid'] = true
            return tok.withPayload(tok.payload + [validated: true])
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([orderId: 'ORD-123']))

        then:
        engine.isCompleted()
        engine.getEndToken() != null
        engine.getEndToken().payload == [orderId: 'ORD-123', processed: true, validated: true]
        engine.executionContext['status'] == 'PROCESSED'
        engine.executionContext['valid'] == true
    }

    def 'should execute end-to-end workflow from file'() {
        given:
        def file = new File('src/test/data/puml/sequence.puml')
        def registry = new HandlerRegistry()
        registry.registerAction('Hello world') { ExecutionContext ctx, Token tok ->
            return tok.withPayload([greeting: 'Hello, World!'])
        }
        registry.registerAction('groovy goodness') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [goodness: true])
        }

        def engine = PlantFlow.from(file, registry)

        when:
        engine.runUntilEnd(Token.of())

        then:
        engine.isCompleted()
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload == [greeting: 'Hello, World!', goodness: true]
    }

    def 'should execute end-to-end conditional workflow from file'() {
        given:
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == true
        }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == false
        }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'then branch'
            return tok.withPayload(tok.payload + [branch: 'then'])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'else branch'
            return tok.withPayload(tok.payload + [branch: 'else'])
        }

        and: 'context with guard evaluating to true'
        def context = new ExecutionContext([actions: ['process all': true]])

        def engine = new PlantFlow(file, registry, context)

        when:
        engine.runUntilEnd(Token.of())

        then:
        engine.isCompleted()
        engine.executionContext['result'] == 'then branch'
        engine.getEndToken().payload['branch'] == 'then'
    }

    // ==================== Parsing + Execution Tests (from PlantFlowParsingSpec) ====================

    def 'should execute if-then-else workflow with guard evaluating to true'() {
        given: 'a conditional activity diagram'
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')

        and: 'a handler registry with registered guard and actions'
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == true
        }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == false
        }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'then branch executed'
            return tok.withPayload(tok.payload + [branch: 'then'])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'else branch executed'
            return tok.withPayload(tok.payload + [branch: 'else'])
        }

        and: 'an execution context where guard evaluates to true'
        def context = new ExecutionContext([actions: ['process all': true]])

        when: 'the workflow engine is instantiated and executed'
        def engine = new PlantFlow(file, registry, context)
        def finalEngine = engine.runUntilEnd()

        then: 'the workflow completes successfully'
        finalEngine == engine
        engine.isCompleted()

        and: 'the then branch was executed'
        context['result'] == 'then branch executed'

        and: 'the token in P_end has the branch information'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload['branch'] == 'then'
    }

    def 'should execute if-then-else workflow with guard evaluating to false'() {
        given: 'a conditional activity diagram'
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')

        and: 'a handler registry with registered guard and actions'
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == true
        }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok ->
            return ctx['actions']?['process all'] == false
        }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'then branch executed'
            return tok.withPayload(tok.payload + [branch: 'then'])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            ctx['result'] = 'else branch executed'
            return tok.withPayload(tok.payload + [branch: 'else'])
        }

        and: 'an execution context where guard evaluates to false'
        def context = new ExecutionContext([actions: ['process all': false]])

        when: 'the workflow engine is instantiated and executed'
        def engine = new PlantFlow(file, registry, context)
        def finalEngine = engine.runUntilEnd()

        then: 'the workflow completes successfully'
        finalEngine == engine
        engine.isCompleted()

        and: 'the else branch was executed'
        context['result'] == 'else branch executed'

        and: 'the token in P_end has the branch information'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload['branch'] == 'else'
    }

    // ==================== Stateful Execution Tests (from PlantFlowExecutionSpec) ====================

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

    def 'should deterministically fire first enabled transition in matrix order when multiple are enabled'() {
        given: 'a workflow with sequential steps'
        def puml = '''
            @startuml
            start
            :step one;
            :step two;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        def context = new ExecutionContext()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok ->
            ctx['order'] = (ctx['order'] ?: []) + ['one']
            return tok
        }
        registry.registerAction('step two') { ExecutionContext ctx, Token tok ->
            ctx['order'] = (ctx['order'] ?: []) + ['two']
            return tok
        }

        def engine = new PlantFlow(puml, registry, context)

        when: 'running the workflow'
        engine.runUntilEnd(Token.of([init: true]))

        then: 'the workflow completes in order'
        engine.isCompleted()
        context['order'] == ['one', 'two']
    }

    def 'should deterministically fire first transition when multiple are enabled'() {
        given: 'a workflow with sequential steps where both are enabled'
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
            ctx['executed'] = 'step one'
            // Return token to same place to enable both transitions
            return tok
        }
        registry.registerAction('step two') { ExecutionContext ctx, Token tok ->
            ctx['executed'] = 'step two'
            return tok
        }

        def context = new ExecutionContext()
        def engine = new PlantFlow(puml, registry, context)
        engine.seedToken(Token.of())

        when: 'firing first transition'
        def firstTransition = engine.petriNet.transitions[0]
        engine.fire(firstTransition)

        then: 'first transition was fired'
        context['executed'] == 'step one'

        when: 'checking enabled transitions after first fire'
        def enabled = engine.getEnabledTransitions()

        then: 'at least one transition is enabled'
        enabled.size() >= 1

        when: 'firing from enabled transitions list'
        engine.fire(enabled[0])

        then: 'a transition was fired'
        enabled.size() >= 1
    }

    // ==================== Error Handling Tests (from PlantFlowErrorHandlingSpec) ====================

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

    def 'should throw UnregisteredHandlerException when evaluating isEnabled for unregistered guard handler'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())

        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)

        def transition = engine.petriNet.transitions.find { it.guardKey == 'unregistered guard' }

        when: 'checking if the transition is enabled'
        engine.isEnabled(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when evaluating getEnabledTransitions with unregistered guard handler'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())

        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)

        when: 'querying enabled transitions'
        engine.getEnabledTransitions()

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when firing transition with unregistered guard'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())

        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)

        def transition = engine.petriNet.transitions.find { it.guardKey == 'unregistered guard' }

        when: 'firing the transition'
        engine.fire(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when firing transition with unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered action;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())
        def transition = engine.petriNet.transitions[0]

        when: 'firing the transition'
        engine.fire(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered action')
    }

    def 'should fail fast with UnregisteredHandlerException during runUntilEnd with unregistered action'() {
        given: 'a workflow with an unregistered action step'
        def puml = '''
            @startuml
            start
            :step one;
            :unregistered step;
            :step three;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('step three') { ExecutionContext ctx, Token tok -> tok }
        // step two is NOT registered

        def engine = PlantFlow.from(puml, registry)

        when: 'running until end'
        engine.runUntilEnd(Token.of([init: true]))

        then: 'it immediately raises UnregisteredHandlerException without completing'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
        !engine.isCompleted()
    }

    def 'should fail fast with UnregisteredHandlerException during runUntilEnd with unregistered guard'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            :step one;
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            :step three;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('step three') { ExecutionContext ctx, Token tok -> tok }
        // guard is NOT registered

        def engine = PlantFlow.from(puml, registry)

        when: 'running until end'
        engine.runUntilEnd(Token.of([init: true]))

        then: 'it immediately raises UnregisteredHandlerException without completing'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
        !engine.isCompleted()
    }

    // ==================== Fork Execution Tests (from ForkEndMergePumlExecutionSpec) ====================

    def 'should execute forkEndMerge.puml with runUntilEnd and complete successfully'() {
        given:
        def file = new File('src/test/data/puml/forkEndMerge.puml')
        def executionLog = []
        def actions = ['action 1', 'action 2', 'action 3', 'action 4', 'action 5']
        def registry = payloadRegistry(actions, executionLog)

        def engine = PlantFlow.from(file, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()
        engine.petriNet.isEmpty(engine.petriNet.startPlace)
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        and:
        executionLog.size() == 5
        executionLog.containsAll(actions)
    }

    def 'should execute forkEndMerge.puml step by step and verify intermediate states'() {
        given:
        def file = new File('src/test/data/puml/forkEndMerge.puml')
        def actions = ['action 1', 'action 2', 'action 3', 'action 4', 'action 5']
        def registry = payloadRegistry(actions)

        def engine = PlantFlow.from(file, registry)
        engine.seedToken(Token.of([initial: true]))

        when: 'step 1: fire fork transition'
        def stepped1 = engine.step()

        then:
        stepped1
        engine.petriNet.isEmpty(engine.petriNet.startPlace)

        and: 'tokens are in all three branch start places'
        def branch1Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_1' }
        def branch2Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_2' }
        def branch3Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3' }
        engine.petriNet.getTokenCount(branch1Place) == 1
        engine.petriNet.getTokenCount(branch2Place) == 1
        engine.petriNet.getTokenCount(branch3Place) == 1

        when: 'step 2: fire action 1 (branch 1)'
        engine.step()

        then: 'branch 1 end place has token'
        def branch1EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_1_end' }
        engine.petriNet.getTokenCount(branch1EndPlace) == 1

        when: 'step 3: fire action 2 (branch 2)'
        engine.step()

        then: 'branch 2 end place has token'
        def branch2EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_2_end' }
        engine.petriNet.getTokenCount(branch2EndPlace) == 1

        when: 'step 4: fire action 3 (branch 3, first action)'
        engine.step()

        then: 'branch 3 intermediate place has token'
        def branch3Mid = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3_1' }
        engine.petriNet.getTokenCount(branch3Mid) == 1

        when: 'step 5: fire action 4 (branch 3, second action)'
        engine.step()

        then: 'branch 3 end place has token'
        def branch3EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3_end' }
        engine.petriNet.getTokenCount(branch3EndPlace) == 1

        when: 'step 6: fire merge sync transition'
        engine.step()

        then: 'merge place has token'
        def mergePlace = engine.petriNet.places.find { it.label == 'P_fork_1_merge' }
        engine.petriNet.getTokenCount(mergePlace) == 1

        when: 'step 7: fire action 5 (post-merge)'
        def stepped7 = engine.step()

        then:
        stepped7
        engine.isCompleted()
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        when: 'step 8: no more transitions enabled'
        def stepped8 = engine.step()

        then:
        !stepped8
    }

    def 'should deep-merge token payloads from all branches at merge point'() {
        given: 'a fork with two branches that produce different payload keys'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [fromA: 'value-a', shared: 'from-a'])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [fromB: 'value-b', shared: 'from-b'])
        }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and: 'the end token has merged payload from both branches'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        endToken.payload.fromA == 'value-a'
        endToken.payload.fromB == 'value-b'
        endToken.payload.shared == 'from-b'
    }

    def 'should deep-merge nested map payloads from branches at merge point'() {
        given: 'a fork with branches that produce nested map payloads'
        def puml = forkPuml([['branch a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [
                config: [host: 'localhost', port: 8080],
                branchA: true
            ])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [
                config: [port: 9090, ssl: true],
                branchB: true
            ])
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and: 'nested maps are deep-merged correctly'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        endToken.payload.branchA == true
        endToken.payload.branchB == true

        and: 'nested config map is merged with overlay winning on conflicts'
        def config = endToken.payload.config as Map
        config.host == 'localhost'
        config.port == 9090
        config.ssl == true
    }

    def 'should execute fork with three branches and verify all run in parallel'() {
        given:
        def puml = forkPuml([['task 1'], ['task 2'], ['task 3']], ['finalize'])

        def executionOrder = []
        def registry = payloadRegistry(['task 1', 'task 2', 'task 3', 'finalize'], executionOrder)

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of())

        then:
        engine.isCompleted()

        and: 'all three tasks executed before finalize'
        executionOrder.size() == 4
        executionOrder.containsAll(['task 1', 'task 2', 'task 3'])
        executionOrder.last() == 'finalize'

        and: 'end token has all task payloads merged'
        def endToken = engine.getEndToken()
        endToken.payload.task_1 == true
        endToken.payload.task_2 == true
        endToken.payload.task_3 == true
    }

    def 'should execute fork with multi-action branches and verify sequential execution within branches'() {
        given:
        def puml = forkPuml([['a1', 'a2'], ['b1', 'b2', 'b3']])

        def executionLog = []
        def registry = payloadRegistry(['a1', 'a2', 'b1', 'b2', 'b3'], executionLog)

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([start: true]))

        then:
        engine.isCompleted()

        and: 'all actions executed'
        executionLog.size() == 5
        executionLog.containsAll(['a1', 'a2', 'b1', 'b2', 'b3'])

        and: 'end token has all payloads merged'
        def endToken = engine.getEndToken()
        endToken.payload.start == true
        endToken.payload.a1 == true
        endToken.payload.a2 == true
        endToken.payload.b1 == true
        endToken.payload.b2 == true
        endToken.payload.b3 == true
    }

    def 'should preserve token id through fork and merge execution'() {
        given:
        def puml = forkPuml([['branch a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        def seedToken = Token.of([original: true])

        when:
        engine.runUntilEnd(seedToken)

        then:
        engine.isCompleted()

        and:
        def endToken = engine.getEndToken()
        endToken != null
        endToken.id == seedToken.id
        endToken.payload.original == true
    }

    @Unroll
    def 'should execute fork with #branchCount branches and verify completion'() {
        given:
        def branches = (1..branchCount).collect { ["action ${it}"] }
        def puml = forkPuml(branches)

        def registry = payloadRegistry(branches.flatten())

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and:
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        (1..branchCount).each { i ->
            assert endToken.payload["action_${i}".toString()] == true
        }

        where:
        branchCount << [2, 3, 5]
    }

    def 'should execute nested fork structure correctly'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
              fork
                :nested action 1;
              fork again
                :nested action 2;
              end merge
            fork again
              :action 2;
            end merge
            end
            @enduml
        '''

        def executionLog = []
        def actions = ['action 1', 'action 2', 'nested action 1', 'nested action 2']
        def registry = payloadRegistry(actions, executionLog)

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and:
        executionLog.size() == 4
        executionLog.containsAll(actions)

        and:
        def endToken = engine.getEndToken()
        endToken.payload.initial == true
        endToken.payload.action_1 == true
        endToken.payload.action_2 == true
        endToken.payload.nested_action_1 == true
        endToken.payload.nested_action_2 == true
    }

    def 'should update execution context from parallel branch actions'() {
        given:
        def puml = forkPuml([['branch a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            ctx['branchAExecuted'] = true
            return tok.withPayload(tok.payload + [a: true])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            ctx['branchBExecuted'] = true
            return tok.withPayload(tok.payload + [b: true])
        }

        def context = new ExecutionContext()
        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of(), context)

        then:
        engine.isCompleted()
        context['branchAExecuted'] == true
        context['branchBExecuted'] == true
    }

    def 'regression: supported puml files should parse and compile without errors'() {
        given: 'puml files that the parser supports (linear, if-then-else-endif, fork-end merge)'
        def pumlFiles = [
            'sequence.puml',
            'ifThenElseEndif.puml',
            'forkEndMerge.puml'
        ]

        expect:
        pumlFiles.each { filename ->
            def file = new File("src/test/data/puml/${filename}")
            def net = new ActivityDiagramParser().parse(file)
            assert net != null : "Failed to parse ${filename}"
            assert net.places.size() > 0 : "No places in ${filename}"
            assert net.transitions.size() > 0 : "No transitions in ${filename}"
            assert net.startPlace != null : "No start place in ${filename}"
            assert net.endPlace != null : "No end place in ${filename}"
        }
    }

    // ==================== Helper Classes ====================

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
