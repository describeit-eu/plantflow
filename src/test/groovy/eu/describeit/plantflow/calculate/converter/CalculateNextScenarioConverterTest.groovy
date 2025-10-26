package eu.describeit.plantflow.calculate.converter

import eu.describeit.plantflow.Utility
import spock.lang.Specification
import spock.lang.Unroll

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class CalculateNextScenarioConverterTest extends Specification {

  @Unroll
  def "convert complete puml resource file: #fileName"() {
    given:
    String puml          = Utility.getResourceText(fileName+'.puml')
    String expectedPflow = Utility.getResourceText(fileName+'.pflow')
    String expectedJson  = Utility.getResourceText(fileName+'Context.json')

    when:
    def converter = new CalculateNextConverter()
    String resultPflow = converter.convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    when:
    String actualJson = converter.context.toJson()

    then:
    assertThatJson(actualJson).isEqualTo(expectedJson)

    where:
    fileName << ['sequence','ifThenElseEndif','ifIsThenEndif','ifEqualsThenEndif','forkEndMerge','whileInfinite','whileEndwhile','repeatWhile','crud']//'switchCaseEndswitch'
  }
}
