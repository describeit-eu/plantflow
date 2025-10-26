package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioSequenceTest extends Specification {

  void 'basic scenario'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'Hello world' }
    PlantFlowAction action2 = Mock() { getName() >> 'groovy goodness' }
    def pflow = new PlantFlow("sequence", [action1, action2])

    when: '1.'
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> true
    0 * action2.activate() >> false
    nextActions
    nextActions[0].name == 'Hello world'

    when: '2.'
    nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> false
    1 * action2.activate() >> true
    nextActions
    nextActions[0].name == 'groovy goodness'

    when: '3.'
    nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> false
    1 * action2.activate() >> false
    nextActions.size() == 0
  }
}
