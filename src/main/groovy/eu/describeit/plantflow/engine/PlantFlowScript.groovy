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

    log.info('run() - start')
    blocks.push(new BlockContext(type: SEQ))

    def result = scriptBody()

    def lastContext = blocks.pop()
    assert lastContext && lastContext.type == SEQ
    nextActions.addAll(lastContext.nextActions)

    log.info('run() - # of nextActions:{}', nextActions.size())

    return result
  }

  Boolean isActive(String action) {
    PlantFlowAction anAction = actions[action]

    if (anAction) {
      log.info("isActive() - found name:{}", anAction.name)

      if (anAction.activate()) {
        blocks.last.nextActions << anAction
        return true
      } else {
        return false
      }
    } else {
      throw new MissingPropertyException("Action '$action' was not found")
    }
  }

  @Override
  Object evaluate(String expression) {
    log.info("eval() - expression:{}", expression)
    return super.evaluate(expression)
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

  def detach() {
    // for infinite loop it behaves like end()/stop()
    log.info('detach()')
  }

  def switchh(String expression) {
    log.info("switchh() - expression:{}", expression)
    return this
  }

  def casee(String expressionValue) {
    log.info("casee() - expressionValue:{}", expressionValue)
  }

  def endswitch() {
    log.info("endswitch()")
  }

  def repeat() {
    log.info("repeat()")
    return this
  }

  def repeatWhile(String expression) {
    log.info("repeatWhile() - expression:{}", expression)
    return this
  }

  def whilee(String expression) {
    log.info("whilee() - expression:{}", expression)
    return this
  }

  def endwhile(String expression) {
    log.info("endwhile() - expression:{}", expression)
  }

  def not(String expressionValue) {
    log.info("not() - expressionValue:{}", expressionValue)
    return this
  }
}
