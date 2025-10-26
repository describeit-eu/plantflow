package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
interface PlantFlowAction {
  String getName()
  boolean activate()
}
