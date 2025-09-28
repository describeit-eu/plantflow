package eu.describeit.plantflow.converter

import spock.lang.Specification

class PlantUmlConverterTest extends Specification {

  def "tab returns requested number of spaces"() {
    expect:
    PlantUmlConverter.tab(n) == ' '.repeat(n)

    where:
    n << [0, 1, 3, 8]
  }

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

  def "convertToPlantFlowDsl converts method-like keywords and preserves indentation"() {
    given:
    String puml = "  fork again"

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    result.contains("  forkAgain()")
  }

  def "convertToPlantFlowDsl converts action lines to action() checks"() {
    given:
    String puml = ":do something;\n:do another;"

    when:
    String result = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    result.contains('if (action("do something")) return')
    result.contains('if (action("do another")) return')
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
    fileName << ['sequence', 'ifThenElseEndif', 'ifIsThenEndif']
    //'forkEndMerge', 'repeatWhile','switchCaseEndswitch','whileEndwhile','whileInfinite'
  }
}
