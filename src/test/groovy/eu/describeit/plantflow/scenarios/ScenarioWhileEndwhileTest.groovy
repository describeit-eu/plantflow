package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class ScenarioWhileEndwhileTest extends Specification {
  PlantFlowAction readFile
  PlantFlowAction closeFile
  PlantFlow pflow
  List<String> scriptEvaluateMockResults = ['not empty', 'not empty', 'empty', 'empty']

  void mockPlantFlow() {
    readFile = Mock() { getName() >> 'read file' }
    closeFile = Mock() { getName() >> 'close file' }

    pflow = new PlantFlow('whileEndwhile', [readFile, closeFile])

    pflow.pflowScript.metaClass.evaluate = { String expression ->
      assert expression == 'check filesize ?'
      def exprValue = scriptEvaluateMockResults.remove(0)
      log.info("MOCKED Script.evaluate() - exprValue:{}", exprValue)
      return exprValue
    }
  }

  void 'scenario loops until condition becomes false, then continues'() {
    given:
    mockPlantFlow()

    when: '1st run - while condition true -> execute body (read file)'
    def nextActions = pflow.calculateNext()

    then:
    1 * readFile.activate() >> true
    0 * closeFile.activate()
    nextActions*.name == ['read file']

    when: '2nd run - while condition still true -> execute body again'
    nextActions = pflow.calculateNext()

    then:
    1 * readFile.activate() >> true
    0 * closeFile.activate()
    nextActions*.name == ['read file']

    when: '3rd run - while condition becomes false -> proceed after loop (close file)'
    nextActions = pflow.calculateNext()

    then:
    0 * readFile.activate()
    1 * closeFile.activate() >> true
    nextActions*.name == ['close file']

    when: '4th run - no more actions'
    nextActions = pflow.calculateNext()

    then:
    0 * readFile.activate()
    1 * closeFile.activate() >> false
    nextActions.isEmpty()
  }
}
