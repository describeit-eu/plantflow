package eu.describeit.plantflow.engine

import com.fasterxml.jackson.databind.ObjectMapper
import eu.describeit.plantflow.block.BlockNode
import eu.describeit.plantflow.block.BlockType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class CalculateNextContext {
  final BlockNode rootBlock
  final Stack<BlockNode> blockStack = new Stack<>()

  List<PlantFlowAction> nextActions

  CalculateNextContext(String json) {
    ObjectMapper mapper = new ObjectMapper()
    rootBlock = mapper.readValue(json, BlockNode)
  }

  void initialise() {
    nextActions = []
  }

  void start(BlockType type, String id) {
    log.info('start() - type:{} id:{}', type, id)

    if (rootBlock.id == id) {
      blockStack.push(rootBlock)
    }
    else {
      blockStack.push(rootBlock.find(id))
    }
  }

  void addAction(PlantFlowAction action) {
    nextActions.add(action)
  }

  void check(BlockType type = null, String id) {
    if (type) assert blockStack.last.type == type
    assert blockStack.last.id == id
  }

  List<PlantFlowAction> end(BlockType type, String id) {
    log.info('end() - type:{} id:{}', type, id)

    check(type, id)
    blockStack.pop()

    return nextActions
  }
}
