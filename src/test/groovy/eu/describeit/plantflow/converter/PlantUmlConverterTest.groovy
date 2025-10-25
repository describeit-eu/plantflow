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
    String result = new PlantUmlConverter().convertToPlantFlowDsl(puml)

    then:
    !result.contains('@startuml')
    !result.contains('@enduml')
    !result.contains('-->')
  }

  @Unroll
  def "convert keywords and preserve indentation - #line"() {
    when:
    String result = new PlantUmlConverter().convertToPlantFlowDsl(line)

    then:
    result == expected

    where:
    line                         || expected
    ':doIt;'                     || 'if (isActive("doIt")) return\n'
    'repeat'                     || 'loop("LOOP0") { do {\n'
    'fork'                       || 'fork("FORK0") { forkBlock("FORK_BLOCK1") {\n'
    'if (a > b) then (explain)'  || 'conditional("CONDITIONAL0") { if (eval("a > b", null, "CONDITIONAL0")) { ifBlock("IF_BLOCK1") { // explain\n'
    '  :doIt;'                   || '  if (isActive("doIt")) return\n'
    '    repeat'                 || '    loop("LOOP0") { do {\n'
    '      fork'                 || '      fork("FORK0") { forkBlock("FORK_BLOCK1") {\n'
  }

  def "convertToPlantFlowDsl converts action lines to isActive() checks"() {
    given:
    String puml = ":do something;\n:do another;"

    when:
    String result = new PlantUmlConverter().convertToPlantFlowDsl(puml)

    then:
    result.contains('if (isActive("do something")) return')
    result.contains('if (isActive("do another")) return')
  }

  def "convertToPlantFlowDsl throws IllegalArgumentException for unknown expression"() {
    when:
    new PlantUmlConverter().convertToPlantFlowDsl('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown case for line:unknown something')
  }
}
