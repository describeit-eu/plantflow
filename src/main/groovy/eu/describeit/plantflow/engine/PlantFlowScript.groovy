package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {

  Map<String, PlantFlowAction> actions
  List<PlantFlowAction> nextActions

  abstract Object scriptBody()

  @Override
  Object run() {
    nextActions = []

    log.info('run() - start')
    def result = scriptBody()
    log.info('run() - end')

    return result
  }

  Boolean action(String name) {
    def anAction = actions[name]

    if (anAction) {
      log.info("action() - found name:{}", anAction.name)

      if (anAction.activate()) {
        nextActions << anAction
        return true
      } else {
        return false
      }
    } else {
      throw new MissingPropertyException("Action '$name' was not found")
    }
  }

  def eval(String expression) {
    log.info("eval() - expression:{}", expression)
    return evaluate(expression)
  }

  def fork(Closure cl) {
    log.info("fork()")

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
  }

  def endFork() {

  }

  def forkAgain(Closure cl) {
    log.info("forkAgain()")

    cl.delegate = this
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()

    return this
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
