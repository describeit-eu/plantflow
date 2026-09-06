package eu.describeit.plantflow

import groovy.transform.CompileStatic

@FunctionalInterface
@CompileStatic
interface ActionHandler {
    Object execute(ExecutionContext context, Token token)
}
