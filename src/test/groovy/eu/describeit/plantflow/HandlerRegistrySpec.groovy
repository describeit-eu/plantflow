package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

class HandlerRegistrySpec extends Specification {

    def 'should register and execute action handler with context and token'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([count: 0])
        def token = Token.of([item: 'book'])

        registry.registerAction('increment count') { ExecutionContext ctx, Token tok ->
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

    def 'should register and execute action handler using ActionHandler SAM interface'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([count: 5])
        def token = Token.of([item: 'pen'])
        ActionHandler actionHandler = new ActionHandler() {
            @Override
            Object execute(ExecutionContext ctx, Token tok) {
                ctx['count'] = (ctx['count'] as int) * 2
                return tok.withPayload([item: 'pen', doubled: true])
            }
        }

        when:
        def result = registry.registerAction('double count', actionHandler)
        def handler = registry.getAction('double count')
        def resultToken = handler.execute(context, token)

        then:
        result.is(registry)
        context['count'] == 10
        resultToken.payload == [item: 'pen', doubled: true]
    }

    def 'should register and evaluate guard predicate with context and token'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([approved: true])
        def token = Token.of([amount: 50])

        registry.registerGuard('isApproved') { ExecutionContext ctx, Token tok ->
            return ctx['approved'] == true && (tok.payload.amount as int) < 100
        }

        when:
        def guard = registry.getGuard('isApproved')

        then:
        guard.evaluate(context, token)
        !guard.evaluate(new ExecutionContext([approved: false]), token)
    }

    def 'should register and evaluate guard predicate using GuardPredicate SAM interface'() {
        given:
        def registry = new HandlerRegistry()
        def context = new ExecutionContext([threshold: 10])
        def token = Token.of([value: 20])
        GuardPredicate guardPredicate = new GuardPredicate() {
            @Override
            boolean evaluate(ExecutionContext ctx, Token tok) {
                return (tok.payload.value as int) >= (ctx['threshold'] as int)
            }
        }

        when:
        def result = registry.registerGuard('exceedsThreshold', guardPredicate)
        def guard = registry.getGuard('exceedsThreshold')

        then:
        result.is(registry)
        guard.evaluate(context, token)
        !guard.evaluate(context, Token.of([value: 5]))
    }

    def 'should reject registration with invalid arguments: #scenario'() {
        given:
        def registry = new HandlerRegistry()

        when:
        operation(registry)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                         | operation                                                                                                         | expectedMessage
        'null label with ActionHandler'  | { HandlerRegistry r -> r.registerAction(null, ({ c, t -> t } as ActionHandler)) }    | 'Action label cannot be null'
        'null ActionHandler'             | { HandlerRegistry r -> r.registerAction('act', (ActionHandler) null) }                                            | 'Action handler cannot be null'
        'null label with action closure' | { HandlerRegistry r -> r.registerAction(null, { c, t -> t }) }                       | 'Action label cannot be null'
        'null action closure'            | { HandlerRegistry r -> r.registerAction('act', (Closure) null) }                                                  | 'Action handler cannot be null'
        'null label with GuardPredicate' | { HandlerRegistry r -> r.registerGuard(null, ({ c, t -> true } as GuardPredicate)) } | 'Guard label cannot be null'
        'null GuardPredicate'            | { HandlerRegistry r -> r.registerGuard('grd', (GuardPredicate) null) }                                            | 'Guard predicate cannot be null'
        'null label with guard closure'  | { HandlerRegistry r -> r.registerGuard(null, { c, t -> true }) }                     | 'Guard label cannot be null'
        'null guard closure'             | { HandlerRegistry r -> r.registerGuard('grd', (Closure) null) }                                                   | 'Guard predicate cannot be null'
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

    def 'should create UnregisteredHandlerException with message and optional cause: #scenario'() {
        when:
        def ex = cause == null ? new UnregisteredHandlerException(message) : new UnregisteredHandlerException(message, cause)

        then:
        ex.message == message
        ex.cause == cause

        where:
        scenario                  | message               | cause
        'message only'            | 'Missing handler: x'  | null
        'message with root cause' | 'Failed to find: y'   | new IllegalStateException('root cause')
    }
}
