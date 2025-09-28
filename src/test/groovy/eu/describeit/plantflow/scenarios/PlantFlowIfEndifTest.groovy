package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class PlantFlowIfEndifTest extends Specification {

  void 'if Then Else Endif'() {
    given:
    PlantFlowAction action1 = Mock() {
      getName() >> 'process all'
    }

    PlantFlowAction action2 = Mock() {
      getName() >> 'process none'
    }
    def pflow = new PlantFlow("ifThenElseEndif.pflow", [action1, action2])

    when: '1.'
    def nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> true
    0 * action2.activate()
    nextActions
    nextActions[0].name == 'process all'

    when: '2.'
    nextActions = pflow.calculateNext()

    then:
    1 * action1.activate() >> false
    0 * action2.activate()
    nextActions.size() == 0
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
    fileName << ['ifIsThenEndif.pflow', 'ifEqualsThenEndif.pflow']
  }
}
