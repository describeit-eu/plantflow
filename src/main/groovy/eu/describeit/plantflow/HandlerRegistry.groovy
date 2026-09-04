package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
class HandlerRegistry {
    private final Map<String, ActionHandler> actions = [:]
    private final Map<String, GuardPredicate> guards = [:]

    HandlerRegistry registerAction(String label, ActionHandler handler) {
        if (label == null) throw new IllegalArgumentException('Action label cannot be null')
        if (handler == null) throw new IllegalArgumentException('Action handler cannot be null')
        actions.put(label, handler)
        return this
    }

    HandlerRegistry registerAction(String label, Closure handler) {
        if (label == null) throw new IllegalArgumentException('Action label cannot be null')
        if (handler == null) throw new IllegalArgumentException('Action handler cannot be null')
        actions.put(label, new ActionHandler() {
            @Override
            Object execute(ExecutionContext context, RecordToken token) {
                return handler.call(context, token)
            }
        })
        return this
    }

    HandlerRegistry registerGuard(String label, GuardPredicate predicate) {
        if (label == null) throw new IllegalArgumentException('Guard label cannot be null')
        if (predicate == null) throw new IllegalArgumentException('Guard predicate cannot be null')
        guards.put(label, predicate)
        return this
    }

    HandlerRegistry registerGuard(String label, Closure predicate) {
        if (label == null) throw new IllegalArgumentException('Guard label cannot be null')
        if (predicate == null) throw new IllegalArgumentException('Guard predicate cannot be null')
        guards.put(label, new GuardPredicate() {
            @Override
            boolean evaluate(ExecutionContext context, RecordToken token) {
                Object res = predicate.call(context, token)
                return Boolean.TRUE.equals(res) || (res instanceof Boolean && (Boolean) res)
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

    boolean hasAction(String label) {
        return actions.containsKey(label)
    }

    boolean hasGuard(String label) {
        return guards.containsKey(label)
    }
}
