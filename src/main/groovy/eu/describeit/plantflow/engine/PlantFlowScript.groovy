package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.BlockContext.BlockType.*

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {
  ContextManager context = new ContextManager()

  Map<String, PlantFlowAction> actions

  abstract Object scriptBody()

  List<PlantFlowAction> getNextActions() {
    return context.nextActions
  }

  @Override
  Object run() {

    context.start(SEQ)
    def result = scriptBody()
    context.end(SEQ)

    log.trace('run() - # of nextActions:{}', nextActions.size())

    return result
  }

  Boolean isActive(String action) {
    PlantFlowAction anAction = actions[action]

    if (anAction) {
      if (anAction.activate()) {
        log.info("isActive() - active name:{}", anAction.name)
        context.addAction(anAction)
        return true
      } else {
        log.info("isActive() - inactive name:{}", anAction.name)
        return false
      }
    } else {
      throw new MissingPropertyException("Action '$action' was not found")
    }
  }

  @Override
  Object evaluate(String expression) {
    return evaluate(expression, null)
  }

  Boolean evaluate(String expression, String expectedValue) {
    log.info("evaluate() - expression:{} expectedValue:{}", expression, expectedValue)

    def evalResult = super.evaluate(expression)
    def returnValue = (expectedValue == null) ? evalResult : evalResult == expectedValue

    return returnValue as Boolean
  }

  def fork(Closure cl) {
    log.info("fork()")

    context.start(FORK)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  def forkAgain(Closure cl) {
    log.info("forkAgain()")

    context.check(FORK)

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  Boolean endFork() {
    def forkActions = context.end(FORK)
    return forkActions as Boolean
  }
}
