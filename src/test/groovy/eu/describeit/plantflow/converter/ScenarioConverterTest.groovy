package eu.describeit.plantflow.converter

import eu.describeit.plantflow.Utility
import eu.describeit.plantflow.block.BlockNode
import spock.lang.Specification
import spock.lang.Unroll

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class ScenarioConverterTest extends Specification {

  @Unroll
  def "convert complete puml resource file: #fileName"() {
    given:
    def puml = Utility.getResourceText("${fileName}.puml")
    def expectedPflow = Utility.getResourceText("${fileName}.pflow")

    when:
    def converter = new PlantUmlConverter()
    def resultPflow = converter.convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    when:
    def actualJson = converter.context.toJson()
    def expectedJson = Utility.getResourceText(fileName+'Context.json')

    then:
    assertThatJson(actualJson).isEqualTo(expectedJson)

    where:
    fileName << ['sequence','ifThenElseEndif','ifIsThenEndif','ifEqualsThenEndif','forkEndMerge','whileInfinite','whileEndwhile','repeatWhile','crud']//'switchCaseEndswitch'
  }
}
