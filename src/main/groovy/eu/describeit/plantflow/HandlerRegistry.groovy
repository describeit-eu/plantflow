package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import groovy.transform.CompileStatic

@CompileStatic
class HandlerRegistry {
    private static final String ACTION_LABEL_NULL_MSG = 'Action label cannot be null'
    private static final String ACTION_HANDLER_NULL_MSG = 'Action handler cannot be null'
    private static final String GUARD_LABEL_NULL_MSG = 'Guard label cannot be null'
    private static final String GUARD_PREDICATE_NULL_MSG = 'Guard predicate cannot be null'

    private final Map<String, ActionHandler> actions = [:]
    private final Map<String, GuardPredicate> guards = [:]

    HandlerRegistry registerAction(String label, ActionHandler handler) {
        if (label == null) throw new IllegalArgumentException(ACTION_LABEL_NULL_MSG)
        if (handler == null) throw new IllegalArgumentException(ACTION_HANDLER_NULL_MSG)
        actions.put(label, handler)
        return this
    }

    HandlerRegistry registerAction(String label, Closure handler) {
        if (label == null) throw new IllegalArgumentException(ACTION_LABEL_NULL_MSG)
        if (handler == null) throw new IllegalArgumentException(ACTION_HANDLER_NULL_MSG)
        actions.put(label, new ActionHandler() {
            @Override
            Object execute(ExecutionContext context, Token token) {
                return handler.call(context, token)
            }
        })
        return this
    }

    HandlerRegistry registerGuard(String label, GuardPredicate predicate) {
        if (label == null) throw new IllegalArgumentException(GUARD_LABEL_NULL_MSG)
        if (predicate == null) throw new IllegalArgumentException(GUARD_PREDICATE_NULL_MSG)
        guards.put(label, predicate)
        return this
    }

    HandlerRegistry registerGuard(String label, Closure predicate) {
        if (label == null) throw new IllegalArgumentException(GUARD_LABEL_NULL_MSG)
        if (predicate == null) throw new IllegalArgumentException(GUARD_PREDICATE_NULL_MSG)
        guards.put(label, new GuardPredicate() {
            @Override
            boolean evaluate(ExecutionContext context, Token token) {
                return predicate(context, token) as Boolean
            }
        })
        return this
    }
    
    ActionHandler getAction(String label) {
        ActionHandler handler = actions.get(label)
        if (handler == null) {
            throw new UnregisteredHandlerException("No action handler registered for: '$label'")
        }
        return handler
    }

    GuardPredicate getGuard(String label) {
        GuardPredicate predicate = guards.get(label)
        if (predicate == null) {
            throw new UnregisteredHandlerException("No guard predicate registered for: '$label'")
        }
        return predicate
    }
}
