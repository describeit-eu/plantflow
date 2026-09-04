package eu.describeit.plantflow

import spock.lang.Specification

class HandlerRegistrySpec extends Specification {

    def 'should register and execute action handler with context and token'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([count: 0])
        def token = RecordToken.of([item: 'book'])

        registry.registerAction('increment count') { ExecutionContext ctx, RecordToken tok ->
            ctx['count'] = (ctx['count'] as int) + 1
            return tok.withPayload([item: 'book', processed: true])
        }

        when:
        def handler = registry.getAction('increment count')
        def resultToken = handler.execute(context, token)

        then:
        context['count'] == 1
        resultToken.payload == [item: 'book', processed: true]
    }

    def 'should register and evaluate guard predicate with context and token'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([approved: true])
        def token = RecordToken.of([amount: 50])

        registry.registerGuard('isApproved') { ExecutionContext ctx, RecordToken tok ->
            return ctx['approved'] == true && (tok.payload.amount as int) < 100
        }

        when:
        def guard = registry.getGuard('isApproved')

        then:
        guard.evaluate(context, token)
        !guard.evaluate(new ExecutionContext([approved: false]), token)
    }

    def 'should throw UnregisteredHandlerException when action handler is missing'() {
        given:
        def registry = new HandlerRegistry()

        when:
        registry.getAction('unknown action')

        then:
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unknown action')
    }

    def 'should throw UnregisteredHandlerException when guard predicate is missing'() {
        given:
        def registry = new HandlerRegistry()

        when:
        registry.getGuard('unknown guard')

        then:
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unknown guard')
    }
}
