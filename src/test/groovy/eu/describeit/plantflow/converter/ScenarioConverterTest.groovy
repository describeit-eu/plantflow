package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

class ScenarioConverterTest extends Specification {

  @Unroll
  def "convert complete puml resource file: #fileName"() {
    given:
    def puml = ConversionUtils.getResourceText("${fileName}.puml")
    def expectedPflow = ConversionUtils.getResourceText("${fileName}.pflow")

    when:
    def resultPflow = new PlantUmlConverter().convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    where:
    fileName << ['sequence','ifThenElseEndif','ifIsThenEndif','forkEndMerge','whileInfinite','whileEndwhile','repeatWhile','crud']//'switchCaseEndswitch'
  }
}
