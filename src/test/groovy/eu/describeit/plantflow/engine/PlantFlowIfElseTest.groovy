package eu.describeit.plantflow.engine

import eu.describeit.plantflow.PlantFlow
import spock.lang.Specification

class PlantFlowIfElseTest extends Specification {

  void 'basic scenario'() {
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
}
