package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

class PlantUmlConverterTest extends Specification {

  def "drops lines start with @ and arrow-like lines"() {
    given:
    String puml = """
      @startuml
      -[babla]->
      @enduml
    """.stripIndent().trim()

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    !result.contains('@startuml')
    !result.contains('@enduml')
    !result.contains('-->')
  }

  @Unroll
  def "convert keywords and preserve indentation - #line"() {
    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(line)

    then:
    result == expected

    where:
    line           || expected
    'endif'        || '}\n'
    'repeat'       || 'loop("LOOP0") { do {\n'
    'end merge'    || '}; if (endFork()) return\n'
    'fork again'   || '} forkAgain {\n'
    'else (no)'    || '} else { // no\n'
    '  end merge'  || '  }; if (endFork()) return\n'
    '  endif'      || '  }\n'
    '  fork again' || '  } forkAgain {\n'
    '  else (no)'  || '  } else { // no\n'
    '  repeat'     || '  loop("LOOP0") { do {\n'
  }

  def "convertToPlantFlowDsl converts action lines to isActive() checks"() {
    given:
    String puml = ":do something;\n:do another;"

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    result.contains('if (isActive("do something")) return')
    result.contains('if (isActive("do another")) return')
  }
}
