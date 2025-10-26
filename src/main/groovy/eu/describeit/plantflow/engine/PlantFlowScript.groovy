package eu.describeit.plantflow.engine

import eu.describeit.plantflow.block.BlockNode
import eu.describeit.plantflow.block.BlockType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.InvokerHelper

import static eu.describeit.plantflow.block.BlockType.*

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {
  CalculateNextContext executionContext

  Map<String, PlantFlowAction> actions

  abstract Object scriptBody()

  List<PlantFlowAction> getNextActions() {
    return executionContext.nextActions
  }

  @Override
  Object run() {
    executionContext.initialise()

    def result = scriptBody()

    log.trace('run() - # of nextActions:{}', nextActions.size())

    return result
  }

  Boolean isActive(String action) {
    return isActive(action, null)
  }

  Boolean isActive(String action, String blockId) {
    PlantFlowAction anAction = actions[action]

    if (anAction) {
      if (anAction.activate()) {
        log.info("isActive( true ) - name:{}, blockId:{}", anAction.name, blockId)
        executionContext.addAction(anAction, blockId)
        return true
      } else {
        log.info("isActive( false ) - inactive name:{}, blockId:{}", anAction.name, blockId)
        return false
      }
    } else {
      throw new MissingPropertyException("Action '$action' was not found")
    }
  }

  Boolean eval(String expression, String expectedValue, String blockId) {
    log.info("eval() - expression:{}, expectedValue:{}, blockId:{}", expression, expectedValue, blockId)

    executionContext.check(blockId)

    // Use Groovy MOP to allow mocking Script.evaluate(String) via metaclass
    def evalResult = InvokerHelper.invokeMethod(this, 'evaluate', expression)
    def returnValue = (expectedValue == null) ? evalResult : evalResult == expectedValue

    return returnValue as Boolean
  }

  PlantFlowScript rootBlock(String rootBlockId, Closure cl) {
    executeBlock(ROOT_BLOCK, rootBlockId, cl)
    return this
  }

  PlantFlowScript fork(String forkId, Closure cl) {
    executeBlock(FORK, forkId, cl)
    return this
  }

  PlantFlowScript forkBlock(String forkBlockId, Closure cl) {
    executeBlock(FORK_BLOCK, forkBlockId, cl)
    return this
  }

  PlantFlowScript loop(String loopId, Closure cl) {
    executeBlock(LOOP, loopId, cl)
    return this
  }

  PlantFlowScript loopBlock(String loopBlockId, Closure cl) {
    executeBlock(LOOP_BLOCK, loopBlockId, cl)
    return this
  }

  PlantFlowScript conditional(String conditionalId, Closure cl) {
    executeBlock(CONDITIONAL, conditionalId, cl)
    return this
  }

  PlantFlowScript ifBlock(String ifBlockId, Closure cl) {
    executeBlock(IF_BLOCK, ifBlockId, cl)
    return this
  }

  PlantFlowScript elseIfBlock(String elseIfBlockId, Closure cl) {
    executeBlock(ELSEIF_BLOCK, elseIfBlockId, cl)
    return this
  }

  PlantFlowScript elseBlock(String elseBlockId, Closure cl) {
    executeBlock(ELSE_BLOCK, elseBlockId, cl)
    return this
  }
  
  private BlockNode executeBlock(BlockType type, String blockId, Closure cl) {
    log.info("executeBlock() - type:{}, id:{}", type, blockId)

    executionContext.start(type, blockId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    BlockNode block = executionContext.end(type, blockId)

    if (! block.isFinished()) {
      throw new StopCalculateNext("$type NOT finished - id:$blockId")
    }

    return block
  }
}
