package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class ScenarioWhileEndwhileTest extends Specification {

  void 'scenario loops until condition becomes false, then continues'() {
    given:
    // Actions used inside and after the while loop
    PlantFlowAction readFile = Mock() { getName() >> 'read file' }
    PlantFlowAction closeFile = Mock() { getName() >> 'close file' }

    // Provide a binding we can also use to keep state if needed
    def binding = new Binding()

    // Initialize PlantFlow with the whileEndwhile.pflow script
    def pflow = new PlantFlow('whileEndwhile.pflow', [readFile, closeFile], binding)

    // Override evaluate(String) on the underlying script to simulate changing condition results
    // We want: 'not empty', 'not empty', then 'empty'
    def results = ['not empty', 'not empty', 'empty'] as List<String>
    def script = pflow.pflowScript
    script.metaClass.evaluate = { String expression, String expectedValue ->
      // ensure we simulate the asked question only
      assert expression == 'check filesize ?'
      return results ? results.remove(0) : 'empty'
    }

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
