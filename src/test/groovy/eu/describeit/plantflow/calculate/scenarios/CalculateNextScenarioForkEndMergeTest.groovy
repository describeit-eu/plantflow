package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioForkEndMergeTest extends Specification {

  void 'basic scenario'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'action 1' }
    PlantFlowAction action2 = Mock() { getName() >> 'action 2' }
    PlantFlowAction action3 = Mock() { getName() >> 'action 3' }
    PlantFlowAction action4 = Mock() { getName() >> 'action 4' }
    PlantFlowAction action5 = Mock() { getName() >> 'action 5' }

    def pflow = new PlantFlow("forkEndMerge", [action1, action2, action3, action4, action5])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.isActive() >> true
    1 * action2.isActive() >> true
    1 * action3.isActive() >> true
    0 * action4.isActive()
    0 * action5.isActive()
    nextActions.size() == 3
  }
}
