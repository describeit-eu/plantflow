package eu.describeit.plantflow


import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

class PlantFlowParsingSpec extends Specification {

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

}
