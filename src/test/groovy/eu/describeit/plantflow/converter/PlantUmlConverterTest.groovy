package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

class PlantUmlConverterTest extends Specification {

  def "convertToPlantFlowDsl drops annotation and arrow-only lines"() {
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
    'end merge'    || '}; if (endFork()) return\n'
    'fork again'   || '} forkAgain {\n'
    '  end merge'  || '  }; if (endFork()) return\n'
    '  endif'      || '  }\n'
    '  fork again' || '  } forkAgain {\n'
  }

  def "convertToPlantFlowDsl converts action lines to action() checks"() {
    given:
    String puml = ":do something;\n:do another;"

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    result.contains('if (isActive("do something")) return')
    result.contains('if (isActive("do another")) return')
  }

  def "convertToPlantFlowDsl maps endif to closing brace"() {
    given:
    String puml = "endif\nendif"

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    // both lines should be converted to a closing brace
    result.readLines().findAll { it.trim() == '}' }.size() == 2
  }

  def "convertToPlantFlowDsl contains expected pflow for resource files"() {
    given:
    def puml = PlantUmlConverter.getResourceText("${fileName}.puml")
    def expectedPflow = PlantUmlConverter.getResourceText("${fileName}.pflow")

    when:
    def resultPflow = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    where:
    fileName << ['sequence', 'ifThenElseEndif', 'ifIsThenEndif', 'forkEndMerge']
    //'repeatWhile','switchCaseEndswitch','whileEndwhile','whileInfinite'
  }
}
