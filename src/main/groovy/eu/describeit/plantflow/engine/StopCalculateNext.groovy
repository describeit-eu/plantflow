package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
class StopCalculateNext extends RuntimeException {
  StopCalculateNext(String message) {
    super(message)
  }
}
