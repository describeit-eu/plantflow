package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.engine.PlantFlowDelegate.RunMode.*

@CompileStatic
@Slf4j
class PlantFlowDelegate {
  enum RunMode {CALCULATE_NEXT, EXECUTE, DRY}

  List<PlantFlowAction> pflowActions
  List<PlantFlowAction> nextActions

  RunMode runMode = DRY

  def start() {
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

  def action(String name) {
    if (runMode != DRY) {
      def action = getAction(name)
      log.info("action() found name:{}", name)

      if (action.active) {
        action.activate()
      }
    }
  }

  def iff(String expression) {
    log.info("iff() - expression:{}", expression)
    return this
  }

  def then(String expressionValue) {
    log.info("then() - expressionValue:{}", expressionValue)
  }

  def elsee(String expressionValue) {
    log.info("elsee() - expressionValue:{}", expressionValue)
  }

  def endif() {
    log.info("endif()")
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
