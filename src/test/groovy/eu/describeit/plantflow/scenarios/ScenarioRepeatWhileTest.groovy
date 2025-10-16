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
  List<String> scriptEvaluateMockResults = ['yes', 'no']

  void mockPlantFlow() {
    readData         = Mock() { getName() >> 'read data' }
    generateDiagrams = Mock() { getName() >> 'generate diagrams' }
    pflow = new PlantFlow('repeatWhile', [readData, generateDiagrams])

    pflow.pflowScript.metaClass.evaluate = { String expression ->
      assert expression == 'more data?'
      def exprValue = scriptEvaluateMockResults.remove(0)
      log.info("MOCKED Script.evaluate() - exprValue:{}", exprValue)
      return exprValue
    }
  }

  void 'scenario executes body at least once and repeats while condition true'() {
    given:
    mockPlantFlow()

    when: '1st calculateNext() - first action from body'
    def nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['read data']
    1 * readData.activate() >> true
    0 * generateDiagrams.activate()

    when: '2nd calculateNext() - second action from body'
    nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['generate diagrams']
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> true

    when: '3rd calculateNext() - first execution of while() loops to enable first action'
    nextActions = pflow.calculateNext()

    then:
    2 * readData.activate() >>> [false, true]
    1 * generateDiagrams.activate() >> false
    nextActions*.name == ['read data']

    when: '4th calculateNext() - second action again'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> true
    nextActions*.name == ['generate diagrams']

    when: '5th calculateNext() - second execution of while() leaves the loop'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> false
    nextActions.isEmpty()
  }
}
