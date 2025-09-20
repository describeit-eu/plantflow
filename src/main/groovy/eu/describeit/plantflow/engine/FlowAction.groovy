package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
interface FlowAction {
  void execute(String input);
  void isEnabled(String input);
}
