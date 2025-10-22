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

  BlockNode start(BlockType type, String id) {
    log.info('start() - type:{} id:{}', type, id)
    final BlockNode nextBlock

    if (rootBlock.id == id) nextBlock = rootBlock
    else                    nextBlock = rootBlock.find(id)

    nextBlock.nextActions = []
    return blockStack.push(nextBlock)
  }

  void addAction(PlantFlowAction action, String blockId) {
    check(blockId)
    blockStack.last.nextActions.add(action)
    nextActions.add(action)
  }

  void check(BlockType type = null, String id) {
    if (type) assert blockStack.last.type == type
    assert blockStack.last.id == id
  }

  BlockNode end(BlockType type, String id) {
    log.info('end() - type:{} id:{}', type, id)
    check(type, id)

    return blockStack.pop()
  }
}
