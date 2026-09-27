package eu.describeit.plantflow


import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

/**
 * Integration tests for PlantFlow that verify end-to-end workflow execution
 * across multiple components (parsing, compilation, engine, handlers).
 */
class PlantFlowSpec extends Specification {

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

}
