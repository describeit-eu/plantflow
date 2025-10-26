package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioCrudTest extends Specification {

  void 'crud with two infinite loops inside fork produces actions from both branches'() {
    given:
    PlantFlowAction update = Mock() { getName() >> 'update' }
    PlantFlowAction deactivate = Mock() { getName() >> 'deactivate' }
    PlantFlowAction activate = Mock() { getName() >> 'activate' }

    def pflow = new PlantFlow('crud', [update, deactivate, activate])

    when: '1st run - both branches produce an action'
    def nextActions = pflow.calculateNext()

    then:
    1 * update.activate() >> true
    1 * deactivate.activate() >> true
    0 * activate.activate()
    nextActions*.name as Set == ['update', 'deactivate'] as Set

    when: '2nd run - first branch inactive, second branch returns activate'
    nextActions = pflow.calculateNext()

    then:
    1 * update.activate() >> false
    1 * deactivate.activate() >> false
    1 * activate.activate() >> true
    nextActions*.name == ['activate']

    when: '3rd run - no branch produces actions'
    nextActions = pflow.calculateNext()

    then:
    1 * update.activate() >> false
    1 * deactivate.activate() >> false
    1 * activate.activate() >> false
    nextActions.isEmpty()
  }
}
