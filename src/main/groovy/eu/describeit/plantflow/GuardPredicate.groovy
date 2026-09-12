package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import groovy.transform.CompileStatic

@FunctionalInterface
@CompileStatic
interface GuardPredicate {
    boolean evaluate(ExecutionContext context, Token token)
}
