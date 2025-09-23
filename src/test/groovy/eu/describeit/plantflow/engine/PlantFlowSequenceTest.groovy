package eu.describeit.plantflow.engine

import spock.lang.Specification

class PlantFlowSequenceTest extends Specification {

  void 'basic scenario'() {
    given:
    def action1 = Mock(PlantFlowAction) {
      getName() >> 'Hello world'
      isActive() >> false
      1 * activate()
    }

    def action2 = Mock(PlantFlowAction) {
      getName() >> 'groovy goodness'
      isActive() >> false
    }
    def pflow = new PlantFlow("sequence.pflow", [action1, action2])

    when:
    def nextActions = pflow.calculateNext()

    then:
    nextActions
    nextActions.size() == 1
  }
}
