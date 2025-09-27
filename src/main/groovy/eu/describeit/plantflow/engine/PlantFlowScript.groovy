package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
abstract class PlantFlowScript extends DelegatingScript {

  List<PlantFlowAction> pflowActions
  List<PlantFlowAction> nextActions

  def action(String name) {
    def anAction = getAction(name)

    if (anAction) {
      log.info("action() - found name:{}", anAction.name)
      if (anAction.activate()) {
        nextActions << anAction
        return true
      } else {
        return false
      }
    }

    throw new MissingPropertyException("Action '$name' was not found")
  }

  def eval(String expression) {
    log.info("eval() - expression:{}", expression)
    return 'yes'
  }

  def start() {
    nextActions = []
    log.info('start()')
  }

  def stop() {
    // link with end()
    log.info('stop()')
  }

  def end() {
    // link with stop()
    log.info('end()')
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

  def is(String expressionValue) {
    log.info("is() - expressionValue:{}", expressionValue)
    return this
  }

  def not(String expressionValue) {
    log.info("not() - expressionValue:{}", expressionValue)
    return this
  }

  def fork() {
    log.info("fork()")
  }

  def forkAgain() {
    log.info("forkAgain()")
  }

  def endMerge() {
    log.info("endMerge()")
  }

  private PlantFlowAction getAction(String name) {
    return pflowActions.find {it.name == name}
  }
}
