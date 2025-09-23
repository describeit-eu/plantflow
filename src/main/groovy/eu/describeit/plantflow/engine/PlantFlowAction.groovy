package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
interface PlantFlowAction {
  String getName()
  boolean isActive()

  void activate()
  void execute()
}
