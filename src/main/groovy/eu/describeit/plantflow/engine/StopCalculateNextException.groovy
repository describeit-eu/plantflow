package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
class StopCalculateNextException extends RuntimeException {
  StopCalculateNextException(String message) {
    super(message)
  }
}
