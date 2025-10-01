package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.BlockContext.BlockType.SEQ

@CompileStatic
@Slf4j
class ExecutionContext {
  Stack<BlockContext> blocks = new Stack<>()
  List<PlantFlowAction> nextActions = []

  void start(BlockContext.BlockType type) {
    log.info('startContext() - type:{}', type)

    blocks.push(new BlockContext(type: type))
  }

  void addAction(PlantFlowAction action) {
    if (blocks.last.type == SEQ) assert blocks.last.nextActions.empty

    blocks.last.nextActions.add(action)
  }

  void check(BlockContext.BlockType type) {
    assert blocks.last.type == type
  }

  List<PlantFlowAction> end(BlockContext.BlockType type) {
    log.info('endContext() - type:{}', type)

    check(type)
    BlockContext endingContext = blocks.pop()

    if (blocks.empty()) nextActions = endingContext.nextActions
    else blocks.last.nextActions.addAll(endingContext.nextActions)

    return endingContext.nextActions
  }
}
