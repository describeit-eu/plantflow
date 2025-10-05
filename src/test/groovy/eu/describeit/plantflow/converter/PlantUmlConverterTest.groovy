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
    line                         || expected
    ':doIt;'                     || 'if (isActive("doIt")) return\n'
    'repeat'                     || 'loop("LOOP0") { do {\n'
    'fork'                       || 'fork("FORK0") {\n'
    'if (a > b) then (explain)'  || 'conditional("CONDITIONAL0") { if (eval("a > b")) { // explain\n'
    '  :doIt;'                   || '  if (isActive("doIt")) return\n'
    '    repeat'                 || '    loop("LOOP0") { do {\n'
    '      fork'                 || '      fork("FORK0") {\n'
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

  def "convertToPlantFlowDsl throws IllegalArgumentException for unknown expression"() {
    when:
    PlantUmlConverter.convertToPlantFlowDsl('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown case for line:unknown something')
  }
}
