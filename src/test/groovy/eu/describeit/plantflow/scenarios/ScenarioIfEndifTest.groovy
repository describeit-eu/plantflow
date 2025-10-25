package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification
import spock.lang.Unroll

class ScenarioIfEndifTest extends Specification {
  @Unroll
  void 'basic scenario for pflow: #fileName'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'process all' }
    def pflow = new PlantFlow(fileName, [action1])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> true
    nextActions
    nextActions[0].name == 'process all'

    when:
    nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> false
    nextActions.size() == 0

    where:
    fileName << ['ifIsThenEndif', 'ifEqualsThenEndif']
  }
}
