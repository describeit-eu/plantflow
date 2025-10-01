package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class ScenarioRepeatWhileTest extends Specification {
  PlantFlowAction readData
  PlantFlowAction generateDiagrams
  PlantFlow pflow

  def mockPlantFlow() {
    readData         = Mock() { getName() >> 'read data' }
    generateDiagrams = Mock() { getName() >> 'generate diagrams' }

    pflow = new PlantFlow('repeatWhile.pflow', [readData, generateDiagrams])

    List<String> exprMockResults = ['yes', 'no', 'no']

    pflow.pflowScript.metaClass.evaluate = { String expression, String expectedValue ->
      assert expression == 'more data?'
      def exprValue = exprMockResults.remove(0)
      log.info("mock evaluate() - exprValue:{}", exprValue)
      return exprValue
    }
  }

  void 'scenario executes body at least once and repeats while condition true'() {
    given:
    mockPlantFlow()

    when: '1st run - first action from body'
    def nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['read data']
    1 * readData.activate() >> true
    0 * generateDiagrams.activate()

    when: '2nd run - second action from body'
    nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['generate diagrams']
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> true

    when: '3rd run - no actions active'
    nextActions = pflow.calculateNext()

    then:
    nextActions.isEmpty()
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> false

    when: '4th run - second action again'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> true
    0 * generateDiagrams.activate()
    nextActions*.name == ['generate diagrams']

    when: '5th run - nothing left'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> false
    nextActions.isEmpty()
  }
}
