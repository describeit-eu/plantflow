package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class ScenarioForkEndMergeTest extends Specification {

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
    1 * action1.activate() >> true
    1 * action2.activate() >> true
    1 * action3.activate() >> true
    0 * action4.activate()
    0 * action5.activate()
    nextActions.size() == 3
  }
}
