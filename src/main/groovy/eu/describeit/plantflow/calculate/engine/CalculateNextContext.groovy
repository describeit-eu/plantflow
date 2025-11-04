package eu.describeit.plantflow.calculate.engine

import com.fasterxml.jackson.databind.ObjectMapper
import eu.describeit.plantflow.PlantFlowAction
import eu.describeit.plantflow.block.Block
import eu.describeit.plantflow.block.BlockType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class CalculateNextContext {
  final Block rootBlock
  final Stack<Block> blockStack = new Stack<>()

  List<PlantFlowAction> nextActions = []

  CalculateNextContext(String json) {
    ObjectMapper mapper = new ObjectMapper()
    rootBlock = mapper.readValue(json, Block)
  }

  void initialise() {
    nextActions.clear()
  }

  Block startBlock(BlockType type, String id) {
    final Block nextBlock = rootBlock.find(id)

    assert nextBlock, "Unable to find Block by id:$id"

    log.info('startBlock() - {}', nextBlock)

    nextBlock.initialise()
    return blockStack.push(nextBlock)
  }

  Block addAction(PlantFlowAction action, String blockId) {
    def currentBlock = checkBlock(blockId)
    currentBlock.addNextAction(action)
    nextActions.add(action)
    return blockStack.last
  }

  Block checkBlock(BlockType type = null, String id) {
    if (type) assert blockStack.last.type == type
    assert blockStack.last.id == id

    return blockStack.last
  }

  Block endBlock(BlockType type, String id) {
    def currentBlock = checkBlock(type, id)
    log.info('endBlock() - {}', currentBlock)

    return blockStack.pop()
  }
}
