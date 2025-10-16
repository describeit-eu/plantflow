package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class ScenarioIfEndifTest extends Specification {

  void 'if Then Else Endif'() {
    given:
    PlantFlowAction processAll = Mock() { getName() >> 'process all' }
    PlantFlowAction processNone = Mock() { getName() >> 'process none' }
    PlantFlow pflow = new PlantFlow("ifThenElseEndif", [processAll, processNone])

    when:
    def nextActions = pflow.calculateNext()

    then:
    1 * processAll.activate() >> true
    0 * processNone.activate()
    nextActions[0].name == 'process all'

    when:
    nextActions = pflow.calculateNext()

    then:
    1 * processAll.activate() >> false
    0 * processNone.activate()
    nextActions.isEmpty()
  }

  void 'if Then Endif'() {
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
