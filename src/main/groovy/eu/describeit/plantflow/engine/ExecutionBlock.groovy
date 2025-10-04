package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includePackage=false)
class ExecutionBlock {
  enum Type { SEQ, FORK }

  Type type
  List<PlantFlowAction> nextActions = []
}
