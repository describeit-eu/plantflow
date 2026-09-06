package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import groovy.transform.CompileStatic

@FunctionalInterface
@CompileStatic
interface ActionHandler {
    Object execute(ExecutionContext context, Token token)
}
