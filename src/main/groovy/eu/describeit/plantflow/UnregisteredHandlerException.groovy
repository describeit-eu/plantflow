package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
class UnregisteredHandlerException extends RuntimeException {
    UnregisteredHandlerException(String message) {
        super(message)
    }

    UnregisteredHandlerException(String message, Throwable cause) {
        super(message, cause)
    }
}
