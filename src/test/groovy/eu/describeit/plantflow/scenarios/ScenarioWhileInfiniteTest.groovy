package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class ScenarioWhileInfiniteTest extends Specification {

  void 'basic scenario'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'do something' }

    def pflow = new PlantFlow("whileInfinite.pflow", [action1])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> true
    nextActions.size() == 1
  }
}
