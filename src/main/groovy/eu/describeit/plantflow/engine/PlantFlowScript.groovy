package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.BlockContext.BlockType.*

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {
  Stack<BlockContext> blocks

  Map<String, PlantFlowAction> actions
  List<PlantFlowAction> nextActions

  abstract Object scriptBody()

  @Override
  Object run() {
    nextActions = []
    blocks = new Stack<>()

    log.trace('run() - start')
    blocks.push(new BlockContext(type: SEQ))

    def result = scriptBody()

    def lastContext = blocks.pop()
    assert lastContext && lastContext.type == SEQ
    nextActions.addAll(lastContext.nextActions)

    log.trace('run() - # of nextActions:{}', nextActions.size())

    return result
  }

  Boolean isActive(String action) {
    PlantFlowAction anAction = actions[action]

    if (anAction) {
      if (anAction.activate()) {
        log.info("isActive() - active name:{}", anAction.name)
        blocks.last.nextActions << anAction
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

    def result = expectedValue == null ? super.evaluate(expression) : super.evaluate(expression) == expectedValue

    return result as Boolean
  }

  def fork(Closure cl) {
    log.info("fork()")

    blocks.push(new BlockContext(type: FORK))

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  def forkAgain(Closure cl) {
    log.info("forkAgain()")

    assert blocks.last.type == FORK

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  Boolean endFork() {
    def forkBlock = blocks.pop()
    assert forkBlock && forkBlock.type == FORK

    blocks.last.nextActions.addAll(forkBlock.nextActions)

    return forkBlock.nextActions as Boolean
  }
}
