package eu.describeit.plantflow.engine

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
        log.info("isActive() - active name:{}, blockId:{}", anAction.name, blockId)
        executionContext.addAction(anAction)
        return true
      } else {
        log.info("isActive() - inactive name:{}, blockId:{}", anAction.name, blockId)
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

    return this
  }

  PlantFlowScript forkAgain(String forkId, Closure cl) {
    log.info("forkAgain() - id:{}", forkId)

    executionContext.check(FORK, forkId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  Boolean endFork(String forkId) {
    def forkActions = executionContext.end(FORK, forkId)
    return forkActions as Boolean
  }

  def loop(String loopId, Closure cl) {
    log.info('loop() - id:{}', loopId)

    executionContext.start(LOOP, loopId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    executionContext.end(LOOP, loopId)
  }

  def conditional(String conditionalId, Closure cl) {
    log.info('conditional() - id:{}', conditionalId)

    executionContext.start(CONDITIONAL, conditionalId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    executionContext.end(CONDITIONAL, conditionalId)
  }
}
