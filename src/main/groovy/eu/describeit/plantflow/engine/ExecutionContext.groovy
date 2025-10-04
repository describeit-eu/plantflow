package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.ExecutionBlock.Type.SEQ

@CompileStatic
@Slf4j
class ExecutionContext {
  Stack<ExecutionBlock> blocks = new Stack<>()
  List<PlantFlowAction> nextActions = []

  void start(ExecutionBlock.Type type) {
    log.info('startContext() - type:{}', type)

    blocks.push(new ExecutionBlock(type: type))
  }

  void addAction(PlantFlowAction action) {
    if (blocks.last.type == SEQ) {
      assert blocks.last.nextActions.empty, 'SEQ already have an active Action:'+blocks.last.nextActions[0].name
    }

    blocks.last.nextActions.add(action)
  }

  void check(ExecutionBlock.Type type) {
    assert blocks.last.type == type
  }

  List<PlantFlowAction> end(ExecutionBlock.Type type) {
    log.info('endContext() - type:{}', type)

    check(type)
    ExecutionBlock endingContext = blocks.pop()

    if (blocks.empty()) nextActions = endingContext.nextActions
    else blocks.last.nextActions.addAll(endingContext.nextActions)

    return endingContext.nextActions
  }
}
