package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.ExecutionBlock.Type.SEQ

@CompileStatic
@Slf4j
class ExecutionContext {
  Stack<ExecutionBlock> blocks = new Stack<>()
  List<PlantFlowAction> nextActions = []

  void start(ExecutionBlock.Type type, String id) {
    log.info('start() - type:{} id:{}', type, id)

    blocks.push(new ExecutionBlock(type: type, id: id))
  }

  void addAction(PlantFlowAction action) {
    if (blocks.last.type == SEQ) {
      assert blocks.last.nextActions.empty, 'SEQ already have an active Action:'+blocks.last.nextActions[0].name
    }

    blocks.last.nextActions.add(action)
  }

  void check(ExecutionBlock.Type type, String id) {
    assert blocks.last.type == type && blocks.last.id == id
  }

  List<PlantFlowAction> end(ExecutionBlock.Type type, String id) {
    check(type, id)
    ExecutionBlock endingContext = blocks.pop()

    log.info('end() - context:{}', endingContext)

    if (blocks.empty()) nextActions = endingContext.nextActions
    else blocks.last.nextActions.addAll(endingContext.nextActions)

    return endingContext.nextActions
  }
}
