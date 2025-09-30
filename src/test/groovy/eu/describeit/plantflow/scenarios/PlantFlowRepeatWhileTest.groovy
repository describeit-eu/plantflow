package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class PlantFlowRepeatWhileTest extends Specification {

  void 'scenario executes body at least once and repeats while condition true'() {
    given:
    // Actions used inside the repeat-while loop
    PlantFlowAction readData = Mock() { getName() >> 'read data' }
    PlantFlowAction generateDiagrams = Mock() { getName() >> 'generate diagrams' }

    def binding = new Binding()

    // Initialize PlantFlow with the repeatWhile.pflow script
    def pflow = new PlantFlow('repeatWhile.pflow', [readData, generateDiagrams], binding)

    // Simulate condition results for do-while: yes, yes, then no
    def results = ['yes', 'yes', 'no'] as List<String>
    def script = pflow.pflowScript
    script.metaClass.evaluate = { String expression ->
      assert expression == 'more data?'
      return results ? results.remove(0) : 'no'
    }

    when: '1st run - condition true after body -> two actions from body'
    def nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> true
    1 * generateDiagrams.activate() >> true
    nextActions*.name == ['read data', 'generate diagrams']

    when: '2nd run - condition still true -> body executes again'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> true
    1 * generateDiagrams.activate() >> true
    nextActions*.name == ['read data', 'generate diagrams']

    when: '3rd run - condition becomes false after body -> loop stops, no actions left'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    0 * generateDiagrams.activate()
    nextActions.isEmpty()
  }
}
