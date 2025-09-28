package eu.describeit.plantflow.engine

class BlockContext {
  enum BlockType { SEQ, FORK }

  BlockType type
  List<PlantFlowAction> nextActions = []
}
