package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification
import spock.lang.Unroll

class CalculateNextScenarioIfEndifTest extends Specification {
  @Unroll
  void 'basic scenario for pflow: #fileName'() {
    given:
    PlantFlowAction action1 = Mock() { getName() >> 'process all' }
    def pflow = new PlantFlow(fileName, [action1])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.isActive() >> true
    nextActions
    nextActions[0].name == 'process all'

    when:
    nextActions = pflow.calculateNext()

    then:
    1 * action1.isActive() >> false
    nextActions.size() == 0

    where:
    fileName << ['ifIsThenEndif', 'ifEqualsThenEndif']
  }
}
