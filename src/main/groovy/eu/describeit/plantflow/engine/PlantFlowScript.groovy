package eu.describeit.plantflow.engine

import eu.describeit.plantflow.block.BlockNode
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

    executionContext.start(SEQ, 'SEQ0')
    def result = scriptBody()
    executionContext.end(SEQ, 'SEQ0')

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

  PlantFlowScript fork(String forkId, Closure cl) {
    log.info("fork() - id:{}", forkId)

    executionContext.start(FORK, forkId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    def forkBlock = executionContext.end(FORK, forkId)
    if (! forkBlock.isFinished()) {
      throw new StopCalculateNextException("Fork NOT finished - id:$forkId")
    }

    return this
  }

  PlantFlowScript forkBlock(String forkBlockId, Closure cl) {
    log.info("forkBlock() - id:{}", forkBlockId)

    executionContext.start(FORK_BLOCK, forkBlockId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    executionContext.end(FORK_BLOCK, forkBlockId)

    return this
  }

  void loop(String loopId, Closure cl) {
    log.info('loop() - id:{}', loopId)

    executionContext.start(LOOP, loopId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    executionContext.end(LOOP, loopId)
  }

  void conditional(String conditionalId, Closure cl) {
    log.info('conditional() - id:{}', conditionalId)

    executionContext.start(CONDITIONAL, conditionalId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    executionContext.end(CONDITIONAL, conditionalId)
  }
}
