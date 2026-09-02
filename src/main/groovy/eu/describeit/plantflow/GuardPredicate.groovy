package eu.describeit.plantflow

import groovy.transform.CompileStatic

@FunctionalInterface
@CompileStatic
interface GuardPredicate {
    boolean evaluate(ExecutionContext context, RecordToken token)
}
