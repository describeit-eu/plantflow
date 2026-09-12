package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
class UnregisteredHandlerException extends RuntimeException {
    UnregisteredHandlerException(String message) {
        super(message)
    }
}
