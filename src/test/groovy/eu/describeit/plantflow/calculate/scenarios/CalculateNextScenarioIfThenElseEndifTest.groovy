package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class CalculateNextScenarioIfThenElseEndifTest extends Specification {

  void 'basic scenario'() {
    given:
    PlantFlowAction processAll = Mock() { getName() >> 'process all' }
    PlantFlowAction processNone = Mock() { getName() >> 'process none' }
    PlantFlow pflow = new PlantFlow("ifThenElseEndif", [processAll, processNone])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * processAll.isActive() >> true
    0 * processNone.isActive()
    nextActions[0].name == 'process all'

    when:
    nextActions = pflow.calculateNext()

    then:
    1 * processAll.isActive() >> false
    0 * processNone.isActive()
    nextActions.isEmpty()
  }
}
