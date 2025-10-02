package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Ignore
import spock.lang.Specification

class ScenarioWhileEndwhileTest extends Specification {
  PlantFlowAction readFile
  PlantFlowAction closeFile
  PlantFlow pflow

  void mockPlantFlow() {
    readFile = Mock() { getName() >> 'read file' }
    closeFile = Mock() { getName() >> 'close file' }

    pflow = new PlantFlow('whileEndwhile.pflow', [readFile, closeFile])

    def results = ['not empty', 'not empty', 'empty'] as List<String>
    def script = pflow.pflowScript
    script.metaClass.evaluate = { String expression, String expectedValue ->
      assert expression == 'check filesize ?'
      return results ? results.remove(0) : 'empty'
    }
  }

  @Ignore
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
