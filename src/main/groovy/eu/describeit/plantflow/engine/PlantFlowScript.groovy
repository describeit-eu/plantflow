package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.InvokerHelper

import static eu.describeit.plantflow.engine.ExecutionBlock.Type.*

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {
  ExecutionContext executionContext = new ExecutionContext()

  Map<String, PlantFlowAction> actions

  abstract Object scriptBody()

  List<PlantFlowAction> getNextActions() {
    return executionContext.nextActions
  }

  @Override
  Object run() {
    executionContext.start(SEQ)
    def result = scriptBody()
    executionContext.end(SEQ)

    log.trace('run() - # of nextActions:{}', nextActions.size())

    return result
  }

  Boolean isActive(String action) {
    return isActive(action, null)
  }

  Boolean isActive(String action, String loopId) {
    PlantFlowAction anAction = actions[action]

    if (anAction) {
      if (anAction.activate()) {
        log.info("isActive() - active name:{}, loopId:{}", anAction.name, loopId)
        executionContext.addAction(anAction)
        return true
      } else {
        log.info("isActive() - inactive name:{}, loopId:{}", anAction.name, loopId)
        return false
      }
    } else {
      throw new MissingPropertyException("Action '$action' was not found")
    }
  }

  Boolean eval(String expression) {
    return eval(expression, null)
  }

  Boolean eval(String expression, String expectedValue) {
    log.info("eval() - expression:{} expectedValue:{}", expression, expectedValue)

    // Use Groovy MOP to allow mocking Script.evaluate(String) via metaclass
    def evalResult = InvokerHelper.invokeMethod(this, 'evaluate', expression)
    def returnValue = (expectedValue == null) ? evalResult : evalResult == expectedValue

    return returnValue as Boolean
  }

  PlantFlowScript fork(String forkId, Closure cl) {
    log.info("fork() - forkId:{}", forkId)

    executionContext.start(FORK)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  PlantFlowScript forkAgain(String forkId, Closure cl) {
    log.info("forkAgain() - forkId:{}", forkId)

    executionContext.check(FORK)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  Boolean endFork(String forkId) {
    def forkActions = executionContext.end(FORK)
    return forkActions as Boolean
  }

  def loop(String loopId, Closure cl) {
    log.info('loop() - loopId:{}', loopId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()
  }

  def conditional(String condId, Closure cl) {
    log.info('conditional() - loopId:{}', condId)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()
  }
}
