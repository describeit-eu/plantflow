package eu.describeit.plantflow.calculate.converter

import spock.lang.Specification
import spock.lang.Unroll

class CalculateNextConverterTest extends Specification {

  def "drops lines start with @ and arrow-like lines"() {
    given:
    String puml = """
      @startuml
      -[babla]->
      @enduml
    """.stripIndent().trim()

    when:
    String result = new CalculateNextConverter().convertToPlantFlowDsl(puml)

    then:
    !result.contains('@startuml')
    !result.contains('@enduml')
    !result.contains('-->')
  }

  @Unroll
  def "convert keywords and preserve indentation - #line"() {
    when:
    String result = new CalculateNextConverter().convertToPlantFlowDsl(line)

    then:
    result == expected

    where:
    line                         || expected
    ':doIt;'                     || 'isActive("doIt")\n'
    'repeat'                     || 'loop("LOOP0") { do { loopBlock("LOOP_BLOCK1") {\n'
    'fork'                       || 'fork("FORK0") { forkBlock("FORK_BLOCK1") {\n'
    'if (a > b) then (explain)'  || 'conditional("CONDITIONAL0") { if (eval("a > b", null, "CONDITIONAL0")) { ifBlock("IF_BLOCK1") { // explain\n'
    '  :doIt;'                   || '  isActive("doIt")\n'
    '    repeat'                 || '    loop("LOOP0") { do { loopBlock("LOOP_BLOCK1") {\n'
    '      fork'                 || '      fork("FORK0") { forkBlock("FORK_BLOCK1") {\n'
  }

  def "convertToPlantFlowDsl converts action lines to isActive() checks"() {
    given:
    String puml = ":do something;\n:do another;"

    when:
    String result = new CalculateNextConverter().convertToPlantFlowDsl(puml)

    then:
    result.contains('isActive("do something")')
    result.contains('isActive("do another")')
  }

  def "convertToPlantFlowDsl throws IllegalArgumentException for unknown expression"() {
    when:
    new CalculateNextConverter().convertToPlantFlowDsl('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown case for line:unknown something')
  }
}
