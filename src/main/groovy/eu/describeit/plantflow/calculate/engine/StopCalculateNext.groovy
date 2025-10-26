package eu.describeit.plantflow.calculate.engine

import groovy.transform.CompileStatic

@CompileStatic
class StopCalculateNext extends RuntimeException {
  StopCalculateNext(String message) {
    super(message)
  }
}
