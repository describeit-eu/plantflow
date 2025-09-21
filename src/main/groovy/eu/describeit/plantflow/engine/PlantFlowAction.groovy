package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
interface PlantFlowAction {
  void execute();
  void isEnabled();
}
