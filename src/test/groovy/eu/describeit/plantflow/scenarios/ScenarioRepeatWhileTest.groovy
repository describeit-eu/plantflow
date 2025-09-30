package eu.describeit.plantflow.scenarios

import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PlantFlowAction
import spock.lang.Specification

class ScenarioRepeatWhileTest extends Specification {

  void 'scenario executes body at least once and repeats while condition true'() {
    given:
    // Actions used inside the repeat-while loop
    PlantFlowAction readData = Mock() {
      getName() >> 'read data'
      // Allow multiple invocations within a single calculateNext() call by providing a sequence
      activate() >>> [true, false, true, false, false]
    }
    PlantFlowAction generateDiagrams = Mock() {
      getName() >> 'generate diagrams'
      activate() >>> [true, true, false, false]
    }

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

    when: '1st run - first action from body'
    def nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['read data']

    when: '2nd run - second action from body'
    nextActions = pflow.calculateNext()

    then:
    nextActions*.name == ['generate diagrams']

    when: '3rd run - first action again'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> true
    0 * generateDiagrams.activate()
    nextActions*.name == ['read data']

    when: '4th run - second action again'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> true
    nextActions*.name == ['generate diagrams']

    when: '5th run - nothing left'
    nextActions = pflow.calculateNext()

    then:
    1 * readData.activate() >> false
    1 * generateDiagrams.activate() >> false
    nextActions.isEmpty()
  }
}
