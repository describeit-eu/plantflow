package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioWhileInfiniteTest extends Specification {

  void 'basic scenario'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'do something' }

    def pflow = new PlantFlow("whileInfinite", [action1])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.isActive() >> true
    nextActions.size() == 1

    // TODO: this scenario needs to be extended
  }
}
