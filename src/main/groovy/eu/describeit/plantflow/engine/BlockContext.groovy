package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString
class BlockContext {
  enum BlockType { SEQ, FORK }

  BlockType type
  List<PlantFlowAction> nextActions = []
}
