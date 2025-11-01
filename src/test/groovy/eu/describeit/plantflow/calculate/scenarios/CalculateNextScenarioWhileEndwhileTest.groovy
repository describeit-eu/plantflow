package eu.describeit.plantflow.calculate.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.PlantFlowAction
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class CalculateNextScenarioWhileEndwhileTest extends Specification {
  final List<String> scriptEvaluateMockResults = ['not empty', 'not empty', 'empty', 'empty']

  PlantFlowAction openFile
  PlantFlowAction readFile
  PlantFlowAction closeFile
  PlantFlow pflow

  void mockPlantFlow() {
    openFile = Mock() { getName() >> 'open file' }
    readFile = Mock() { getName() >> 'read file' }
    closeFile = Mock() { getName() >> 'close file' }

    pflow = new PlantFlow('whileEndwhile', [openFile, readFile, closeFile])

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

    when: '0st run - execute Action "open file"'
    def nextActions = pflow.calculateNext()
    
    then:
    1 * openFile.isActive() >> true
    0 * readFile.isActive()
    0 * closeFile.isActive()
    nextActions*.name == ['open file']

    when: '1st run - while condition true -> execute body (read file)'
    nextActions = pflow.calculateNext()

    then:
    1 * openFile.isActive() >> false
    1 * readFile.isActive() >> true
    0 * closeFile.isActive()
    nextActions*.name == ['read file']

    when: '2nd run - while condition still true -> execute body again'
    nextActions = pflow.calculateNext()

    then:
    1 * openFile.isActive() >> false
    1 * readFile.isActive()  >> true
    0 * closeFile.isActive()
    nextActions*.name == ['read file']

    when: '3rd run - while condition becomes false -> proceed after loop (close file)'
    nextActions = pflow.calculateNext()

    then:
    1 * openFile.isActive() >> false
    0 * readFile.isActive()
    
    and:
    1 * closeFile.isActive() >> true
    nextActions*.name == ['close file']

    when: '4th run - no more actions'
    nextActions = pflow.calculateNext()

    then:
    1 * openFile.isActive() >> false
    0 * readFile.isActive()
    1 * closeFile.isActive() >> false
    nextActions.isEmpty()
  }
}
