package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioCrudTest extends Specification {

  void 'crud with two infinite loops inside fork produces actions from both branches'() {
    given:
    PlantFlowAction init = Mock() { getName() >> 'init' }
    PlantFlowAction update = Mock() { getName() >> 'update' }
    PlantFlowAction deactivate = Mock() { getName() >> 'deactivate' }
    PlantFlowAction activate = Mock() { getName() >> 'activate' }

    def pflow = new PlantFlow('crud', [init, update, deactivate, activate])

    when: '0st run - the initial update is enabled'
    def nextActions = pflow.calculateNext()

    then:
    1 * init.isActive() >> true
    0 * update.isActive()
    0 * deactivate.isActive()
    0 * activate.isActive()
    nextActions*.name == ['init']

    when: '1st run - both branches produce an action'
    nextActions = pflow.calculateNext()

    then:
    1 * init.isActive() >> false
    1 * update.isActive() >> true
    1 * deactivate.isActive() >> true
    0 * activate.isActive()
    nextActions*.name == ['update', 'deactivate']

    when: '2nd run - first branch inactive, second branch returns activate'
    nextActions = pflow.calculateNext()

    then:
    1 * init.isActive() >> false
    1 * update.isActive() >> false
    1 * deactivate.isActive() >> false
    1 * activate.isActive() >> true
    nextActions*.name == ['activate']

    when: '3rd run - no branch produces actions'
    nextActions = pflow.calculateNext()

    then:
    1 * init.isActive() >> false
    1 * update.isActive() >> false
    1 * deactivate.isActive() >> false
    1 * activate.isActive() >> false
    nextActions.isEmpty()
  }
}
